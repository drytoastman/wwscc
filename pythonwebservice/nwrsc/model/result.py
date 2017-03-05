import logging
import json
from flask import g
from math import ceil
from collections import defaultdict
from operator import attrgetter

from .base import AttrBase
from .runs import Run
from .classlist import ClassData

#from math import ceil
#from nwrsc.lib.helpers import t3

log = logging.getLogger(__name__)

class JEncoder(json.JSONEncoder):
    def default(self, o):
        if hasattr(o, 'getPublicFeed'):
            return o.getPublicFeed()
        else:
            return str(o)

class Entrant(AttrBase):
    pass

class EventResult(object):

    @classmethod
    def get(cls, eventid):
        with g.db.cursor() as cur:
            # check if we need to update the results
            cur.execute("select max(time) from serieslog where tablen in ('classlist', 'indexlist', 'events', 'cars', 'runs')")
            mods = cur.fetchone()
            cur.execute("select modified from results where series=%s and name=%s", (g.series, "e%d"%eventid))
            resmod  = cur.fetchone()
            if True: #mods is None or resmod is None or mods['max'] > resmod['modified']:
                cls.update(eventid)

            # everything should be the latest now, load and return 
            cur.execute("select data from results where series=%s and name=%s", (g.series, "e%d"%eventid))
            res = cur.fetchone()
            if res is not None:
                return res['data']
            else:
                return json.loads('{}')

    @classmethod
    def update(cls, eventid):
        """
            Creating the cached event result data for the given event.
            The event result data is {<classcode>, [<Entrant>]}.
            Each Entrant is a json object of attributes and a list of Run objects.
            Each Run object is regular run data with attributes like bestraw, bestnet assigned.
        """
        log.info("produce new data for %s %d", g.series, eventid)

        results = defaultdict(list)
        cptrs = {}

        classdata = ClassData.get()
        eventtrophy = True # FINISH ME
        indexafterpenalties = False # FINISH ME
        usepospoints = False # FINISH ME
        PPOINTS = [20, 16, 12, 10, 8, 6, 4, 1 ]

        with g.db.cursor() as cur:
            # Fetch all of the entrants (driver/car combo), place in class lists, save pointers for quicker access
            cur.execute("select d.*,c.*,d.attr as attr1, c.attr as attr2 from drivers as d join cars as c on c.driverid=d.driverid " +
                        "where c.carid in (select distinct carid from runs where eventid=%s)", (eventid,))
            for row in cur.fetchall():
                e = Entrant(**row)
                e.attrToUpper()
                e.indexstr = classdata.getIndexStr(e)
                e.indexval = classdata.getEffectiveIndex(e)
                e.runs = [[] for x in range(g.event.courses)]
                results[e.classcode].append(e)
                cptrs[e.carid] = e
            
            # Fetch all of the runs, calc net and assign to the correct entrant
            cur.execute("select * from runs where eventid=%s", (eventid,))
            for row in cur.fetchall():
                r = Run(**row)
                r.attrToUpper()
                match = cptrs[r.carid]
                match.runs[r.course-1].append(r)
                if indexafterpenalties:
                    r.net = (r.raw + (r.cones * g.event.conepen) + (r.gates * g.event.gatepen)) * match.indexval
                else:
                    r.net = (r.raw*match.indexval) + (r.cones * g.event.conepen) + (r.gates * g.event.gatepen)

            # For every entrant, calculate their bestraw, best net and event sum
            for e in cptrs.values():
                e.sum = 0
                for course in range(g.event.courses):
                    bestraw = sorted(e.runs[course], key=attrgetter('raw'))[0]
                    bestnet = sorted(e.runs[course], key=attrgetter('net'))[0]
                    bestraw.bestraw = True
                    bestnet.bestnet = True
                    e.sum += bestnet.net

            # Now for each class we can sort and update position, trophy, points(both types) and diffs
            for cls in results:
                res = results[cls]
                res.sort(key=attrgetter('sum'))
                trophydepth = ceil(len(res) / 3.0)
                for ii, e in enumerate(res):
                    e.position = ii+1
                    e.trophy = eventtrophy and (ii < trophydepth)
                    if ii == 0:
                        e.diff       = 0
                        e.diffpoints = 100.0
                        e.pospoints  = PPOINTS[0]
                    else:
                        e.diff       = res[ii-1].sum - e.sum
                        e.diffpoints = res[0].sum*100/e.sum;
                        e.pospoints  = ii >= len(PPOINTS) and PPOINTS[-1] or PPOINTS[ii]

                    # quick access for templates
                    e.points = usepospoints and e.pospoints or e.diffpoints

            # Get access for modifying series rows, check if we need to insert a default first.  Don't upsert as we have to specify LARGE json object twice.
            cur.execute("set role %s", (g.series,))
            name = "e%d"%eventid
            cur.execute("insert into results values (%s, %s, '{}', now()) ON CONFLICT (series, name) DO NOTHING", (g.series, name))
            cur.execute("update results set data=%s, modified=now() where series=%s and name=%s", (JEncoder().encode(results), g.series, name))
            cur.execute("reset role")
            g.db.commit()
                
            
"""
def UpdateClassResults(session, eventid, course, classcode, carid):
    cevals = [classcode, eventid]

    # Now we will loop from 1st to last, calculating points and inserting new event results 
    position = 1
    first = True
    basis = 1.0
    prev = 1.0
    basecnt = 1
    mysum = 0
    sumlist = []

    PPOINTS = [int(x) for x in session.query(Setting).get('pospointlist').val.split(',')]
    lists = []
    for r in session.sqlmap("GETCLASSRESULTS", cevals):
        thesum = float(r['sum'])
        insertcarid = int(r['carid'])

        sumlist.append(thesum)
        if insertcarid == carid:
            mysum = thesum

        cnt = int(r['coursecnt'])
        if (first):
            basis = thesum
            prev = thesum
            basecnt = cnt
            first = False

        rvals = [0]*10
        rvals[0] = eventid
        rvals[1] = insertcarid
        rvals[2] = classcode # classcode doesn't change
        rvals[3] = position
        rvals[4] = cnt
        rvals[5] = thesum

        if cnt == basecnt:
            rvals[6] = thesum-prev
            rvals[7] = basis/thesum*100
            rvals[8] = position >= len(PPOINTS) and PPOINTS[-1] or PPOINTS[position-1]
        else:
            #This person ran less courses than the other people
            rvals[6] = 999.999
            rvals[7] = 0.0
            rvals[8] = 0

        session.sqlmap("INSERTCLASSRESULTS", rvals)
        position += 1
        prev = thesum

    if course != 0 and carid != 0:
        UpdateAnnouncerDetails(session, eventid, course, carid, classcode, mysum, sumlist, PPOINTS)
"""
        


class Result(object):
    """ Contains driver name, car description, overall result and 2D array of runs by course and run # """

    def __init__(self, row, **kwargs): # kwargs = classdata, usepospoints
        for k in row.keys():
            setattr(self, k, getattr(row,k))

        if 'classdata' in kwargs:
            self.indexstr = kwargs['classdata'].getIndexStr(self)
            self.indexval = kwargs['classdata'].getEffectiveIndex(self)
        else:
            self.indexstr = self.indexcode
            self.indexval = 1.0

        if type(getattr(self, 'toptime', None)) is not float:
            self.toptime = 0.0

        if self.alias and not config['nwrsc.private']:
            self.firstname = self.alias
            self.lastname = ""

        if 'usepospoints' in kwargs:
            if kwargs['usepospoints']:
                self.points = self.pospoints
            else:
                self.points = self.diffpoints


auditList = """select r.*,d.firstname,d.lastname,d.alias,c.year,c.number,c.make,c.model,c.color,c.classcode,c.indexcode,c.tireindexed
                from runorder as r, cars as c, drivers as d  
                where r.carid=c.id and c.driverid=d.id and 
                r.eventid=:eventid and r.course=:course and r.rungroup=:group """

def getAuditResults(session, settings, event, course, rungroup):
    ret = list()
    reshold = dict()
    for row in session.execute(auditList, params={'eventid':event.id, 'course':course, 'group':rungroup}):
        r = Result(row)
        r.runs = [None] * event.runs
        ret.append(r)
        reshold[r.carid] = r

    for run in session.query(Run).filter_by(eventid=event.id).filter(Run.course==course).filter(Run.carid.in_(reshold.iterkeys())):
        r = reshold[run.carid]
        if run.run > event.runs:
            r.runs[:] =  r.runs + [None]*(run.run-event.runs)
        r.runs[run.run-1] = run

    return ret



classResult = """select r.*, c.year, c.make, c.model, c.color, c.number, c.indexcode, c.tireindexed, d.firstname, d.lastname, d.alias, d.membership, x.rungroup
                from eventresults as r, cars as c, drivers as d, runorder as x 
                where r.carid=c.id and c.driverid=d.id and r.eventid=%d and r.classcode in (%s) and x.eventid=r.eventid and x.carid=c.id and x.course=1
                order by r.position"""

def getClassResultsShort(session, settings, event, classdata, code):
    """
        Similar to getClassResults but but doesn't include any run information.
        For display of final points, nettime, only, this will be quicker and use less memory
    """
    ret = []
    for row in session.execute(classResult % (event.id, "'%s'" % code)):
        ret.append(Result(row, classdata=classdata, usepospoints=settings.usepospoints))

    trophydepth = ceil(len(ret) / 3.0)
    last = None
    eventtrophy = classdata.classlist[code].eventtrophy
    for ii, result in enumerate(ret):
        result.trophy = eventtrophy and (ii < trophydepth)
        if last is not None:
            result.diff = result.sum - last
        last = result.sum

    return ret



def getClassResults(session, settings, event, classdata, codes):
    """
        Get a dictionary of results that looks like:
        {classcode}[orderedlistof Results]
        The Result class will include all their runs as well as trophy indicators
    """
    ret = dict()
    reshold = dict()

    for code in codes:
        ret[code] = []

    ## Outside normal alchemy use, mass one time selects using IN clause, much faster 
    for row in session.execute(classResult % (event.id, ','.join(["'%s'" % x for x in codes]))):
        r = Result(row, classdata=classdata, usepospoints=settings.usepospoints)
        r.runs = [[None for i in range(event.runs)] for j in range(event.courses)]
        ret[r.classcode].append(r)
        reshold[r.carid] = r

    for run in session.query(Run).filter_by(eventid=event.id).filter(Run.carid.in_(reshold.iterkeys())):
        r = reshold[run.carid]
        if run.course > event.courses or run.run > event.runs:
            continue
        r.runs[run.course-1][run.run-1] = run
        for attr in ('raw', 'net', 'reaction', 'sixty', 'seg1', 'seg2', 'seg3', 'seg4', 'seg5'):
            if getattr(run, attr, None) is None:
                setattr(run, attr, 0.0)

    for code, clsresult in ret.iteritems():
        trophydepth = ceil(len(clsresult) / 3.0)
        for ii, result in enumerate(clsresult):
            result.trophy = (classdata.classlist[code].eventtrophy) and (ii < trophydepth)

    return ret


top1 = "select d.firstname as firstname, d.lastname as lastname, d.alias as alias, c.classcode as classcode, c.indexcode as indexcode, c.tireindexed as tireindexed, c.id as carid "
top2 = "from runs as r, cars as c, drivers as d where r.carid=c.id and c.driverid=d.id and r.eventid=:eventid "


topCourseRaw    = top1 + ", (r.raw+:conepen*r.cones+:gatepen*r.gates) as toptime " + top2 + " and r.course=:course and r.norder=1 order by toptime"
topCourseRawAll = top1 + ", (r.raw+:conepen*r.cones+:gatepen*r.gates) as toptime " + top2 + " and r.course=:course and r.bnorder=1 order by toptime"
topCourseNet    = top1 + ", r.net as toptime " + top2 + " and r.course=:course and r.norder=1 order by toptime"
topCourseNetAll = top1 + ", r.net as toptime " + top2 + " and r.course=:course and r.bnorder=1 order by toptime"
topRaw    = top1 + ", COUNT(r.raw) as courses, SUM(r.raw+:conepen*r.cones+:gatepen*r.gates) as toptime " + top2 + " and r.norder=1 group by c.id order by courses DESC, toptime"
topRawAll = top1 + ", COUNT(r.raw) as courses, SUM(r.raw+:conepen*r.cones+:gatepen*r.gates) as toptime " + top2 + " and r.bnorder=1 group by c.id order by courses DESC, toptime"
topNet    = top1 + ", COUNT(r.net) as courses, SUM(r.net) as toptime " + top2 + " and r.norder=1 group by c.id order by courses DESC, toptime"
topNetAll = top1 + ", COUNT(r.net) as courses, SUM(r.net) as toptime " + top2 + " and r.bnorder=1 group by c.id order by courses DESC, toptime"


class TopTimeEntry(object):
    def __init__(self, rowproxy=None):
        if rowproxy is not None:
            self.__dict__.update(zip(rowproxy.keys(), rowproxy.values()))
            if self.alias and not config['nwrsc.private']:
                self.name = self.alias
            else:
                self.name = self.firstname + " " + self.lastname

    def setIter(self, attributes):
        """ set the attributes that should be iterated over if someone tries to iterate us """
        self._attributes = attributes

    def __iter__(self):
        """ return a set of attributes as determined by setIter """
        for attr in self._attributes:
            yield getattr(self, attr)

    def copyWith(self, **kwargs):
        ret = TopTimeEntry()
        ret.__dict__ = self.__dict__.copy()
        ret.__dict__.update(kwargs)
        return ret
        
    def getFeed(self):
        d = dict()
        for k,v in self.__dict__.iteritems():
            if v is None or k in ['alias', 'firstname', 'lastname', '_attributes']:
                continue
            d[k] = v
        return d



class TopTimesList(object):
    def __init__(self, title, headers, attributes):
        self.title = title
        self.cols = headers
        self.attributes = attributes
        self.rows = list()

    def add(self, entry):
        entry.setIter(self.attributes)
        self.rows.append(entry)

    def getFeed(self):
        d = dict()
        for k,v in self.__dict__.iteritems():
            if v is None or k in ['_sa_instance_state']:
                continue
            d[k] = v
        return d



class TopTimesStorage(object):

    def __init__(self, session, event, classdata):
        self.session = session
        self.event = event
        self.classdata = classdata
    
        if self.event.courses > 1:
            coursecnt = self.event.courses + 1
        else:
            coursecnt = 1  # all courses == course 1

        # first tuple is allruns vs counted runs
        # next tuple is raw times vs net times
        # last step is a list with 0=all_courses, 1=course1, ...
        self.store = (([None]*coursecnt, [None]*coursecnt), ([None]*coursecnt, [None]*coursecnt))  

        self.segs = [[None]*self.event.getSegmentCount()]*self.event.courses


    def getList(self, allruns=False, raw=False, course=0, settitle=None):
        if self.store[allruns][raw][course] is None:
            if course == 0:
                if raw:
                    ttl = loadTopRawTimes(self.session, self.event, self.classdata, allruns)
                else:
                    ttl = loadTopNetTimes(self.session, self.event, self.classdata, allruns)
            else:
                if raw:
                    ttl = loadTopCourseRawTimes(self.session, self.event, self.course, self.classdata, allruns)
                else:
                    ttl = loadTopCourseNetTimes(self.session, self.event, self.course, self.classdata, allruns)
    
            self.store[allruns][raw][course] = ttl

        if settitle is not None:
            self.store[allruns][raw][course].title = settitle
        return self.store[allruns][raw][course]


    def getSegmentList(self, course, seg):
        if self.segs[course][seg] is None:
            self.segs[course][seg] = loadTopSegRawTimes(self.session, self.event, course, seg)
        return self.segs[course][seg]
    


def loadTopSegRawTimes(session, event, course, seg):
    getcol = ", MIN(r.seg%d) as toptime " % (seg)
    topSegRaw = top1 + getcol + top2 + " and r.course=:course and r.seg%d > %d group by r.carid order by toptime " % (seg, event.getSegments()[seg-1])

    ttl = TopTimesList("Top Segment Times (Course %d)" % course, ['Name', 'Class', 'Time'], ['name', 'classcode', 'toptime'])
    for row in session.execute(topSegRaw, params={'eventid':event.id, 'course':course}):
        entry = TopTimeEntry(row)
        entry.toptime = t3(entry.toptime)
        ttl.add(entry)
    return ttl
            

def loadTopCourseRawTimes(session, event, course, classdata, allruns=False):
    if allruns:
        sql = topCourseRawAll
    else:
        sql = topCourseRaw

    ttl = TopTimesList("Top Times (Course %d)" % course, ['Name', 'Class', 'Time'], ['name', 'classcode', 'toptime'])
    for row in session.execute(sql, params={'eventid':event.id, 'course':course, 'conepen':event.conepen, 'gatepen':event.gatepen}):
        entry = TopTimeEntry(row)
        entry.toptime = t3(entry.toptime)
        ttl.add(entry)
    return ttl
        

def loadTopCourseNetTimes(session, event, course, classdata, allruns=False):
    if allruns:
        sql = topCourseNetAll
    else:
        sql = topCourseNet

    ttl = TopTimesList("Top Index Times (Course %d)" % course, ['Name', 'Index', '', 'Time'], ['name', 'indexstr', 'indexvalue', 'toptime'])
    for row in session.execute(sql, params={'eventid':event.id, 'course':course}):
        entry = TopTimeEntry(row)
        entry.indexstr = classdata.getIndexStr(entry)
        entry.indexvalue = t3(classdata.getEffectiveIndex(entry))
        entry.toptime = t3(entry.toptime)
        ttl.add(entry)
    return ttl


def loadTopRawTimes(session, event, classdata, allruns=False):
    if allruns:
        sql = topRawAll
        title = "Top Times (All)"
    else:
        sql = topRaw
        title = "Top Times (Counted)"

    ttl = TopTimesList(title, ['Name', 'Class', 'Time'], ['name', 'classcode', 'toptime'])
    for row in session.execute(sql, params={'eventid':event.id,'conepen':event.conepen,'gatepen':event.gatepen}):
        entry = TopTimeEntry(row)
        entry.toptime = t3(entry.toptime)
        ttl.add(entry)
    return ttl


def loadTopNetTimes(session, event, classdata, allruns=False):
    if allruns:
        sql = topNetAll
        title = "Top Index Times (All)"
    else:
        sql = topNet
        title = "Top Index Times (Counted)"

    ttl = TopTimesList(title, ['Name', 'Class', 'Index', '', 'Time'], ['name', 'classcode', 'indexstr', 'indexvalue', 'toptime'])
    for row in session.execute(sql, params={'eventid':event.id}):
        entry = TopTimeEntry(row)
        entry.indexstr = classdata.getIndexStr(row)
        entry.indexvalue = t3(classdata.getEffectiveIndex(row))
        entry.toptime = t3(row.toptime)
        ttl.add(entry)
    return ttl

