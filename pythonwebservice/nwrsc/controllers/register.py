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
from nwrsc.lib.encoding import json_encode

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
    return render_template('register/serieslist.html', subapp='register', serieslist=Series.active())

@Register.route("/<series>/")
def series():
    """ If logged in, just redirect to events """
    if not g.driver: return login()
    return redirect(url_for('.events'))

@Register.route("/profile", methods=['POST', 'GET'])
@Register.route("/<series>/profile", methods=['POST', 'GET'])
def profile():
    if not g.driver: return login()
    g.driver = Driver.get(session['driverid'])
    form = ProfileForm()

    if form.validate_on_submit():
        formIntoAttrBase(form, g.driver)
        g.driver.update()

    g.classdata = ClassData.get()
    upcoming = getAllUpcoming(g.driver.driverid)
    attrBaseIntoForm(g.driver, form)
    return render_template('register/profile.html', form=form, formerror=form.errors, upcoming=upcoming)


@Register.route("/<series>/cars", methods=['POST', 'GET'])
def cars():
    if not g.driver: return login()
    g.classdata = ClassData.get()
    carform     = CarForm()
    carform.classcode.choices = [(c.classcode, "%s - %s" % (c.classcode, c.descrip)) for c in sorted(g.classdata.classlist.values(), key=attrgetter('classcode'))]
    carform.indexcode.choices = [(i.indexcode, "%s - %s" % (i.indexcode, i.descrip)) for i in sorted(g.classdata.indexlist.values(), key=attrgetter('indexcode'))]
    formerror = ""

    if request.method == 'POST':
        try:
            action = request.form.get('submit')
            if action == 'Delete':
                Car.delete(request.form.get('carid', ''), g.driver.driverid)
            elif carform.validate():
                car = Car()
                formIntoAttrBase(carform, car)
                if action == 'Update':
                    car.update(g.driver.driverid)
                elif action == 'Create':
                    car.new(g.driver.driverid)
                else:
                    formerror = "Invalid request ({})".format(action)

        except Exception as e:
            g.db.rollback()
            formerror = str(e)

    events = {e.eventid:e for e in Event.byDate()}
    cars   = {c.carid:c   for c in Car.getForDriver(g.driver.driverid)}
    active = defaultdict(set)
    for carid,eventid in Driver.activecars(g.driver.driverid):
        active[carid].add(eventid)
    return render_template('register/cars.html', events=events, cars=cars, active=active, carform=carform, formerror=formerror or carform.errors)


@Register.route("/<series>/events", methods=['POST', 'GET'])
def events():
    if not g.driver: return login()
    formerror = ""

    if request.method == 'POST':
        try:
            carids = [uuid.UUID(k) for (k,v) in request.form.items() if v == 'y' or v is True]
            eventid = int(request.form['eventid'])
            Registration.update(eventid, carids, g.driver.driverid)
        except Exception as e:
            g.db.rollback()
            formerror = str(e)
        
    g.classdata = ClassData.get()
    events = Event.byDate()
    cars   = {c.carid:c   for c in Car.getForDriver(g.driver.driverid)}
    registered = defaultdict(list)
    payments = defaultdict(list)
    for r in Registration.getForDriver(g.driver.driverid):
        registered[r.eventid].append(r.carid)
    for p in Payment.getForDriver(g.driver.driverid):
        payments[p.eventid] = p
    for e in events:
        e.drivercount = e.getDriverCount()
        e.entrycount  = e.getCount()
        mycount = len(registered[e.eventid])
        ds = e.attr.get('doublespecial', False)
        if ds and e.totlimit and e.drivercount > e.totlimit and mycount == 0:
            e.mylimit = 0
            e.limitmessage = "This event's single entry limit of {} has been met".format(e.totlimit)
        elif not ds and e.totlimit and e.entrycount >= e.totlimit:
            e.mylimit = mycount
            e.limitmessage = "This event's prereg limit of {} has been met".format(e.totlimit)
        elif mycount >= e.perlimit:
            e.mylimit = mycount
            e.limitmessage = "You have reached this event's prereg limit of {} car(s)".format(e.perlimit)
        else:
            e.mylimit = min(e.perlimit, e.totlimit and e.totlimit-e.entrycount or 9999)
            e.limitmessage = ""

    return render_template('register/events.html', events=events, cars=cars, registered=registered, payments=payments, formerror=formerror)


@Register.route("/<series>/usednumbers")
def usednumbers():
    if not g.driver: abort(404)
    classcode = request.args.get('classcode', None)
    if classcode is None:
        return "missing data in request"

    g.settings = Settings.get()
    return json_encode(sorted(list(Car.usedNumbers(g.driver.driverid, classcode, g.settings.superuniquenumbers))))


@Register.route("/logout")
def logout():
    session.pop('driverid', None)
    return redirect(url_for('.index'))


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
    upcoming = getAllUpcoming(driverid)
    return str(upcoming)

@Register.route("/login", methods=['POST', 'GET'])
def login():
    if g.driver: return redirect_series()
    if request.form.get('message'): # super simple bot test (advanced bots will get by this)
        abort(404)

    login = PasswordForm(prefix='login')
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
    form = PasswordForm()
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
        return redirect(url_for(".events", series=series))
    return redirect(url_for(".index"))

def getAllUpcoming(driverid):
    upcoming = defaultdict(lambda: defaultdict(list))
    for s in Series.active():
        for r in Registration.getForSeries(s, driverid):
            if r.date >= datetime.date.today():
                upcoming[r.date][s, r.name].append(r)
    return upcoming
 
def _checkLimitState(event, driverid, setResponse=True):
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
    

