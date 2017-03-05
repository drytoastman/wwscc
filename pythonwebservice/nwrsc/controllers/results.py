
from flask import Blueprint, request, render_template, g
from nwrsc.model import *

Results = Blueprint("Results", __name__)

@Results.route("/")
def index():
    return render_template('results/eventlist.html', eventlist=Event.byDate())

@Results.route("/<int:eventid>/")
def event():
    g.event = Event.get(g.eventid)
    g.active = Class.activeClasses(g.eventid)
    g.challenges = Challenge.getForEvent(g.eventid)
    return render_template('results/event.html')

@Results.route("/<int:eventid>/byclass")
def byclass():
    g.event = Event.get(g.eventid)
    g.results = EventResult.get(g.eventid)
    return render_template('results/classresult.html')
#    return "looking for %s" % request.args.get('list')

@Results.route("/<int:eventid>/bygroup")
def bygroup():
    return "looking for %s, %s" % (request.args.get('course'), request.args.get('list'))

@Results.route("/<int:eventid>/tt<tttype>")
def tt(tttype):
    return "tt %s %s" % (g.eventid, tttype)

@Results.route("/<int:eventid>/bracket")
def bracket():
    return "bracket"

@Results.route("/<int:eventid>/challenge")
def challenge():
    return "challenge"

@Results.route("/<int:eventid>/all")
def all():
    return "all"

@Results.route("/<int:eventid>/post")
def post():
    return "post"

@Results.route("/<int:eventid>/grid")
def grid():
    return "grid"

@Results.route("/<int:eventid>/dialins")
def dialins():
    return "dialins"

@Results.route("/champ")
def champ():
    return "champ"

