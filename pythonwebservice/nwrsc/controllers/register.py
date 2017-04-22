import datetime
import logging
import uuid
import time
import itsdangerous
from collections import defaultdict

from flask import abort, Blueprint, current_app, flash, g, redirect, request, render_template, session, url_for

from nwrsc.model import *
from nwrsc.lib.forms import *
from nwrsc.lib.encoding import json_encode, ical_encode

log = logging.getLogger(__name__)

Register = Blueprint("Register", __name__) 

@Register.before_request
def setup():
    g.title = 'Scorekeeper Registration'
    g.activeseries = Series.active()
    g.selection = request.endpoint
    if 'driverid' in session:
        g.driver = Driver.get(session['driverid'])
    else:
        g.driver = None

####################################################################
# Authenticated functions

@Register.route("/")
def index():
    if not g.driver: return login()
    g.selection = 'Register.events'
    return render_template('register/serieslist.html', subapp='register', serieslist=Series.active())

@Register.route("/<series>/")
def series():
    """ If logged in, just redirect to events """
    if not g.driver: return login()
    return redirect(url_for('.events'))


@Register.route("/profile")
@Register.route("/<series>/profile")
def profile():
    if not g.driver: return login()
    g.driver    = Driver.get(session['driverid'])
    g.classdata = ClassData.get()
    form        = ProfileForm()
    upcoming    = getAllUpcoming(g.driver.driverid)
    attrBaseIntoForm(g.driver, form)
    return render_template('register/profile.html', form=form, upcoming=upcoming)

@Register.route("/profilepost", methods=['POST'])
@Register.route("/<series>/profilepost", methods=['POST'])
def profilepost():
    form = ProfileForm()
    if form.validate_on_submit():
        formIntoAttrBase(form, g.driver)
        g.driver.update()
    flashformerrors(form)
    return redirect(url_for('.profile'))


@Register.route("/<series>/cars")
def cars():
    if not g.driver: return login()
    g.classdata = ClassData.get()
    carform = CarForm(g.classdata)
    events  = {e.eventid:e for e in Event.byDate()}
    cars    = {c.carid:c   for c in Car.getForDriver(g.driver.driverid)}
    active  = defaultdict(set)
    for carid,eventid in Driver.activecars(g.driver.driverid):
        active[carid].add(eventid)
    return render_template('register/cars.html', events=events, cars=cars, active=active, carform=carform)


@Register.route("/<series>/carspost", methods=['POST'])
def carspost():
    if not g.driver: return login()
    g.classdata = ClassData.get()
    carform     = CarForm(g.classdata)

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
                flash("Invalid request ({})".format(action))
        else:
            flashformerrors(carform)

    except Exception as e:
        g.db.rollback()
        flash(str(e))
    return redirect(url_for('.cars'))


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
    for e in events:
        e.drivercount  = e.getDriverCount()
        e.entrycount   = e.getCount()
        mycount        = len(registered[e.eventid])

        limits = [[999, ""],]
        if e.sinlimit and e.drivercount >= e.sinlimit and mycount == 0:
            limits.append([0,          "The single entry limit of {} has been met".format(e.sinlimit)])
        if e.perlimit:
            limits.append([e.perlimit, "The personal entry limit of {} has been met".format(e.perlimit)])
        if e.totlimit:
            limits.append([e.totlimit - e.entrycount + mycount, "The total entry limit of {} has been met".format(e.totlimit)])

        (e.mylimit, e.limitmessage) = min(limits, key=lambda x: x[0])

    return render_template('register/events.html', events=events, cars=cars, registered=registered, payments=payments)


@Register.route("/<series>/eventspost", methods=['POST'])
def eventspost():
    if not g.driver: return login()
    try:
        carids = [uuid.UUID(k) for (k,v) in request.form.items() if v == 'y' or v is True]
        eventid = int(request.form['eventid'])
        Registration.update(eventid, carids, g.driver.driverid)
    except Exception as e:
        g.db.rollback()
        flash(str(e))
    return redirect(url_for('.events'))


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
    return ical_encode(getAllUpcoming(driverid))

@Register.route("/login", methods=['POST', 'GET'])
def login():
    if g.driver: return redirect_series()
    if request.form.get('message'): # super simple bot test (advanced bots will get by this)
        abort(404)

    login = PasswordForm(prefix='login')
    reset = ResetForm(prefix='reset')
    register = RegisterForm(prefix='register')
    active = "login"

    if login.submit.data:
        if login.validate_on_submit():
            user = Driver.byusername(login.username.data)
            if user and user.password == login.password.data:
                session['driverid'] = user.driverid
                return redirect_series(login.gotoseries.data)
            flash("Invalid username/password")
        else:
            flashformerrors(login.errors)

    elif reset.submit.data:
        active = "reset"
        if reset.validate_on_submit():
            for d in Driver.find(reset.firstname.data, reset.lastname.data):
                if d.email == reset.email.data:
                    token = current_app.usts.dumps({'request': 'reset', 'driverid': str(d.driverid)})
                    link = url_for('.reset', token=token, _external=True)
                    return render_template("simple.html", content="An email as been sent with a link to reset your username/password. (%s)" % link)
            flash("No user could be found with those parameters")
        else:
            flashformerrors(reset.errors)

    elif register.submit.data:
        active = "register"
        if register.validate_on_submit():
            if Driver.byusername(register.username.data) != None:
                flash("That username is already taken")
            else:
                session['driverid'] = Driver.new(register.firstname.data.strip(), register.lastname.data.strip(), register.email.data.strip(),
                                            register.username.data.strip(), register.password.data.strip())
                return redirect_series(register.gotoseries.data)
        else:
            flashformerrors(register.errors)

    login.gotoseries.data = g.series
    register.gotoseries.data = g.series
    return render_template('/register/login.html', active=active, login=login, reset=reset, register=register)
        

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
            return render_template("register/reset.html", form=form)

    elif form.errors:
        return render_template("register/reset.html", form=form, formerror=form.errors)

    abort(400)

 
####################################################################
# Utility functions

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
 
def checkLimitState(event, driverid, setResponse=True):
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
    

