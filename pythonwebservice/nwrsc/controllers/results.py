
from flask import Blueprint, request, render_template, g
from nwrsc.model import *

Results = Blueprint("Results", __name__)

@Results.route("/")
def eventlist():
    return "eventlist"
#	c.events = self.session.query(Event).order_by(Event.date).all()
#	return render_mako('/results/eventselect.mako')

@Results.route("/<int:eventid>/")
def index(eventid):
    g.event = Event.get(eventid)
    g.active = Class.activeClasses(eventid)
    g.challenges = Challenge.getForEvent(eventid)
    return render_template('results/index.html')

@Results.route("/byclass/")
def byclass():
    return "looking for %s" % request.args.get('list')

@Results.route("/bygroup/")
def bygroup():
    return "looking for %s, %s" % (request.args.get('course'), request.args.get('list'))

@Results.route("/bracket/")
def bracket():
    return "bracket"

@Results.route("/challenge/")
def challenge():
    return "challenge"

