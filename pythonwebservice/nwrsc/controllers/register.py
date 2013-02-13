import datetime
import logging
import operator

from pylons import request, response, session, config, tmpl_context as c
from pylons.templating import render_mako, render_mako_def
from pylons.controllers.util import redirect, url_for, abort
from pylons.decorators import validate
from sqlalchemy import create_engine, func

from nwrsc.controllers.lib.base import BaseController, BeforePage
from nwrsc.controllers.lib.paypal import PayPalIPN
from nwrsc.controllers.lib.objecteditors import ObjectEditor

from nwrsc.lib.schema import *
from nwrsc.model import *

log = logging.getLogger(__name__)


class Cred(object):
	def __init__(self, firstname, lastname, email):
		self.firstname = firstname
		self.lastname = lastname
		self.email = email

	def __hash__(self):
		return hash((self.firstname, self.lastname, self.email))

	def __eq__(self, other):
		return (self.firstname, self.lastname, self.email) == (other.firstname, other.lastname, other.email)

	def __repr__(self):
		return "%s, %s, %s" % (self.firstname, self.lastname, self.email)



class UserSession(object):

	def __init__(self, data, active):
		self.data = data
		self.active = active

	def _series(self, series=None):
		if series is None: series = self.active
		return self.data.setdefault(series.upper(), {})

	def getDriverId(self):
		return self._series().get('driverid', -1)

	def getPreviousError(self):
		db = self._series()
		ret = db.get('previouserror', '')
		db['previouserror'] = ''
		session.save()
		return ret

	def setPreviousError(self, error):
		db = self._series()
		db['previouserror'] = error
		session.save()

	def setLoginInfo(self, driver=None, series=None):
		db = self._series(series)
		db['driverid'] = driver.id
		db['creds'] = Cred(driver.firstname, driver.lastname, driver.email)
		session.save()

	def hasCreds(self, series):
		db = self._series(series)
		return db.get('driverid', -1) > 0

	def activeSeries(self):
		seriesmap = dict()
		for name, db in self.data.iteritems():
			creds = db.get('creds', None)
			if creds is not None:
				seriesmap[name] = creds
		return seriesmap

	def activeCreds(self):
		ids = set()
		for db in self.data.itervalues():
			creds = db.get('creds', None)
			if creds is not None:
				ids.add(creds)
		return ids

	def clear(self):
		""" Remove all active creds """
		for name in self.data:
			self.data[name] = {}
		session.save()

	def clearSeries(self):
		""" Clear just the data from this active series (if something is kajigered) """
		self._series().clear()

	def __repr__(self):
		return repr(self.data)
		


class RegisterController(BaseController, PayPalIPN, ObjectEditor):

	def __before__(self):
		action = self.routingargs.get('action', '')
		if action == 'ipn': # nothing else needs to be done for IPN
			return

		c.title = 'Scorekeeper Registration'
		c.stylesheets = ['/css/register.css']
		c.javascript = ['/js/register.js']

		c.activeSeries = self._activeSeries()
		if self.database is None:
			return

		self.user = UserSession(session.setdefault(('register', self.srcip), {}), self.database) 

		c.settings = self.settings
		c.database = self.database
		c.driverid = self.user.getDriverId()
		c.previouserror = self.user.getPreviousError()
		c.classdata = ClassData(self.session)
		c.eventmap = dict()
		now = datetime.datetime.now()
		for event in self.session.query(Event):
			event.closed = now > event.regclosed 
			event.opened = now > event.regopened
			event.isOpen = not event.closed and event.opened
			c.eventmap[event.id] = event

		c.events = sorted(c.eventmap.values(), key=lambda obj: obj.date)

		if action not in ('view') and self.settings.locked:
			# Delete any saved session data for this person
			raise BeforePage(render_mako('/register/locked.mako'))

		if action in ('index', 'events', 'cars', 'profile') and c.driverid < 1:
			c.activecreds = self.user.activeCreds()
			for cred in c.activecreds:
				driver = self._verifyID(**cred.__dict__)
				if driver is not None:
					self.user.setLoginInfo(driver)
					c.driverid = self.user.getDriverId()
					return # continue on to regular page, we are now verified

			c.fields = self.session.query(DriverField).all()
			c.otherseries = self.user.activeSeries()
			raise BeforePage(render_mako('/register/login.mako'))


	def login(self):
		redirect(url_for(action=''))

	def logout(self):
		# Clear session for database
		self.user.clear()
		redirect(url_for(action=''))

	def index(self):
		""" First load of page gets all data along with it so no need for lots of ajax requests """
		if self.database is None:
			return self.databaseSelector(archived=False)

		self._loadDriver()
		self._loadCars()
		if c.driver is None:
			c.previouserror = "Invalid driver ID saved in session, that is pretty weird, login again"
			self.user.clearSeries()
			c.otherseries = self.user.activeSeries()
			return render_mako('/register/login.mako')

		for e in c.events:
			e.regentries = self.session.query(Registration).join('car') \
						.filter(Registration.eventid==e.id).filter(Car.driverid==c.driverid).all()
			e.payments = self.session.query(Payment) \
						.filter(Payment.eventid==e.id).filter(Payment.driverid==c.driverid).all()


		return render_mako('/register/layout.mako')



	def _checkLimitState(self, event, driverid):
		if event.totlimit and event.count >= event.totlimit:
			response.status = "400 Event limit of %d reached" % (event.totlimit)
			return False
		entries = self.session.query(Registration.id).join('car').filter(Registration.eventid==event.id).filter(Car.driverid==driverid)
		if entries.count() >= event.perlimit:
			response.status = "400 Entrant limit of %d reached" % (event.perlimit)
			return False
		return True
		
		
	def registerCarsForEvent(self):
		try:
			eventid = int(request.POST.pop('eventid', 0))
			event = self.session.query(Event).get(eventid)
			for carid in map(int, request.POST):
				car = self.session.query(Car).get(carid)
				if not self._checkLimitState(event, car.driverid):
					return
				reg = Registration(eventid, carid)
				self.session.add(reg)
			self.session.commit()
		except Exception, e:
			response.status = '400 Possible stale browser state, try reloading page'
			log.error("registerevents", exc_info=1)

		 
	def registerEventsForCar(self):
		try:
			carid = int(request.POST.pop('carid', 0))
			car = self.session.query(Car).get(carid)

			for eventid in map(int, request.POST):
				event = self.session.query(Event).get(eventid)
				if not self._checkLimitState(event, car.driverid):
					return
				reg = Registration(eventid, carid)
				self.session.add(reg)

			self.session.commit()
		except Exception, e:
			response.status = '400 Possible stale browser state, try reloading page'
			log.error("registercars", exc_info=1)

		 
	def unRegisterCar(self):
		try:
			regid = int(request.POST.get('regid', 0))
			self.session.delete(self.session.query(Registration).get(regid))
			self.session.commit()
		except Exception, e:
			self.user.setPreviousError("Possible stale browser state, try reloading page")

		 
	@validate(schema=LoginSchema(), form='login', prefix_error=False)
	def checklogin(self):
		fr = self.form_result

		# Try and copy user profile from another series
		if fr['otherseries']:
			log.info("Copy user profile from %s to %s", fr['otherseries'], self.database)
			driver = self._loadDriverFrom(fr['otherseries'], fr['firstname'], fr['lastname'], fr['email'])
			if driver is not None:
				self.session.add(driver)
				self.session.commit()
				self.user.setLoginInfo(driver)
			else:
				self.user.setPreviousError("Failed to find profile in %s" % (fr['otherseries']))
				log.error("Failed to load driver from other series (%s)", fr)

			redirect(url_for(action=''))

		# Try and login to all matching series, may or may not be this one
		othermatches = list()
		for series in self._databaseList(archived=False, driver=Driver(**fr)):
			if series.driver is not None:
				othermatches.append(series)
				if not self.user.hasCreds(series.name):
					self.user.setLoginInfo(series.driver, series.name)
				
		if not self.user.hasCreds(c.database.upper()):
			self.user.setPreviousError("login failed")
		redirect(url_for(action=''))



	@validate(schema=DriverSchema(), form='profile', prefix_error=False)
	def newprofile(self):
		query = self.session.query(Driver)
		query = query.filter(Driver.firstname.like(self.form_result['firstname'])) # no case compare
		query = query.filter(Driver.lastname.like(self.form_result['lastname'])) # no case compare
		query = query.filter(Driver.email.like(self.form_result['email'])) # no case compare
		for d in query.all():
			self.user.setPreviousError("Name and unique ID already exist, please login instead")
			redirect(url_for(action=''))

		driver = Driver()
		self.session.add(driver)
		self._extractDriver(driver)
		self.session.commit()

		self.user.setLoginInfo(driver)
		redirect(url_for(action=''))


	def getprofile(self):
		self._loadDriver()
		return render_mako_def('/register/profile.mako', 'profile')

	def getcars(self):
		self._loadCars()
		return render_mako_def('/register/cars.mako', 'carlist')

	def getevent(self):
		eventid = int(self.routingargs.get('other', 0))
		event = c.eventmap.get(eventid, None)
		if event is None:
			return "Event does not exist"

		event.regentries = self.session.query(Registration).join('car').filter(Registration.eventid==event.id).filter(Car.driverid==c.driverid).all()
		event.payments = self.session.query(Payment).filter(Payment.eventid==event.id).filter(Payment.driverid==c.driverid).all()
		self._loadCars()
		return render_mako_def('/register/events.mako', 'eventdisplay', ev=event)

		 
	def view(self):
		id = int(self.routingargs.get('other', 0))
		if id == 0:
			redirect(url_for(action=''))
			
		c.classdata = ClassData(self.session)
		c.event = self.session.query(Event).get(id)
		query = self.session.query(Driver,Car,Registration).join('cars', 'registration').filter(Registration.eventid==id)
		query = query.order_by(Car.classcode, Car.number)
		c.reglist = query.all()
		return render_mako('/register/reglist.mako')


	def _loadCars(self):
		c.cars = self.session.query(Car).filter(Car.driverid==c.driverid).order_by(Car.classcode,Car.number).all()
		registration = self.session.query(Registration).join('car').distinct().filter(Car.driverid==c.driverid)

		openevents = list()
		for event in c.events:
			if not event.isOpen: continue
			if event.totlimit and event.count >= event.totlimit: continue
			if registration.filter(Registration.eventid==event.id).count() >= event.perlimit: continue
			openevents.append(event.id)

		for car in c.cars:
			car.regevents = sorted([(c.eventmap[reg.eventid], reg.id) for reg in registration.all() if reg.carid == car.id], key = lambda x: x[0].date)
			car.canregevents = list(openevents)
			for event, regid in car.regevents:
				try: car.canregevents.remove(event.id)
				except: pass

	def _loadDriver(self):
		c.driver = self.session.query(Driver).filter(Driver.id==c.driverid).first()
		if c.driver is None:
			return
		c.fields = self.session.query(DriverField).all()
		for field in c.fields:
			setattr(c.driver, field.name, c.driver.getExtra(field.name))

