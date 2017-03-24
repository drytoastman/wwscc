
import logging
import time
import urllib

from flask import Blueprint, abort, current_app, g, request, render_template
from nwrsc.lib.encoding import json_encode
from nwrsc.model import *
from nwrsc.lib.misc import csvlist

log = logging.getLogger(__name__)
Timer = Blueprint("Timer", __name__)
Announcer = Blueprint("Announcer", __name__) 

@Timer.route("/<float:lasttime>")
def timer(lasttime):
    """ Proxy this request to local data entry machine if we can """
    try :
        if current_app.config['SHOWLIVE']:
            f = urllib.request.urlopen("http://127.0.0.1:9090/timer/%0.3lf" % lasttime);
            return f.read()
        else:
            time.sleep(1)
            abort(403, "not an onsite server")

    except EnvironmentError as e:
        time.sleep(1) # slow down any out of control loops
        abort(404, e)
            

@Announcer.route("/")
def index():
    return "indexpage"

@Announcer.route("/<int:eventid>")
def base():
    g.event = Event.get(g.eventid)
    return render_template('/announcer/main.html')

@Announcer.route("/<int:eventid>/last")
def last():
    classes  = csvlist(request.args.get('classcodes',''))
    lasttime = request.args.get('time','2000-01-01')

    # Long polling, hold the connection until something is actually new
    then = time.time()
    while True:
        result = LastRun.getLast(g.eventid, lasttime, classes)
        if result != []:
            break
        if time.time() > then + 30:  # 30 second wait max to stop forever threads
            break
        time.sleep(0.8)

    return json_encode(result)

    
class AnnouncerController(object):

    def index(self):
        c.title = 'Scorekeeper Announcer'

        if self.eventid:
            c.javascript = ['/js/announcer.js']
            c.stylesheets = ['/css/announcer.css']
            c.event = self.event
            return render_mako('/announcer/main.mako')
        elif self.database is not None:
            c.events = self.session.query(Event).all()
            return render_mako('/results/eventselect.mako')
        else:
            return self.databaseSelector()

    def runorder(self):
        """
            Returns the HTML to render the NextToFinish box
        """
        c.order = loadNextRunOrder(self.session, self.event, int(request.GET.get('carid', 0)))
        return render_mako('/announcer/runorder.mako')


    #@jsonify
    def toptimes(self):
        """
            Returns the top times tables that are shown in the announer panel
        """
        carid = int(request.GET.get('carid', 0))
        ##data = self._toptimes(int(request.GET.get('carid', 0)))
        c.e2label = self.e2label

        ret = {}
        ret['updated'] = int(request.GET.get('updated', 0)) # Return it

        c.toptimes = self._loadTopTimes(carid, raw=False) #data['topnet']
        ret['topnet'] = render_mako('/announcer/topnettimes.mako').replace('\n', '')

        c.toptimes = self._loadTopTimes(carid, raw=True) #data['topraw']
        ret['topraw'] = render_mako('/announcer/toprawtimes.mako').replace('\n', '')

        if self.event.getSegmentCount() > 0:
            for ii in range(1, self.event.getSegmentCount()+1):
                c.toptimes = data['topseg%d' % ii]
                ret['topseg%d' % ii] = render_mako('/announcer/topsegtimes.mako').replace('\n', '')

        return ret


    def nexttofinish(self):
        nextid = getNextCarIdInOrder(self.session, self.event, int(request.GET.get('carid', 0)))
        return self._allentrant(nextid)

    def results(self):
        return self._allentrant(int(request.GET.get('carid', 0)))

    def _allentrant(self, carid):
        res = Result.getEventResults(g.eventid)
        ann = Result.getAnnouncerDetails(Settings.get(), res, carid)

        data = self._entrant(carid)
        c.event = self.event
        c.runs = data['runlist']
        c.results = data['classlist']
        c.champ = data['champlist']
        c.driver = self.driver
        c.cls = self.cls
        c.e2label = self.e2label
        
        ret = {}
        ret['updated'] = int(request.GET.get('updated', 0)) # Return it
        ret['entrantresult'] = render_mako('/announcer/entrant.mako').replace('\n', '')
        return ret


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
        c.e2label = self.e2label
        return render_mako_def('/live/tables.mako', 'classlist')

    def Champ(self):
        carid = int(self.routingargs.get('other', 0))
        c.champ = self._champlist(carid)
        c.cls = self.cls
        c.e2label = self.e2label
        return render_mako_def('/live/tables.mako', 'champlist')

    def PAX(self):
        carid = int(self.routingargs.get('other', 0))
        c.e2label = self.e2label
        c.toptimes = self._loadTopTimes(carid, raw=False)
        return render_mako('/announcer/topnettimes.mako').replace('\n', '')

    def Raw(self):
        carid = int(self.routingargs.get('other', 0))
        c.e2label = self.e2label
        c.toptimes = self._loadTopTimes(carid, raw=True)
        return render_mako('/announcer/toprawtimes.mako').replace('\n', '')



CONVERT = {'old':'improvedon', 'raw':'couldhave', 'current':'highlight'}

class MobileController(object):

    def __before__(self):
        self.eventid = self.routingargs.get('eventid', None)
        try:
            self.eventid = int(self.eventid)
            self.event = self.session.query(Event).get(self.eventid)
        except (ValueError, TypeError):
            pass

    def _encode(self, head, o):
        response.headers['Content-type'] = 'text/javascript'
        return JEncoder(indent=1).encode(o)

    def index(self):
        if self.database is None:
            return self._encode("serieslist", self._databaseList())
        elif self.eventid is None:
            return self._encode("events", self.session.query(Event).all())
        elif self.eventid == 'classes':
            return self._encode("classes", self.session.query(Class).all())
        elif self.eventid == 'indexes':
            return self._encode("indexes", self.session.query(Index).all())
        else:
            return self._encode("nothing", [])


    def topnet(self):
        carid = int(self.routingargs.get('other', 0))
        return self._encode("toptimes", self._loadTopTimes(carid, False))


    def topraw(self):
        carid = int(self.routingargs.get('other', 0))
        return self._encode("toptimes", self._loadTopTimes(carid, True))


    def _loadTopTimes(self, carid, raw=False):
        classdata = ClassData(self.session)
        car = self.session.query(Car).get(carid)
        self.announcer = self.session.query(AnnouncerData).filter(AnnouncerData.eventid==self.eventid).filter(AnnouncerData.carid==carid).first()
        if self.announcer is None:
            raise BeforePage('')
        index = classdata.getEffectiveIndex(car)

        toptimes = TopTimesStorage(self.session, self.event, classdata)
        if raw:
            ret = self._convertTTS(toptimes.getList(allruns=False, raw=True, course=0), carid, self.announcer.oldsum/index, self.announcer.potentialsum/index)
        else:
            ret = self._convertTTS(toptimes.getList(allruns=False, raw=False, course=0), carid, self.announcer.oldsum, self.announcer.potentialsum)
        return ret


    def _convertTTS(self, tts, carid, oldsum, rawsum):
        position = 1
        additions = list()
        
        for entry in tts.rows:
            entry.position = position
            position += 1
            if entry.carid == carid:
                entry.label = 'current'
                if oldsum is not None and oldsum > 0:
                    additions.append(entry.copyWith(position='old', toptime=t3(oldsum), label='old'))
                if rawsum is not None and rawsum > 0:
                    additions.append(entry.copyWith(position='raw', toptime=t3(rawsum), label='raw'))

        if additions:
            tts.rows.extend(additions)
            tts.rows.sort(key=lambda x: float(x.toptime))
            tts.rows.sort(key=lambda x: int(x.courses), reverse=True)

        return tts.rows


    def _entrant(self, carid):
        return {
            'runlist': self._runlist(carid),
            'classlist': self._classlist(carid),
            'champlist': self._champlist(carid)
        }
        

    def runlist(self):
        carid = int(self.routingargs.get('other', 0))
        return self._encode("runlist", self._runlist(int(carid)))

    def _runlist(self, carid):
        self.announcer = self.session.query(AnnouncerData).filter(AnnouncerData.eventid==self.eventid).filter(AnnouncerData.carid==carid).first()
        if self.announcer is None:
            raise BeforePage('no announcer data')

        query = self.session.query(Run).filter(Run.carid==carid).filter(Run.eventid==self.eventid)
        query = query.filter(Run.course==self.announcer.lastcourse).filter(Run.course==self.announcer.lastcourse).order_by(Run.run)
        runs = query.all()
        if self.announcer.rawdiff:
            runs[-1].rawdiff = self.announcer.rawdiff
        if self.announcer.netdiff:
            runs[-1].netdiff = self.announcer.netdiff
        for r in runs:
            if r.norder == 1: r.label = 'current'
            if r.norder == 2 and self.announcer.oldsum > 0: r.label = 'old'
        if runs[-1].norder != 1 and self.announcer.potentialsum > 0:
            runs[-1].label = 'raw'
            
        return runs


    def classlist(self):
        carid = int(self.routingargs.get('other', 0))
        return self._encode("classlist", self._classlist(int(carid)))

    def _classlist(self, carid):
        (self.driver, self.car) = self.session.query(Driver,Car).join('cars').filter(Car.id==carid).first()
        if self.driver.alias and not config['nwrsc.private']:
            self.driver.firstname = self.driver.alias
            self.driver.lastname = ""
        self.announcer = self.session.query(AnnouncerData).filter(AnnouncerData.eventid==self.eventid).filter(AnnouncerData.carid==carid).first()
        if self.announcer is None:
            raise BeforePage('no announcer data')

        ret = []
        classdata = ClassData(self.session)
        savecourses = 0
        for res in getClassResultsShort(self.session, self.settings, self.event, classdata, self.car.classcode):
            ret.append(_extract(res, 'sum', 'pospoints', 'diffpoints', 'carid', 'firstname', 'lastname', 'indexstr', 'position', 'trophy', 'diff', 'courses'))
            if res.carid == carid:
                ret[-1]['label'] = "current"
                savecourses = ret[-1]['courses']

        if self.announcer.oldsum > 0:
            ret.append({'sum': t3(self.announcer.oldsum), 'firstname':self.driver.firstname, 'lastname':self.driver.lastname, 'position':'old', 'label':'old', 'courses':savecourses})
        if self.announcer.potentialsum > 0:
            ret.append({'sum': t3(self.announcer.potentialsum), 'firstname':self.driver.firstname, 'lastname':self.driver.lastname, 'position':'raw', 'label':'raw', 'courses':savecourses})
        ret.sort(key=lambda x: float(x['sum']))
        ret.sort(key=lambda x: int(x['courses']), reverse=True)
        return ret


    def champlist(self):
        carid = int(self.routingargs.get('other', 0))
        return self._encode("champlist", self._champlist(int(carid)))

    def _champlist(self, carid):
        self.car = self.session.query(Car).get(carid)
        self.cls = self.session.query(Class).filter(Class.code==self.car.classcode).first()
        self.announcer = self.session.query(AnnouncerData).filter(AnnouncerData.eventid==self.eventid).filter(AnnouncerData.carid==carid).first()
        if self.announcer is None:
            raise BeforePage('no announcer data')

        ret = []
        pos = 1
        for res in getChampResults(self.session, self.settings, self.cls.code).get(self.cls.code, []):
            entry = dict()
            entry['points'] = t3(res.points.total)
            entry['carid'] = res.carid
            entry['driverid'] = res.id
            entry['firstname'] = res.firstname
            entry['lastname'] = res.lastname
            entry['events'] = res.events
            entry['position'] = pos
            ret.append(entry)
            pos += 1

            if res.id != self.car.driverid:
                continue
            
            entry['label'] = 'current'
            if res.points == res.pospoints:
                if self.announcer.oldpospoints > 0:
                    entry = entry.copy()
                    entry['position'] = "old"
                    entry['label'] = "old"
                    entry['points'] = res.pospoints.theory(self.eventid, self.announcer.oldpospoints)
                    ret.append(entry)
                if self.announcer.potentialpospoints > 0:
                    entry = entry.copy()
                    entry['position'] = "raw"
                    entry['label'] = "raw"
                    entry['points'] = res.pospoints.theory(self.eventid, self.announcer.potentialpospoints)
                    ret.append(entry)

            if res.points == res.diffpoints:
                if self.announcer.olddiffpoints > 0:
                    entry = entry.copy()
                    entry['position'] = "old"
                    entry['label'] = "old"
                    entry['points'] = t3(res.diffpoints.theory(self.eventid, self.announcer.olddiffpoints))
                    ret.append(entry)
                if self.announcer.potentialdiffpoints > 0:
                    entry = entry.copy()
                    entry['position'] = "raw"
                    entry['label'] = "raw"
                    entry['points'] = t3(res.diffpoints.theory(self.eventid, self.announcer.potentialdiffpoints))
                    ret.append(entry)

        ret.sort(key=lambda x: float(x['points']), reverse=True)
        return ret

