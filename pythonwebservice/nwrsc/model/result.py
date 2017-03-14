import logging
from flask import g
from math import ceil
from copy import copy
from collections import defaultdict
from operator import attrgetter

from .base import AttrBase, BaseEncoder, Entrant
from .classlist import ClassData
from .runs import Run
from .settings import Settings

log = logging.getLogger(__name__)

def marklist(lst, label):
    """ Creates an attribute for each entry in the list with the value of index+1 """
    for ii, entry in enumerate(lst):
        setattr(entry, label, ii+1)

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
                cls.update(event)

            # everything should be the latest now, load and return 
            cur.execute("select data from results where series=%s and name=%s", (g.series, "e%d"%event.eventid))
            res = cur.fetchone()
            if res is not None:
                return res['data']
            else:
                return dict()

    @classmethod
    def audit(cls, event, course, group):
        with g.db.cursor() as cur:
            cur.execute("SELECT d.firstname,d.lastname,c.*,r.* FROM runorder r " \
                        "JOIN cars c ON r.carid=c.carid JOIN drivers d ON c.driverid=d.driverid " \
                        "WHERE r.eventid=%s and r.course=%s and r.rungroup=%s order by r.row", (event.eventid, course, group))
            hold = dict()
            for res in [Entrant(**x) for x in cur.fetchall()]:
                res.runs = [None] * event.runs
                hold[res.carid] = res

            cur.execute("SELECT * FROM runs WHERE eventid=%s and course=%s and carid in %s", (event.eventid, course, tuple(hold.keys())))
            for run in [Run(**x) for x in cur.fetchall()]:
                res = hold[run.carid]
                if run.run > event.runs:
                    res.runs[:] =  res.runs + [None]*(run.run - event.runs)
                res.runs[run.run-1] = run

            return list(hold.values())


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

        blankrun = Run(raw=999.999, net=999.999, status="DNS")
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
                e.runs = [[copy(blankrun) for x in range(event.runs)] for x in range(event.courses)]
                results[e.classcode].append(e)
                cptrs[e.carid] = e
            
            # Fetch all of the runs, calc net and assign to the correct entrant
            cur.execute("select * from runs where eventid=%s", (event.eventid,))
            for row in cur.fetchall():
                r = Run(**row)
                r.attrToUpper()
                match = cptrs[r.carid]
                match.runs[r.course-1][r.run - 1] = r
                penalty = (r.cones * event.conepen) + (r.gates * event.gatepen)
                if r.status != "OK":
                    r.pen = 999.999
                    r.net = 999.999
                elif settings.indexafterpenalties:
                    r.pen = r.raw + penalty
                    r.net = r.pen * match.indexval
                else:
                    r.pen = r.raw + penalty
                    r.net = (r.raw*match.indexval) + penalty

            # For every entrant, calculate their best runs (raw,net,allraw,allnet) and event sum(net)
            for e in cptrs.values():
                e.net = 0
                e.pen = 0
                counted = min(classdata.getCountedRuns(e.classcode), event.getCountedRuns())

                for course in range(event.courses):
                    marklist (sorted(e.runs[course], key=attrgetter('raw')), 'allraworder')
                    marklist (sorted(e.runs[course], key=attrgetter('net')), 'allnetorder')
                    marklist (sorted(e.runs[course][0:counted], key=attrgetter('raw')), 'raworder')
                    bestnet = sorted(e.runs[course][0:counted], key=attrgetter('net'))
                    marklist(bestnet, 'netorder')
                    e.net += bestnet[0].net
                    e.pen += bestnet[0].pen

            # Now for each class we can sort and update position, trophy, points(both types) and diffs
            for cls in results:
                res = results[cls]
                res.sort(key=attrgetter('net'))
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
                        e.diff       = res[ii-1].net - e.net
                        e.diffpoints = res[0].net*100/e.net;
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

            

class TopTimesAccessor(object):

    def __init__(self, event, results):
        self.event = event
        self.classdata = ClassData.get()
        self.results = results

    def getLists(self, *keys):
        """
            Generate lists on demand as there are many iterations
                net      = True for indexed times, False for penalized but raw times
                counted  = True for to only included 'counted' runs and non-second run classes
                course   = 0 for combined course total, >0 for specific course
               Extra fields that have standard defaults we stick with:
                settitle = A string to override the list title with
                col      = A list of column names for the table
                fields   = The fields to match to each column
        """
        lists = list()
        for key in keys:
            net     = key.get('net', True)
            counted = key.get('counted', True)
            course  = key.get('course', 0)
            title   = key.get('settitle', None)
            cols    = key.get('cols', None)
            fields  = key.get('fields', None)

            if title is None:
                title  = "Top {} Times ({} Runs)".format(net and "Net" or "Raw", counted and "Counted" or "All")
                if course > 0: title += " Course {}".format(course)

            if cols is None:   cols   = ['Name', 'Class',     'Index',    '',         'Time']
            if fields is None: fields = ['name', 'classcode', 'indexstr', 'indexval', 'time']

            ttl = TopTimesList(title, cols, fields)
            for cls in self.results:
                for e in self.results[cls]:
                    if course > 0:
                        for r in e['runs'][course-1]:
                            if r['netorder'] == 1:
                                time = net and r['net'] or r['pen']
                    else:
                        time = net and e['net'] or e['pen']

                    ttl.append(TopTimeEntry(fields,
                        name="{} {}".format(e['firstname'], e['lastname']),
                        classcode = e['classcode'],
                        indexstr  =  e['indexstr'],
                        indexval  =  e['indexval'],
                        time      =  time
                    ))

            # Sort and set 'pos' attribute, then add to the mass table
            ttl.sort(key=attrgetter('time'))
            lists.append(ttl)

        return TopTimesTable(*lists)


class TopTimesList(list):
    """ A list of top times along with the title, column and field info """
    def __init__(self, title, cols, fields):
        self.title = title
        self.cols = cols
        self.fields = fields
        

class TopTimeEntry(object):
    """ A row entry in the TopTimesList """
    def __init__(self, fields, **kwargs):
        self._fields = fields
        self.__dict__.update(kwargs)

    def __iter__(self):
        """ return a set of attributes as determined by original fields """
        for f in self._fields:
            yield getattr(self, f, "missing")

    def __repr__(self):
        return "{}, {}".format(self._fields, self.__dict__)


class TopTimesRow(list):
    pass

class TopTimesTable(object):
    """ We need to zip our lists together ourselves so we create our Table and Rows here """

    def __init__(self, *lists):
        self.titles   = list()
        self.colcount = list()
        self.cols     = list()
        self.fields   = list()
        self.rows     = list()

        for ttl in lists:
            self.addList(ttl)

    def addList(self, ttl):
        if len(ttl.cols) != len(ttl.fields):
            raise Exception('Top times columns and field arrays are not equals in size ({}, {})'.format(len(ttl.cols), len(ttl.fields)))

        self.titles.append(ttl.title)
        self.colcount.append(len(ttl.cols))
        self.cols.append(ttl.cols)
        self.fields.append(ttl.fields)

        if len(self.rows) < len(ttl):
            self.rows.extend([TopTimesRow() for x in range(len(ttl) - len(self.rows))])

        for ii in range(len(ttl)):
            self.rows[ii].append(ttl[ii])


