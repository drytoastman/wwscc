from collections import defaultdict, OrderedDict
from operator import attrgetter
from flask import g
from .base import AttrBase, Entrant

class Audit(object):

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

class Challenge(AttrBase):

    @classmethod
    def getAll(cls):
        with g.db.cursor() as cur:
            cur.execute("select * from challenges order by challengeid")
            return [cls(**x) for x in cur.fetchall()]


class Event(AttrBase):

    def feedFilter(self, key, value):
        if key in ('paypal', 'snail', 'cost'):
            return None
        return value
    def getCountedRuns(self): return 999

    @classmethod
    def get(cls, eventid):
        with g.db.cursor() as cur:
            cur.execute("select * from events where eventid=%s", (eventid,))
            return cls(**cur.fetchone())

    @classmethod
    def byDate(cls):
        with g.db.cursor() as cur:
            cur.execute("select * from events order by date")
            return [cls(**x) for x in cur.fetchall()]


class Registration(AttrBase):

    @classmethod
    def getForEvent(cls, eventid):
        with g.db.cursor() as cur:
            cur.execute("SELECT d.*,c.*,r.* FROM cars c JOIN drivers d ON c.driverid=d.driverid JOIN registered r ON r.carid=c.carid WHERE r.eventid=%s ORDER BY c.number", (eventid,))
            return [Entrant(**x) for x in cur.fetchall()]


"""
class RunOrder(AttrBase):
    pass

def getNextCarIdInOrder(session, event, carid):
    order = # runorder carids (in order) for this cars rungroup
    for ii, row in enumerate(order):
        if row == carid:
            return order[(ii+1)%len(order)]

def loadNextRunOrder(session, event, carid):
    order = # runorder carids (in order) for this cars rungroup
    for ii, row in enumerate(order):
        if row == carid:
            for jj in range(ii+1, ii+4):
                carid = order[jj%len(order)]
                entrant = # get driver,car info
                result =  # get diff, position, rungroup, row 
                ret.append(entrant, result)
            break
"""

class Run(AttrBase):

    def feedFilter(self, key, value):
        if key in ('carid', 'eventid', 'modified') or (isinstance(value, int) and value < 0):
            return None
        return value
       

class ClassList(list):
    def __init__(self):
        list.__init__(self)
        self.numbers = set()

    def add(self, e):
        if (e.number+100)%200 in self.numbers: return False
        self.append(e)
        self.numbers.add(e.number)
        return True

class GroupOrder(OrderedDict):

    def pad(self):
        """ If the class is a odd # of entries and next class is not single, add a space """
        codes = list(self.keys())
        for ii in range(len(codes)-1):
            if len(self[codes[ii]]) % 2 != 0 and len(self[codes[ii+1]]) > 1:
                self[codes[ii]].append(Entrant())

    def number(self):
        """ Create the grid numbers for each entry """
        ii = 0
        for code in self:
            for e in self[code]:
                ii += 1
                e.grid = ii

class RunGroups(defaultdict):

    def put(self, entrant):
        cc = entrant.classcode
        for num, go in self.items():
            if cc in go:
                if not go[cc].add(entrant):
                    self[num+100][cc].add(entrant)
                return
        raise Exception("Failed to find a rungroup for {}".format(cc))

    def sort(self, key):
        for go in self.values():
            for clist in go.values():
                clist.sort(key=attrgetter(key))

    @classmethod
    def getForEvent(cls, eventid):
        ret = RunGroups(GroupOrder)
        with g.db.cursor() as cur:
            cur.execute("select * from classorder where eventid=%s order by rungroup, gorder", (eventid,))
            for x in cur.fetchall():
                ret[x['rungroup']][x['classcode']] = ClassList()
                ret[x['rungroup']+100][x['classcode']] = ClassList()
        return ret

