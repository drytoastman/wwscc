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
from nwrsc.lib.paypal import PayPalIPN
from nwrsc.lib.objecteditors import ObjectEditor
from nwrsc.lib.schema import *
from nwrsc.model import *

log = logging.getLogger(__name__)

class RegisterController(BaseController, PayPalIPN, ObjectEditor):

	def __before__(self):
		action = self.routingargs.get('action', '')
		if action == 'ipn': # nothing else needs to be done for IPN
			return

		c.title = 'Scorekeeper Registration'
		c.stylesheets = ['/css/register.css', '/css/redmond/jquery-ui-1.8.2.custom.css']
		c.javascript = ['/js/register.js', '/js/jquery-1.4.2.min.js', '/js/jquery-ui-1.8.2.custom.min.js', '/js/jquery.validate.min.js']
		c.settings = self.settings

		ipsession = session.setdefault(self.srcip, {})

		if self.database is not None:
			self.user = ipsession.setdefault(self.database, {})
			c.driverid = self.user.get('driverid', 0)
			c.previouserror = self.user.get('previouserror', '')
			self.user['previouserror'] = ''

			if action in ['index', 'events', 'cars', 'profile'] and c.driverid < 1:
				session.save()
				redirect(url_for(action='login'))

			if action not in ['view'] and self.settings.locked:
				# Delete any saved session data for this person
				del ipsession[self.database]
				session.save()
				raise BeforePage(render_mako('/register/locked.mako'))

			c.events = self.session.query(Event).all()
			c.cars = self.session.query(Car).filter(Car.driverid==c.driverid).order_by(Car.classcode,Car.number).all()
			session.save()
			

	def login(self):
		return render_mako('/register/login.mako')

	def logout(self):
		# Clear session for database
		del session[self.srcip][self.database]
		session.save()
		redirect(url_for(action='index'))

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
		#return render_mako('/registerold/profile.mako')

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
		#return render_mako('/registerold/events.mako')

		c.classdata = ClassData(self.session)
		c.inuse = []
		c.notinuse = []
		regids = [x[0] for x in self.session.query(Registration.carid).join('car').distinct().filter(Car.driverid==c.driverid)]
		for car in c.cars:
			car.inuse = car.id in regids
		return render_mako('/register/layout.mako')


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
		return render_mako('/registerold/reglist.mako')


