"""
  This is the code for the results pages. Everything should be taken from the results table so
  that it continues to work after old series are expunged.
"""
from operator import itemgetter

from flask import Blueprint, request, abort, render_template, get_template_attribute, make_response, g
from nwrsc.model import Result, ClassData, Event, Challenge
from nwrsc.lib.bracket import Bracket

Results = Blueprint("Results", __name__)

## The indexes and lists

def eventfromlist(info, eventid):
    for e in info['events']:
        if e['eventid'] == eventid:
            return Event(**e)
    return None


@Results.route("/")
def index():
    return render_template('results/eventlist.html', events=Result.getSeriesInfo()['events'])

@Results.route("/<int:eventid>/")
def event():
    info    = Result.getSeriesInfo()
    results = Result.getEventResults(g.eventid)
    active  = results.keys()
    event   = eventfromlist(info, g.eventid)
    challenges = [Challenge(**c) for c in info['challenges'] if c['eventid'] == event.eventid]
    return render_template('results/eventindex.html', event=event, active=active, challenges=challenges)


## Basic results display

def _resultsforclasses(clslist=None, grplist=None):
    """ Show our class results """
    info        = Result.getSeriesInfo()
    resultsbase = Result.getEventResults(g.eventid)
    g.classdata = ClassData(info['classes'], info['indexes'])
    g.event     = eventfromlist(info, g.eventid)

    if clslist is None and grplist is None:
        ispost         = True
        results        = resultsbase
        g.toptimes     = Result.getTopTimesTable(results, {'indexed':True}, {'indexed':False})
        g.entrantcount = sum([len(x) for x in results.values()])
        g.settings     = info['settings']
    elif grplist is not None:
        ispost         = False
        results        = dict()
        for code, entries in resultsbase.items():
            for e in entries:
                if e['rungroup'] in grplist:
                    results[code] = entries
                    break
    else:
        ispost         = False
        results        = { k: resultsbase[k] for k in (set(clslist) & set(resultsbase.keys())) }

    return render_template('results/eventresults.html', ispost=ispost, results=results)


@Results.route("/<int:eventid>/byclass")
def byclass():
    classes = request.args.get('list', '')
    g.title = "Results For Class {}".format(classes)
    return _resultsforclasses(clslist=classes.split(','))

@Results.route("/<int:eventid>/bygroup")
def bygroup():
    groups = request.args.get('list', '')
    g.title = "Results For Group {}".format(groups)
    return _resultsforclasses(grplist=[int(x) for x in groups.split(',')])

@Results.route("/<int:eventid>/post")
def post():
    return _resultsforclasses()

@Results.route("/champ")
def champ():
    return render_template('/results/champ.html', champ=Result.getChampResults())


## Special display for toptimes, brackets, grids, etc

@Results.route("/<int:eventid>/tt")
def tt():
    indexed  = bool(int(request.args.get('indexed', '1')))
    counted  = bool(int(request.args.get('counted', '1')))
    segments = bool(int(request.args.get('segments', '0')))
    course   = int(request.args.get('course', '0'))

    info     = Result.getSeriesInfo()
    event    = eventfromlist(info, g.eventid)

    keys = []
    if segments:
        return "Implement the top segment times now. :)"
    elif course == 0 and event.courses > 1:
        keys.extend([{'indexed':indexed, 'counted':counted, 'course':c, 'title':c and "Course {}".format(c) or "Total"} for c in range(event.courses+1)])
    elif course == 0:
        keys.append({'indexed':indexed, 'counted':counted, 'course':0, 'title':'Top Times'})
    else:
        keys.append({'indexed':indexed, 'counted':counted, 'course':course, 'title':'Course {}'.format(course)})

    header   = "Top {} Times ({} Runs) for {}".format(indexed and "Indexed" or "", counted and "Counted" or "All", event.name)
    table    = Result.getTopTimesTable(Result.getEventResults(g.eventid), *keys)

    return render_template('/results/toptimes.html', header=header, table=table)


def _loadChallengeResults(challengeid, load=True):
    info = Result.getSeriesInfo()
    challenge = None
    for c in info['challenges']:
        if c['challengeid'] == challengeid:
            challenge = Challenge(**c)
    if challenge is None:
        abort(404, "Invalid or no challenge id")
    return (challenge, load and Result.getChallengeResults(challengeid) or None)

@Results.route("/<int:eventid>/bracket/<int:challengeid>")
def bracket(challengeid):
    (challenge, results) = _loadChallengeResults(challengeid, load=False)
    (coords, size) = Bracket.coords(challenge.depth)
    return render_template('/challenge/bracketbase.html', challengeid=challengeid, coords=coords, size=size)

@Results.route("/<int:eventid>/bracketimg/<int:challengeid>")
def bracketimg(challengeid):
    (challenge, results) = _loadChallengeResults(challengeid)
    response = make_response(Bracket.image(challenge.depth, results))
    response.headers['Content-type'] = 'image/png'
    return response

@Results.route("/<int:eventid>/bracketround/<int:challengeid>/<int:round>")
def bracketround(challengeid, round):
    (challenge, results) = _loadChallengeResults(challengeid)
    roundReport = get_template_attribute('/challenge/challengemacros.html', 'roundReport')
    return roundReport(results[round])

@Results.route("/<int:eventid>/challenge/<int:challengeid>")
def challenge(challengeid):
    (challenge, results) = _loadChallengeResults(challengeid)
    return render_template('/challenge/challengereport.html', results=results, chal=challenge)

@Results.route("/<int:eventid>/dialins")
def dialins():
    orderkey = request.args.get('order', 'net')
    if orderkey not in ('net', 'prodiff'):
        return "Invalid order key"

    info    = Result.getSeriesInfo()
    results = Result.getEventResults(g.eventid)
    event   = eventfromlist(info, g.eventid)
    entrants = [e for cls in results.values() for e in cls]
    entrants.sort(key=itemgetter(orderkey))
    return render_template('/challenge/dialins.html', orderkey=orderkey, event=event, entrants=entrants)


## Perhaps move these to an admin like site?

@Results.route("/<int:eventid>/audit")
def audit():
    course = request.args.get('course', 1)
    group  = request.args.get('group', 1)
    order  = request.args.get('order', 'runorder')
    event  = Event.get(g.eventid)
    audit  = Audit.audit(event, course, group)

    if order in ['firstname', 'lastname']:
        audit.sort(key=lambda obj: str.lower(str(getattr(obj, order))))
    if order in ['runorder']:
        audit.sort(key=lambda obj: obj.row)

    return render_template('/results/audit.html', audit=audit, event=event, course=course, group=group, order=order)

@Results.route("/<int:eventid>/grid")
def grid():
    return "grid"

