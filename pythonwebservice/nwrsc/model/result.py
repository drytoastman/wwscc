import logging
from flask import g
from math import ceil
from collections import defaultdict
from operator import attrgetter

from .base import AttrBase, BaseEncoder, Entrant
from .classlist import ClassData
from .runs import Run
from .settings import Settings

log = logging.getLogger(__name__)

class EventResult(object):

    @classmethod
    def get(cls, event):
        with g.db.cursor() as cur:
            # check if we need to update the results (serieslog shows changes later than calculated results)
            cur.execute("select " +
                "(select max(time) from serieslog where tablen in ('classlist', 'indexlist', 'events', 'cars', 'runs')) >" +
                "(select modified from results where series=%s and name=%s)", (g.series, "e%d"%event.eventid))
            mod = cur.fetchone()[0]
            if mod is None or mod:  # > if no results or serieslog data, we get a None, recalc on that case either way
                print("update")
                cls.update(event)

            # everything should be the latest now, load and return 
            cur.execute("select data from results where series=%s and name=%s", (g.series, "e%d"%event.eventid))
            res = cur.fetchone()
            if res is not None:
                return res['data']
            else:
                return dict()


    @classmethod
    def update(cls, event):
        """
            Creating the cached event result data for the given event.
            The event result data is {<classcode>, [<Entrant>]}.
            Each Entrant is a json object of attributes and a list of Run objects.
            Each Run object is regular run data with attributes like bestraw, bestnet assigned.
        """
        log.info("produce new data for %s %d", g.series, event.eventid)

        results = defaultdict(list)
        cptrs = {}

        blankrun = Run(raw=999.999, net=9999.999)
        classdata = ClassData.get()
        settings = Settings.get()
        ppoints = list(map(int, settings.pospointlist.split(',')))

        with g.db.cursor() as cur:
            # Fetch all of the entrants (driver/car combo), place in class lists, save pointers for quicker access
            cur.execute("select d.firstname,d.lastname,d.membership,c.* from drivers as d join cars as c on c.driverid=d.driverid " +
                        "where c.carid in (select distinct carid from runs where eventid=%s)", (event.eventid,))
            for row in cur.fetchall():
                e = Entrant(**row)
                e.attrToUpper()
                e.indexstr = classdata.getIndexStr(e)
                e.indexval = classdata.getEffectiveIndex(e)
                e.runs = [[blankrun]*event.runs for x in range(event.courses)]
                results[e.classcode].append(e)
                cptrs[e.carid] = e
            
            # Fetch all of the runs, calc net and assign to the correct entrant
            cur.execute("select * from runs where eventid=%s", (event.eventid,))
            for row in cur.fetchall():
                r = Run(**row)
                r.attrToUpper()
                match = cptrs[r.carid]
                match.runs[r.course-1][r.run - 1] = r
                if r.status != "OK":
                    r.net = 999.999
                elif settings.indexafterpenalties:
                    r.net = (r.raw + (r.cones * event.conepen) + (r.gates * event.gatepen)) * match.indexval
                else:
                    r.net = (r.raw*match.indexval) + (r.cones * event.conepen) + (r.gates * event.gatepen)

            # For every entrant, calculate their bestraw, best net and event sum
            for e in cptrs.values():
                e.sum = 0
                for course in range(event.courses):
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
                eventtrophy = classdata.classlist[cls].eventtrophy
                for ii, e in enumerate(res):
                    e.position = ii+1
                    e.trophy = eventtrophy and (ii < trophydepth)
                    if ii == 0:
                        e.diff       = 0
                        e.diffpoints = 100.0
                        e.pospoints  = ppoints[0]
                    else:
                        e.diff       = res[ii-1].sum - e.sum
                        e.diffpoints = res[0].sum*100/e.sum;
                        e.pospoints  = ii >= len(ppoints) and ppoints[-1] or ppoints[ii]

                    # quick access for templates
                    e.points = settings.usepospoints and e.pospoints or e.diffpoints

            # Get access for modifying series rows, check if we need to insert a default first.  Don't upsert as we have to specify LARGE json object twice.
            cur.execute("set role %s", (g.series,))
            name = "e%d"%event.eventid
            cur.execute("insert into results values (%s, %s, '{}', now()) ON CONFLICT (series, name) DO NOTHING", (g.series, name))
            cur.execute("update results set data=%s, modified=now() where series=%s and name=%s", (BaseEncoder().encode(results), g.series, name))
            cur.execute("reset role")
            g.db.commit()

            
"""
UpdateAnnouncerDetails(session, eventid, course, carid, classcode, mysum, sumlist, ppoints)
"""
        

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

