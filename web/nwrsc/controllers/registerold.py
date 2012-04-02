import logging
import os
import glob
import datetime
import urllib

from pylons import request, response, session, config, tmpl_context as c
from pylons.templating import render_mako
from pylons.controllers.util import redirect, url_for
from pylons.decorators import jsonify, validate
from nwrsc.lib.base import BaseController, BeforePage
from nwrsc.lib.schema import *
from nwrsc.model import *

log = logging.getLogger(__name__)

class RegisteroldController(BaseController):

	def __before__(self):
		action = self.routingargs.get('action', '')
		if action == 'ipn': # nothing else needs to be done for IPN
			return

		c.title = 'Scorekeeper Registration'
		c.stylesheets = ['/css/registerold.css', '/css/custom-theme/jquery-ui-1.8.18.custom.css']
		c.javascript = ['/js/registerold.js', '/js/jquery-1.7.1.min.js', '/js/jquery-ui-1.8.18.custom.min.js', '/js/jquery.validate.min.js']
		c.tabflags = {}
		c.sponsorlink = self.settings.sponsorlink
		c.seriesname = self.settings.seriesname

		ipsession = session.setdefault(self.srcip, {})

		if self.database is not None:
			self.user = ipsession.setdefault(self.database, {})
			c.driverid = self.user.get('driverid', 0)
			c.firstname = self.user.get('firstname', '')
			c.lastname = self.user.get('lastname', '')
			c.previouserror = self.user.get('previouserror', '')
			self.user['previouserror'] = ''

			if action in ['index', 'events', 'cars', 'profile'] and c.driverid < 1:
				session.save()
				redirect(url_for(action='login'))

			if action not in ['view'] and self.settings.locked:
				# Delete any saved session data for this person
				del ipsession[self.database]
				session.save()
				raise BeforePage(render_mako('/registerold/locked.mako'))

			c.events = self.session.query(Event).all()
			c.cars = self.session.query(Car).filter(Car.driverid==c.driverid).order_by(Car.classcode,Car.number).all()
			session.save()
			

	def index(self):
		if self.database is not None:
			return self.events()
		else:
			c.files = map(os.path.basename, glob.glob('%s/*.db' % (config['seriesdir'])))
			return render_mako('/databaseselect.mako')

	def login(self):
		return render_mako('/registerold/login.mako')

	@validate(schema=LoginSchema(), form='login', prefix_error=False)
	def checklogin(self):
		query = self.session.query(Driver)
		query = query.filter(Driver.firstname.like(self.form_result['firstname']+'%'))
		query = query.filter(Driver.lastname.like(self.form_result['lastname']+'%'))
		for d in query.all():
			if d.email.lower().strip() == self.form_result['email'].lower().strip():
				self.user['driverid'] = d.id
				self.user['firstname'] = d.firstname
				self.user['lastname'] = d.lastname
				session.save()
				redirect(url_for(action='events'))

		self.user['previouserror'] =  """Couldn't find a match for your information, try again.<br>
				If you have never registered before, you can <a href='%s'>create a new profile</a>""" % url_for(action='new')
		session.save()
		redirect(url_for(action='login'))


	def events(self):
		now = datetime.datetime.now()
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
		return render_mako('/registerold/events.mako')

	def register(self):
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
				self.user['previouserror'] = "Sorry, prereg reached its limit of %d since your last page load" % (event.totlimit)
				session.save()
			else:
				reg = Registration(eventid, carid)
				self.session.add(reg)

		self.session.commit()
		redirect(url_for(action='events'))

		 
	def cars(self):
		c.classdata = ClassData(self.session)
		c.inuse = []
		c.notinuse = []
		regids = [x[0] for x in self.session.query(Registration.carid).join('car').distinct().filter(Car.driverid==c.driverid)]
		for car in c.cars:
			if car.id in regids:
				c.inuse.append(car)
			else:
				c.notinuse.append(car)
		return render_mako('/registerold/cars.mako')

	def available(self):
		c.stylesheets = []
		c.code = request.GET.get('code', None)
		if c.code is None:
			return "<h3>No class was selected.  Cannot print a list of available numbers</h3>\n"

		if self.settings.superuniquenumbers:
			query = self.session.query(Car.number).distinct().filter(Car.driverid!=c.driverid)
		else:
			query = self.session.query(Car.number).distinct().filter(Car.classcode==c.code).filter(Car.driverid!=c.driverid)
		c.numbers = set([x[0] for x in query])
		c.largestnumber = self.settings.largestcarnumber
		return render_mako('/registerold/available.mako')

	def new(self):
		c.fields = self.session.query(DriverField).all()
		return render_mako('/registerold/profile.mako')
		 
	def profile(self):
		c.driver = self.session.query(Driver).filter(Driver.id==c.driverid).first()
		c.fields = self.session.query(DriverField).all()
		return render_mako('/registerold/profile.mako')


	def _filldriver(self, form_result, driver):
		fields = self.session.query(DriverField).all()
		fieldnames = [x.name for x in fields]

		for k, v in form_result.iteritems():
			if v is not None and hasattr(driver, k):
				setattr(driver, k, v)
			elif k in fieldnames:
				if len(v) == 0:
					driver.delExtra(k)
				else:
					driver.setExtra(k, v)


	@validate(schema=DriverSchema(), form='profile', prefix_error=False)
	def editprofile(self):
		driver = self.session.query(Driver).filter(Driver.id==c.driverid).first()
		self._filldriver(self.form_result, driver)
		self.session.commit()
		redirect(url_for(action='profile'))


	@validate(schema=DriverSchema(), form='profile', prefix_error=False)
	def newprofile(self):
		query = self.session.query(Driver)
		query = query.filter(Driver.firstname.like(self.form_result['firstname'])) # no case compare
		query = query.filter(Driver.lastname.like(self.form_result['lastname'])) # no case compare
		query = query.filter(Driver.email.like(self.form_result['email'])) # no case compare
		for d in query.all():
			self.user['previouserror'] =  "Name and unique ID already exist, please login instead"
			redirect(url_for(action='login'))

		driver = Driver()
		self.session.add(driver)
		self._filldriver(self.form_result, driver)
		self.session.commit()

		self.user['driverid'] = driver.id
		self.user['firstname'] = driver.firstname
		self.user['lastname'] = driver.lastname
		session.save()
		redirect(url_for(action='events'))
		 

	def view(self):
		id = request.GET.get('event', None)
		if id is None:
			return render_mako('/registerold/eventselect.mako')
			
		c.classdata = ClassData(self.session)
		c.event = self.session.query(Event).get(id)
		query = self.session.query(Driver,Car,Registration).join('cars', 'registration').filter(Registration.eventid==id)
		query = query.order_by(Car.classcode, Car.number)
		c.reglist = query.all()
		for reg in c.reglist:
			if reg[0].alias:
				reg[0].firstname = reg[0].alias
				reg[0].lastname = ""

		return render_mako('/registerold/reglist.mako')

	def editcar(self):
		p = request.POST
		car = self.session.query(Car).get(p['carid'])
		if car is None:
			car = Car()
			self.session.add(car)
			car.driverid = c.driverid
		if p.get('ctype', '') == 'delete':
			self.session.delete(car)
		else:
			for attr in ('year', 'make', 'model', 'color', 'number', 'classcode', 'indexcode'):
				if attr in p:
					setattr(car, attr, p[attr])
		self.session.commit()
		redirect(url_for(action='cars'))
		 
	def logout(self):
		# Clear session for database
		del session[self.srcip][self.database]
		session.save()
		redirect(url_for(action='index'))

