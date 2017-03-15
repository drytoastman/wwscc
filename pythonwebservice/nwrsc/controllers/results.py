"""
  This is the code for the results pages. Everything should be taken from the results table so
  that it continues to work after old series are expunged.
"""

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
    return render_template('results/eventindex.html')


## Basic results display

def _resultsforclasses(clslist=None):
    """ Show our class results, if the classlist is None, we are posting for the event """
    if clslist == [] or clslist == ['']:
        return "No classes with recorded runs for this request"

    g.event     = Event.get(g.eventid)
    g.classdata = ClassData.get()
    results     = EventResult.get(g.event)
    ispost      = clslist is None

    if ispost:
        g.results      = results
        g.toptimes     = TopTimesAccessor(g.event, g.results)
        g.entrantcount = sum([len(x) for x in g.results.values()])
        g.settings     = Settings.get()
    else:
        g.results = { k: results[k] for k in (set(clslist) & set(results.keys())) }

    return render_template('results/eventresults.html', ispost=ispost)


@Results.route("/<int:eventid>/byclass")
def byclass():
    g.title = "Results For Class {}".format(request.args.get('list', ''))
    return _resultsforclasses(clslist=request.args.get('list','').split(','))

@Results.route("/<int:eventid>/bygroup")
def bygroup():
    g.title = "Results For Group {}".format(request.args.get('list', ''))
    groups = [int(x) for x in request.args.get('list', '').split(',')]
    return _resultsforclasses(clslist=RunGroup.getClassesForRunGroup(g.eventid, groups))

@Results.route("/<int:eventid>/post")
def post():
    return _resultsforclasses()

@Results.route("/champ")
def champ():
    return "champ"

@Results.route("/<int:eventid>/audit")
def audit():
    course = request.args.get('course', 1)
    group  = request.args.get('group', 1)
    order  = request.args.get('order', 'runorder')
    event  = Event.get(g.eventid)
    audit  = EventResult.audit(event, course, group)

    if order in ['firstname', 'lastname']:
        audit.sort(key=lambda obj: str.lower(str(getattr(obj, order))))
    if order in ['runorder']:
        audit.sort(key=lambda obj: obj.row)

    return render_template('/results/audit.html', audit=audit, event=event, course=course, group=group, order=order)


## Special display for toptimes, brackets, grids, etc

@Results.route("/<int:eventid>/tt")
def tt():
    net      = bool(int(request.args.get('net', '1')))
    counted  = bool(int(request.args.get('counted', '1')))
    course   = int(request.args.get('course', '0'))

    event    = Event.get(g.eventid)
    results  = EventResult.get(event)
    toptimes = TopTimesAccessor(event, results)
    header   = "Top {} Times ({} Runs) for {}".format(net and "Net" or "Raw", counted and "Counted" or "All", event.name)

    if event.courses > 1:
        table = toptimes.getLists(*[{'net':net, 'counted':counted, 'course':c, 'title':c and "Course {}".format(c) or "Total"} for c in range(event.courses+1)])
    else:
        table = toptimes.getLists({'net':net, 'counted':counted, 'course':0, 'title':'Top Times'})

    return render_template('/results/toptimes.html', header=header, table=table)

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

