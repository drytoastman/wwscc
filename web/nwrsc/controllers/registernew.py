import logging
import os
import glob
import datetime
import urllib

from pylons import request, response, session, config, tmpl_context as c
from pylons.templating import render_mako, render_mako_def
from pylons.controllers.util import redirect, url_for, etag_cache
from pylons.decorators import jsonify, validate
from nwrsc.lib.base import BaseController, BeforePage
from nwrsc.lib.paypal import PayPalIPN
from nwrsc.lib.objecteditors import ObjectEditor
from nwrsc.lib.schema import *
from nwrsc.model import *

log = logging.getLogger(__name__)

class RegisternewController(BaseController, PayPalIPN, ObjectEditor):

	def __before__(self):
		action = self.routingargs.get('action', '')
		if action == 'ipn': # nothing else needs to be done for IPN
			return

		c.title = 'Scorekeeper Registration'
		c.stylesheets = ['/css/register.css', '/css/custom-theme/jquery-ui-1.8.18.custom.css']
		c.javascript = ['/js/jquery-1.7.1.min.js', '/js/jquery-ui-1.8.18.custom.min.js', '/js/jquery.validate.min.js']

		try:
			c.javascript.append(url_for(action='scripts'))
		except:
			pass

		c.settings = self.settings

		ipsession = session.setdefault(self.srcip, {})

		if self.database is not None:
			self.user = ipsession.setdefault(self.database, {})
			c.driverid = self.user.get('driverid', 0)
			c.previouserror = self.user.get('previouserror', '')
			c.database = self.database
			self.user['previouserror'] = ''

			if action in ['index', 'events', 'cars', 'profile'] and c.driverid < 1:
				session.save()
				redirect(url_for(action='login'))

			if action not in ['view'] and self.settings.locked:
				# Delete any saved session data for this person
				del ipsession[self.database]
				session.save()
				raise BeforePage(render_mako('/register/locked.mako'))

			session.save()
			

	def login(self):
		c.fields = self.session.query(DriverField).all()
		return render_mako('/register/login.mako')

	def logout(self):
		# Clear session for database
		del session[self.srcip][self.database]
		session.save()
		redirect(url_for(action='index'))

	def scripts(self):
		response.headers['Cache-Control'] = 'max-age=360' 
		response.headers.pop('Pragma', None)
		return render_mako('/forms/careditor.mako') + render_mako('/forms/drivereditor.mako')

	@validate(schema=LoginSchema(), form='login', prefix_error=False)
	def checklogin(self):
		query = self.session.query(Driver)
		query = query.filter(Driver.firstname.like(self.form_result['firstname']+'%'))
		query = query.filter(Driver.lastname.like(self.form_result['lastname']+'%'))
		for d in query.all():
			if d.email.lower().strip() == self.form_result['email'].lower().strip():
				self.user['driverid'] = d.id
				session.save()
				redirect(url_for(action=''))

		self.user['previouserror'] =  """Couldn't find a match for your information, try again.<br>
				If you have never registered before, you can <a href='%s'>create a new profile</a>""" % url_for(action='new')
		session.save()
		redirect(url_for(action='login'))


	def index(self):
		if self.database is None:
			c.files = map(os.path.basename, glob.glob('%s/*.db' % (config['seriesdir'])))
			return render_mako('/databaseselect.mako')

		c.driver = self.session.query(Driver).filter(Driver.id==c.driverid).first()
		c.fields = self.session.query(DriverField).all()

		now = datetime.datetime.now()
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
				self.user['previouserror'] = "Sorry, prereg reached its limit of %d since your last page load" % (event.totlimit)
				session.save()
			else:
				reg = Registration(eventid, carid)
				self.session.add(reg)

		self.session.commit()

		 
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
		self._extractDriver(driver)
		self.session.commit()

		self.user['driverid'] = driver.id
		session.save()
		redirect(url_for(action='index'))


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


