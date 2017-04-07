import datetime
import logging
import uuid
import time
import itsdangerous
from collections import defaultdict
from operator import attrgetter

from flask import abort, Blueprint, current_app, g, redirect, request, render_template, session, url_for

from nwrsc.model import *
from nwrsc.lib.forms import *

log = logging.getLogger(__name__)

Register = Blueprint("Register", __name__) 

@Register.before_request
def setup():
    g.title = 'Scorekeeper Registration'
    g.activeseries = Series.active()
    if 'driverid' in session:
        g.driver = Driver.get(session['driverid'])
    else:
        g.driver = None

####################################################################
# Authenticated functions

@Register.route("/")
def index():
    if not g.driver: return login()
    return render_template('serieslist.html', subapp='register', serieslist=Series.list())

@Register.route("/<series>/")
def series():
    """ First load of page gets all data along with it so no need for lots of ajax requests """
    if not g.driver: return login()
    return redirect(url_for('.events'))

@Register.route("/<series>/profile", methods=['POST', 'GET'])
def profile():
    if not g.driver: return login()
    g.driver = Driver.get(session['driverid'])
    form = ProfileForm()

    if form.validate_on_submit():
        formIntoAttrBase(form, g.driver)
        g.driver.update()

    attrBaseIntoForm(g.driver, form)
    return render_template('register/profile.html', form=form, formerror=form.errors)


@Register.route("/<series>/cars", methods=['POST', 'GET'])
def cars():
    if not g.driver: return login()
    g.classdata = ClassData.get()
    events      = {e.eventid:e for e in Event.byDate()}
    cars        = {c.carid:c   for c in Car.getForDriver(g.driver.driverid)}
    registered  = defaultdict(list)
    formerror   = ""
    carform     = CarForm()
    carform.classcode.choices = [(c.classcode, "%s - %s" % (c.classcode, c.descrip)) for c in sorted(g.classdata.classlist.values(), key=attrgetter('classcode'))]
    carform.indexcode.choices = [(i.indexcode, "%s - %s" % (i.indexcode, i.descrip)) for i in sorted(g.classdata.indexlist.values(), key=attrgetter('indexcode'))]

    for r in Registration.getForDriver(g.driver.driverid):
        registered[r.carid].append(r.eventid)
    return render_template('register/cars.html', events=events, cars=cars, registered=registered, carform=carform, formerror=formerror)


@Register.route("/<series>/events")
def events():
    if not g.driver: return login()
    g.classdata = ClassData.get()
    events = Event.byDate()
    cars   = {c.carid:c   for c in Car.getForDriver(g.driver.driverid)}
    registered = defaultdict(list)
    payments = defaultdict(list)
    for r in Registration.getForDriver(g.driver.driverid):
        registered[r.eventid].append(r.carid)
    for p in Payment.getForDriver(g.driver.driverid):
        payments[p.eventid] = p

    return render_template('register/events.html', events=events, cars=cars, registered=registered, payments=payments)


@Register.route("/logout")
def logout():
    session.pop('driverid')
    return redirect(url_for('.index'))

@Register.route("/<series>/register", methods=['POST'])
def register():
    return processregistration(Registration.add)
    
@Register.route("/<series>/unregister", methods=['POST'])
def unregister():
    return processregistration(Registration.delete)

def processregistration(func):
    if not g.series or 'driverid' not in session: abort(404)
    # TODO verify carid related to driver
    eventid = int(request.form.get('eventid', ''))
    carid = uuid.UUID(request.form.get('carid', ''))
    func(eventid, carid)
    return redirect_series(g.series)

####################################################################
# Unauthenticated functions

@Register.route("/<series>/view/<int:eventid>")
def view():
    event = Event.get(g.eventid)
    if event is None:
        abort(404, "No event found for id %s" % g.eventid)
    g.settings = Settings.get()
    g.classdata = ClassData.get()
    registered = defaultdict(list)
    for r in Registration.getForEvent(g.eventid):
        registered[r.classcode].append(r)
    return render_template('register/reglist.html', event=event, registered=registered)

@Register.route("/<series>/ipn")
def ipn():
    return "ipn"

@Register.route("/ical/<driverid>")
def ical(driverid):
    return "ical for %s" % driverid

@Register.route("/login", methods=['POST', 'GET'])
def login():
    if g.driver: return redirect_series()
    if request.form.get('message'): # super simple bot test (advanced bots will get by this)
        abort(404)

    login = LoginForm(prefix='login')
    reset = ResetForm(prefix='reset')
    register = RegisterForm(prefix='register')
    formerror = ""
    active = "login"

    if login.submit.data:
        if login.validate_on_submit():
            user = Driver.byusername(login.username.data)
            if user and user.password == login.password.data:
                session['driverid'] = user.driverid
                return redirect_series(login.gotoseries.data)
            formerror = "Invalid username/password"
        else:
            formerror = login.errors

    elif reset.submit.data:
        active = "reset"
        if reset.validate_on_submit():
            for d in Driver.find(reset.firstname.data, reset.lastname.data):
                if d.email == reset.email.data:
                    token = current_app.usts.dumps({'request': 'reset', 'driverid': str(d.driverid)})
                    link = url_for('.reset', token=token, _external=True)
                    return render_template("simple.html", content="An email as been sent with a link to reset your username/password. (%s)" % link)
            formerror = "No user could be found with those parameters"
        else:
            formerror = reset.errors

    elif register.submit.data:
        active = "register"
        if register.validate_on_submit():
            if Driver.byusername(register.username.data) != None:
                formerror = "That username is already taken"
            else:
                session['driverid'] = Driver.new(register.firstname.data.strip(), register.lastname.data.strip(), register.email.data.strip(),
                                            register.username.data.strip(), register.password.data.strip())
                return redirect_series(register.gotoseries.data)
        else:
            formerror = register.errors

    login.gotoseries.data = g.series
    register.gotoseries.data = g.series
    return render_template('/register/login.html', active=active, formerror=formerror, login=login, reset=reset, register=register)
        

@Register.route("/reset", methods=['GET', 'POST'])
def reset():
    form = ResetPasswordForm()
    if form.submit.data and form.validate_on_submit():
        if 'driverid' not in session: 
            abort(400, 'No driverid present during reset, how?')
        Driver.updatepassword(session['driverid'], form.username.data, form.password.data)
        return redirect_series("")

    elif request.method == 'GET':
        if 'token' not in request.args:
            return render_template("simple.html", header="Reset Error", content="This URL is meant to be loaded from a link with a reset token")
            
        token = request.args.get('token', '')
        req   = {}
        try:
            req = current_app.usts.loads(token, max_age=3600) # 1 hour expiry
        except itsdangerous.SignatureExpired as e:
            return render_template("simple.html", header="Confirmation Error", content="Sorry, this confirmation token has expired (%s)" % e.args[0])
        except Exception as e:
            abort(400, e)
    
        if req['request'] == 'reset':
            session['driverid'] = uuid.UUID(req['driverid'])
            return render_template("register/reset.html", form=form, formerror="")

    elif form.errors:
        return render_template("register/reset.html", form=form, formerror=form.errors)

    abort(400)

 
def redirect_series(series=""):
    if series and series in Series.active():
        return redirect(url_for(".series", series=series))
    return redirect(url_for(".index"))



class RegisterController():

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

