import logging
import os
import glob
import datetime
import urllib

from pylons import request, response, session, config, tmpl_context as c
from pylons.templating import render_mako
from pylons.controllers.util import redirect, url_for
from tw.mods.pylonshf import validate
from nwrsc.lib.base import BaseController, BeforePage
from nwrsc.model import *
from nwrsc.forms import *

log = logging.getLogger(__name__)

class RegisterController(BaseController):

	def __before__(self):
		action = self.routingargs.get('action', '')
		if action == 'ipn': # nothing else needs to be done for IPN
			return

		c.title = 'Scorekeeper Registration'
		c.stylesheets = ['/stylesheets/register.css']
		c.javascript = ['/js/register.js', '/js/jquery-1.4.1.min.js']
		c.tabflags = {}
		c.sponsorlink = self.settings.get('sponsorlink', None)
		c.seriesname = self.settings.get('seriesname', 'Missing Name')

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

			if action not in ['view'] and int(self.settings['locked']):
				# Delete any saved session data for this person
				del ipsession[self.database]
				session.save()
				raise BeforePage(render_mako('/register/locked.mako'))

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
		return render_mako('/register/login.mako')

	@validate(form=loginForm, error_handler='login')
	def checklogin(self):
		query = self.session.query(Driver)
		query = query.filter(Driver.firstname.like(self.form_result['firstname']+'%'))
		query = query.filter(Driver.lastname.like(self.form_result['lastname']+'%'))
		for d in query.all():
			if d.email.lower() == self.form_result['email'].lower():
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
		return render_mako('/register/events.mako')

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
		return render_mako('/register/cars.mako')

	def available(self):
		c.stylesheets = []
		c.code = request.GET.get('code', None)
		if c.code is None:
			return "<h3>No class was selected.  Cannot print a list of available numbers</h3>\n"

		query = self.session.query(Car.number).distinct().filter(Car.classcode==c.code).filter(Car.driverid!=c.driverid)
		c.numbers = set([x[0] for x in query])
		return render_mako('/register/available.mako')

	def new(self):
		return render_mako('/register/profile.mako')
		 
	def profile(self):
		c.driver = self.session.query(Driver).filter(Driver.id==c.driverid).first()
		return render_mako('/register/profile.mako')

	@validate(form=personForm, error_handler='profile')
	def editprofile(self):
		driver = self.session.query(Driver).filter(Driver.id==c.driverid).first()
		for k, v in self.form_result.iteritems():
			if v is not None and v.strip() != "" and hasattr(driver, k):
				setattr(driver, k, v)
		self.session.commit()
		redirect(url_for(action='profile'))

	@validate(form=personFormValidated, error_handler='profile')
	def newprofile(self):
		query = self.session.query(Driver)
		query = query.filter(Driver.firstname == self.form_result['firstname'])
		query = query.filter(Driver.lastname == self.form_result['lastname'])
		query = query.filter(Driver.email == self.form_result['email'])
		for d in query.all():
			self.user['previouserror'] =  "Name and unique ID already exist, please login instead"
			redirect(url_for(action='login'))

		driver = Driver()
		self.session.add(driver)
		self.copyvalues(self.form_result, driver)
		self.session.commit()

		self.user['driverid'] = driver.id
		self.user['firstname'] = driver.firstname
		self.user['lastname'] = driver.lastname
		session.save()
		redirect(url_for(action='events'))
		 

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

	def ipn(self):
		data = dict(request.POST.items())
		args = { 'cmd': '_notify-validate' }
		args.update(dict(request.POST.items()))
		log.debug("Paypal IPN sends: %s" % args)

		result = urllib.urlopen("https://www.paypal.com/cgi-bin/webscr", urllib.urlencode(args)).read()
		log.debug("Paypal IPN: %s" % result)
		if result == 'VERIFIED':
			## txid is a unique key, if a previous exists, this one will overwrite
			## the only time this would concievably happen is when an echeck clears
			tx = self.session.query(Payment).get(data.get('txn_id'))
			if tx is None:
				tx = Payment()
				self.session.add(tx)
				tx.txid = data.get('txn_id')
			tx.type = data.get('payment_type')
			tx.date = datetime.datetime.now()
			tx.status = data.get('payment_status')
			tx.amount = data.get('mc_gross')
			parts = map(int, data.get('custom').split('.'))
			if len(parts) != 2:
				log.error("Paypal IPN: invalid custom: %s" % data.get('custom'))
			else:
				tx.driverid = parts[0]
				tx.eventid = parts[1]
				self.session.commit()

