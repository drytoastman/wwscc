"""
    Handlers for dynamically updated pages like the announcer page.  This will take
    a lot of results of the results table but is also free to the use other series
    tables as needed.  It will not function with offline series
"""
import logging
import time
import urllib
import uuid
import math

from flask import Blueprint, abort, current_app, g, get_template_attribute, request, render_template
from nwrsc.lib.encoding import json_encode
from nwrsc.model import *
from nwrsc.lib.misc import csvlist

log = logging.getLogger(__name__)
Timer = Blueprint("Timer", __name__)
Announcer = Blueprint("Announcer", __name__) 

MAX_WAIT = 30

@Timer.route("/<float:lasttime>")
def timer(lasttime):
    """ Proxy this request to local data entry machine if we can """
    try :
        if current_app.config['SHOWLIVE']:
            f = urllib.request.urlopen("http://127.0.0.1:9090/timer/%0.3lf" % lasttime, timeout=MAX_WAIT);
            return f.read()
        else:
            time.sleep(1)
            abort(403, "not an onsite server")

    except EnvironmentError as e:
        time.sleep(1) # slow down any out of control loops
        abort(404, e)
            

@Announcer.route("/")
def eventlist():
    return "event list here someday"

@Announcer.route("/<int:eventid>")
def index():
    g.event = Event.get(g.eventid)
    return render_template('/announcer/main.html')

@Announcer.route("/<int:eventid>/next")
def nextresult():
    # use ceil so round off doesn't cause an infinite loop
    modified = math.ceil(float(request.args.get('modified', '0')))

    # Long polling, hold the connection until something is actually new
    then = time.time()
    while True:
        result = Run.getLast(g.eventid, modified)
        if result != []:
            data = loadAnnouncerResults(result[0].carid)
            data['modified'] = result[0].modified.timestamp()
            return json_encode(data)
        if time.time() > then + MAX_WAIT:  # wait max to stop forever threads
            return json_encode({})
        time.sleep(0.8)

def loadAnnouncerResults(carid):
    settings  = Settings.get()
    classdata = ClassData.get()
    event     = Event.get(g.eventid)
    results   = Result.getEventResults(g.eventid)
    champ     = Result.getChampResults()
    nextid    = RunOrder.getNextCarIdInOrder(carid, g.eventid)
    tttable   = get_template_attribute('/results/ttmacros.html', 'toptimestable')
    order     = list()

    def entrant_tables(cid):
        (group, driver) = Result.getDecoratedClassResults(settings, results, cid)
        if classdata.classlist[driver['classcode']].champtrophy:
            decchamp = Result.getDecoratedChampResults(champ, driver)
        else:
            decchamp = "Not a champ class"
        return render_template('/announcer/entrant.html', event=event, driver=driver, group=group, champ=decchamp)

    for n in RunOrder.getNextRunOrder(carid, g.eventid):
        for e in results[n.classcode]:
            if e['carid'] == n.carid:
                order.append((e, Result.getBestNetRun(e)))
                break

    data = {}
    data['last']   = entrant_tables(carid)
    data['next']   = entrant_tables(nextid)
    data['order']  = render_template('/announcer/runorder.html', order=order)
    data['topnet'] = tttable(Result.getTopTimesTable(classdata, results, {'indexed':True, 'counted':False}, carid=carid))
    data['topraw'] = tttable(Result.getTopTimesTable(classdata, results, {'indexed':False, 'counted':False}, carid=carid))
    for ii in range(1, event.segments+1):
        ret['topseg%d'% ii] = toptimestable(Result.getTopTimesTable(classdata, results, {'seg':ii}, carid=carid))

    return data


class LiveController(object):

    def index(self):
        if self.eventid:
            return self._browser()
        elif self.database is not None:
            return self._events()
        else:
            return self._database()

    def _database(self):
        c.dblist = self._databaseList(archived=False)
        return render_mako('/live/database.mako')

    def _events(self):
        c.events = self.session.query(Event).all()
        return render_mako('/live/events.mako')

    def _browser(self):
        c.event = self.event
        c.classes = [x[0] for x in self.session.query(Class.code).all()]
        return render_mako('/live/browser.mako')

    def Event(self):
        carid = int(self.routingargs.get('other', 0))
        c.results = self._classlist(carid)
        return render_mako_def('/live/tables.mako', 'classlist')

    def Champ(self):
        carid = int(self.routingargs.get('other', 0))
        c.champ = self._champlist(carid)
        c.cls = self.cls
        return render_mako_def('/live/tables.mako', 'champlist')

    def PAX(self):
        carid = int(self.routingargs.get('other', 0))
        c.toptimes = self._loadTopTimes(carid, raw=False)
        return render_mako('/announcer/topnettimes.mako').replace('\n', '')

    def Raw(self):
        carid = int(self.routingargs.get('other', 0))
        c.toptimes = self._loadTopTimes(carid, raw=True)
        return render_mako('/announcer/toprawtimes.mako').replace('\n', '')
