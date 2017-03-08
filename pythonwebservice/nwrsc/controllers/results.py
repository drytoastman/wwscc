
from flask import Blueprint, request, render_template, g
from nwrsc.model import *

Results = Blueprint("Results", __name__)

## The indexes and lists

@Results.route("/")
def index():
    return render_template('results/eventlist.html', eventlist=Event.byDate())

@Results.route("/<int:eventid>/")
def event():
    g.event = Event.get(g.eventid)
    g.active = Class.activeClasses(g.eventid)
    g.challenges = Challenge.getForEvent(g.eventid)
    return render_template('results/event.html')


## Basic results display

def _resultsforclasses(clslist=[]):
    """ Show our class results, if the classlist is zero, we are posting for the event """
    ispost  = len(clslist) == 0
    g.event = Event.get(g.eventid)
    results = EventResult.get(g.event)

    g.results      = ispost and results or { k: results[k] for k in clslist }
    g.settings     = Settings.get()
    g.entrantcount = sum([len(x) for x in g.results.values()])

    return render_template('results/classresult.html', ispost=ispost)


@Results.route("/<int:eventid>/byclass")
def byclass():
    return _resultsforclasses(clslist=request.args.get('list','').split(','))

@Results.route("/<int:eventid>/bygroup")
def bygroup():
    groups = [int(x) for x in request.args.get('list', '').split(',')]
    return _resultsforclasses(clslist=RunGroup.getClassesForRunGroup(g.eventid, groups))

@Results.route("/<int:eventid>/post")
def post():
    return _resultsforclasses()

@Results.route("/champ")
def champ():
    return "champ"



## Special display for toptimes, brackets, grids, etc

@Results.route("/<int:eventid>/tt<tttype>")
def tt(tttype):
    return "tt %s %s" % (g.eventid, tttype)

@Results.route("/<int:eventid>/bracket")
def bracket():
    return "bracket"

@Results.route("/<int:eventid>/challenge")
def challenge():
    return "challenge"


@Results.route("/<int:eventid>/grid")
def grid():
    return "grid"

@Results.route("/<int:eventid>/dialins")
def dialins():
    return "dialins"

