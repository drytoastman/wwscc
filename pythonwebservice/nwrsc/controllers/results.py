
from flask import Blueprint, request, abort, render_template, get_template_attribute, make_response, g
from nwrsc.model import *
from nwrsc.lib.bracket import Bracket

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

@Results.route("/<int:eventid>/bracket/<int:challengeid>")
def bracket(challengeid):
    challenge = Challenge.get(challengeid)
    if challenge is None:
        abort(404, "Invalid or no challenge id")
    (coords, size) = Bracket.coords(challenge.depth)
    return render_template('/challenge/bracketbase.html', challengeid=challenge.challengeid, coords=coords, size=size)

@Results.route("/<int:eventid>/bracketimg/<int:challengeid>")
def bracketimg(challengeid):
    challenge = Challenge.get(challengeid)
    if challenge is None:
        abort(404, "Invalid or no challenge id")
    results = Challenge.getResults(challengeid)
    response = make_response(Bracket.image(challenge.depth, results))
    response.headers['Content-type'] = 'image/png'
    return response

@Results.route("/<int:eventid>/bracketround/<int:challengeid>/<int:round>")
def bracketround(challengeid, round):
    chal = Challenge.get(challengeid)
    if challenge is None:
        abort(404, "Invalid or no challenge id")
    results = Challenge.getResults(challengeid, round)
    roundReport = get_template_attribute('/challenge/challengemacros.html', 'roundReport')
    return roundReport(results[round])


@Results.route("/<int:eventid>/challenge/<int:challengeid>")
def challenge(challengeid):
    chal = Challenge.get(challengeid)
    results = Challenge.getResults(challengeid)
    return render_template('/challenge/challengereport.html', results=results, chal=chal)

@Results.route("/<int:eventid>/grid")
def grid():
    return "grid"

@Results.route("/<int:eventid>/dialins")
def dialins():
    return "dialins"

