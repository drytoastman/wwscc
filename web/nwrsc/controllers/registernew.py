import datetime
import logging

from pylons import request, response, session, config, tmpl_context as c
from pylons.templating import render_mako, render_mako_def
from pylons.controllers.util import redirect, url_for
from pylons.decorators import jsonify, validate
from sqlalchemy import create_engine

from nwrsc.controllers.lib.base import BaseController, BeforePage
from nwrsc.controllers.lib.paypal import PayPalIPN
from nwrsc.controllers.lib.objecteditors import ObjectEditor

from nwrsc.lib.schema import *
from nwrsc.model import *

log = logging.getLogger(__name__)


class UserSession():

	def __init__(self, data, active):
		self.data = data
		self.active = active

	def _series(self, series=None):
		if series is None: series = self.active
		return self.data.setdefault(series, {})

	def getDriverId(self):
		return self._series().get('driverid', -1)

	def getOptionalLogin(self):
		db = self._series()
		a = db.setdefault('optionailLogin', [])
		b = db.setdefault('showNewProfile', False)
		session.save()
		return (a, b)

	def setOptionalLogin(self, otherseries, showNewProfile=True):
		db = self._series()
		db['optionailLogin'] = otherseries
		db['showNewProfile'] = showNewProfile
		session.save()

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

	def setLoginInfo(self, driver=None):
		db = self._series()
		db['driverid'] = driver.id
		db['creds'] = (driver.firstname, driver.lastname, driver.email)
		session.save()

	def activeCreds(self):
		ids = set()
		for db in self.data.itervalues():
			creds = db.get('creds', '')
			if len(creds) == 3:
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
		


class RegisternewController(BaseController, PayPalIPN, ObjectEditor):

	def __before__(self):
		action = self.routingargs.get('action', '')
		if action == 'ipn': # nothing else needs to be done for IPN
			return

		c.title = 'Scorekeeper Registration'
		c.stylesheets = ['/css/register.css', '/css/custom-theme/jquery-ui-1.8.18.custom.css']
		c.javascript = ['/js/jquery-1.7.1.min.js', '/js/jquery-ui-1.8.18.custom.min.js', '/js/jquery.validate.min.js']

		if self.database is None:
			return

		c.javascript.append(url_for(action='scripts'))
		self.user = UserSession(session.setdefault(('register', self.srcip), {}), self.database) 

		c.settings = self.settings
		c.database = self.database
		c.driverid = self.user.getDriverId()
		c.previouserror = self.user.getPreviousError()

		if action not in ('view', 'scripts') and self.settings.locked:
			# Delete any saved session data for this person
			raise BeforePage(render_mako('/register/locked.mako'))

		if action in ('index', 'events', 'cars', 'profile') and c.driverid < 1:
			for cred in self.user.activeCreds():
				driver = self._verifyID(*cred)
				if driver is not None:
					self.user.setLoginInfo(driver)
					c.driverid = self.user.getDriverId()
					return # continue on to regular page, we are now verified

			c.fields = self.session.query(DriverField).all()
			(c.otherseries, c.shownewprofile) = self.user.getOptionalLogin()
			raise BeforePage(render_mako('/register/login.mako'))


	def login(self):
		redirect(url_for(action=''))

	def logout(self):
		# Clear session for database
		self.user.clear()
		redirect(url_for(action=''))

	def scripts(self):
		response.headers['Cache-Control'] = 'max-age=360' 
		response.headers.pop('Pragma', None)
		return render_mako('/forms/careditor.mako') + render_mako('/forms/drivereditor.mako')


	def index(self):
		if self.database is None:
			return self.databaseSelector()

		now = datetime.datetime.now()
		c.driver = self.session.query(Driver).filter(Driver.id==c.driverid).first()
		if c.driver is None:
			c.previouserror = "Invalid driver ID saved in session, that is pretty weird, login again"
			self.user.clearSeries()
			return render_mako('/register/login.mako')

		c.fields = self.session.query(DriverField).all()
		c.events = self.session.query(Event).all()
		for e in c.events:
			e.regentries = self.session.query(Registration).join('car') \
						.filter(Registration.eventid==e.id).filter(Car.driverid==c.driverid).all()
			e.payments = self.session.query(Payment) \
						.filter(Payment.eventid==e.id).filter(Payment.driverid==c.driverid).all()
			e.closed = now > e.regclosed 
			e.opened = now > e.regopened
			e.tdclass = ''
			if e.closed or not e.opened:
				e.tdclass = 'closed'

		c.classdata = ClassData(self.session)
		c.inuse = []
		c.notinuse = []
		regids = [x[0] for x in self.session.query(Registration.carid).join('car').distinct().filter(Car.driverid==c.driverid)]
		c.cars = self.session.query(Car).filter(Car.driverid==c.driverid).order_by(Car.classcode,Car.number).all()
		for car in c.cars:
			car.inuse = car.id in regids
		return render_mako('/register/layout.mako')



	def registercar(self):
		carid = int(request.POST.get('carid', 0))
		regid = int(request.POST.get('regid', 0))
		eventid = int(request.POST.get('eventid', 0))
		reg = self.session.query(Registration).filter(Registration.id==regid).first()

		if carid < 0: # delete car
			self.session.delete(reg)
		elif regid > 0: # modify car
			reg.carid = carid
		else: # add car
			event = self.session.query(Event).filter(Event.id==eventid).first()
			if event.totlimit and event.count >= event.totlimit:
				self.user.setPreviousError("Sorry, prereg reached its limit of %d since your last page load" % (event.totlimit))
			else:
				reg = Registration(eventid, carid)
				self.session.add(reg)

		self.session.commit()

		 
	@validate(schema=LoginSchema(), form='login', prefix_error=False)
	def checklogin(self):
		fr = self.form_result

		# Try and copy user profile from another series
		if fr['otherseries']:
			log.info("Copy user profile from %s to %s", fr['otherseries'], self.database)
			driver = self._loadDriverFrom(fr['otherseries'], fr['firstname'], fr['lastname'], fr['email'])
			self.session.add(driver)
			self.session.commit()
			self.user.setLoginInfo(driver)
			redirect(url_for(action=''))

		# Try and login to this series
		driver = self._verifyID(fr['firstname'], fr['lastname'], fr['email'])
		if driver is not None:
			log.debug("Login approved")
			self.user.setLoginInfo(driver)
			redirect(url_for(action=''))

		# Nothing worked, find any info we can and let user know on next load
		othermatches = list()
		for series in self._databaseList(archived=False, driver=Driver(**fr)):
			if series.driver is not None:
				othermatches.append(series)
		self.user.setOptionalLogin(othermatches, True)
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


	@jsonify
	def getprofile(self):
		c.driver = self.session.query(Driver).filter(Driver.id==c.driverid).first()
		c.fields = self.session.query(DriverField).all()
		return {'data': str(render_mako_def('/register/profile.mako', 'profile'))}
		 
	@jsonify
	def getcars(self):
		c.driver = self.session.query(Driver).filter(Driver.id==c.driverid).first()
		c.cars = self.session.query(Car).filter(Car.driverid==c.driverid).order_by(Car.classcode,Car.number).all()
		regids = [x[0] for x in self.session.query(Registration.carid).join('car').distinct().filter(Car.driverid==c.driverid)]
		for car in c.cars:
			car.inuse = car.id in regids
		return {'data': str(render_mako_def('/register/cars.mako', 'carlist'))}

	@jsonify
	def getevent(self):
		eventid = int(request.GET.get('eventid', 0))
		event = self.session.query(Event).filter(Event.id==eventid).first()
		event.regentries = self.session.query(Registration).join('car').filter(Registration.eventid==event.id).filter(Car.driverid==c.driverid).all()
		event.payments = self.session.query(Payment).filter(Payment.eventid==event.id).filter(Payment.driverid==c.driverid).all()
		now = datetime.datetime.now()
		event.closed = now > event.regclosed 
		event.opened = now > event.regopened
		event.tdclass = ''
		if event.closed or not event.opened:
			event.tdclass = 'closed'

		c.cars = self.session.query(Car).filter(Car.driverid==c.driverid).order_by(Car.classcode,Car.number).all()
		regids = [x[0] for x in self.session.query(Registration.carid).join('car').distinct().filter(Car.driverid==c.driverid)]
		for car in c.cars:
			car.inuse = car.id in regids
		return {'data': str(render_mako_def('/register/events.mako', 'eventdisplay', ev=event))}

		 

	def view(self):
		id = request.GET.get('event', None)
		if id is None:
			return render_mako('/register/eventselect.mako')
			
		c.classdata = ClassData(self.session)
		c.event = self.session.query(Event).get(id)
		query = self.session.query(Driver,Car,Registration).join('cars', 'registration').filter(Registration.eventid==id)
		query = query.order_by(Car.classcode, Car.number)
		c.reglist = query.all()
		return render_mako('/register/reglist.mako')


