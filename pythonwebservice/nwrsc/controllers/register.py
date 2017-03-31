import datetime
import logging
import operator
import uuid
import time
import itsdangerous

from itsdangerous import URLSafeTimedSerializer
from flask import abort, Blueprint, current_app, g, redirect, request, render_template, session, url_for

from nwrsc.model import *

log = logging.getLogger(__name__)

Register = Blueprint("Register", __name__) 

@Register.route("/")
def index():
    if 'driverid' not in session: return login()
    return "this is the base page"

@Register.route("/<series>/")
def series():
    if 'driverid' not in session: return login()
    return ("this is the series page for %s" % g.series)

   
@Register.route("/login", methods=['POST', 'GET'])
def login():
    if request.method == 'POST':
        submit = request.form.get('submit', '')
        if submit == "Login":
            return process_login()
        elif submit == "Send Reset Information":
            return process_reset()
        elif submit == "Register":
            return process_register()
    else:
        return render_template('/register/login.html', loginactive=True, gotoseries=g.series)
            

@Register.route("/confirm")
def confirm():
    usts  = URLSafeTimedSerializer(current_app.config["SECRET_KEY"])
    token = request.args.get('token', '')
    req   = {}
    try:
        req = usts.loads(token, max_age=10800) # 3 hour expiry
    except itsdangerous.SignatureExpired as e:
        return "Sorry, this confirmation token has expired (%s)" % e.args[0]

    return "Confirmation OK (%s, %s)" % (req['request'], req['email'])

 
def process_login():
    username   = request.form.get('username', '')
    password   = request.form.get('password', '')
    # verify series
    gotoseries = request.form.get('gotoseries', '')
    if gotoseries and gotoseries not in Series.active():
        gotoseries = ""

    # verify login
    if password != 'goodpass':
        return render_template('/register/login.html', loginactive=True, gotoseries=g.series, loginerror='Invalid username/password')
        
    # Load and verify driver with username and password, getting driverid
    session['driverid'] = uuid.uuid1()
    if gotoseries:
        return redirect(url_for(".series", series=gotoseries))
    return redirect(url_for(".index"))


def process_reset():
    if request.form.get('message'): # super simple bot test (advanced bots will get by this)
        abort(404)
    usts = URLSafeTimedSerializer(current_app.config["SECRET_KEY"])
    first = request.form.get('firstname').strip()
    last  = request.form.get('lastname').strip()
    email = request.form.get('email').strip()
    for d in Driver.find(first, last):
        if d.email == email:
            token = usts.dumps({  'request': 'reset', 
                        'firstname': first,
                         'lastname': last,
                            'email': email
                        })
            return "An email as been sent with a link to reset your username/password., token=(%s)" % (token)

    return "No user could be found with those parameters"


def process_register():
    if request.form.get('message'): # super simple bot test (advanced bots will get by this)
        abort(404)
    usts = URLSafeTimedSerializer(current_app.config["SECRET_KEY"])
    token = usts.dumps({'request':'newuser', 'form':request.form})
    return "An email as been sent with a link to confirm your email. token=(%s)" % (token)



class RegisterController(): #BaseController, PayPalIPN, ObjectEditor):

	def __before__(self):
		action = self.routingargs.get('action', '')
		if action == 'ipn': # nothing else needs to be done for IPN
			return

		g.title = 'Scorekeeper Registration'
		c.stylesheets = ['/css/register.css']
		c.javascript = ['/js/register.js']

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



	def _checkLimitState(self, event, driverid, setResponse=True):
		entries = self.session.query(Registration.id).join('car').filter(Registration.eventid==event.id).filter(Car.driverid==driverid)

		if event.doublespecial and event.drivercount >= event.totlimit and entries.count() == 0:  # past single driver limit, no more new singles
			if setResponse: response.status = "400 Single Driver Limit of %d reached" % (event.totlimit)
			return False
		if not event.doublespecial and event.totlimit and event.count >= event.totlimit:
			if setResponse: response.status = "400 Event limit of %d reached" % (event.totlimit)
			return False
		if entries.count() >= event.perlimit:
			if setResponse: response.status = "400 Entrant limit of %d reached" % (event.perlimit)
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
		except Exception as e:
			response.status = '400 Car registration failed. Possible stale browser state, try reloading page'
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
		except Exception as e:
			response.status = '400 Event registration failed. Possible stale browser state, try reloading page'
			log.error("registercars", exc_info=1)

		 
	def unRegisterCar(self):
		try:
			regid = int(request.POST.get('regid', 0))
			self.session.delete(self.session.query(Registration).get(regid))
			self.session.commit()
		except Exception as e:
			response.status = '400 Unregister failed. Possible stale browser state, try reloading page'
			log.error("unregistercar", exc_info=1)

		 
	#@validate(schema=LoginSchema(), form='login', prefix_error=False)
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



	#@validate(schema=DriverSchema(), form='profile', prefix_error=False)
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
		try:
			id = int(self.routingargs.get('other', 0))
		except:
			abort(404, "No event for input provided")
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
			if not self._checkLimitState(event, c.driverid, setResponse=False): continue
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

