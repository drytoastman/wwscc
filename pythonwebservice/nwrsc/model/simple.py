from datetime import datetime
import uuid
import json

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
        return cls.getall("select * from challenges order by challengeid")



class Event(AttrBase):

    def feedFilter(self, key, value):
        if key in ('payments', 'snail'):
            return None
        return value

    def getCountedRuns(self):
        ret = getattr(self, 'counted', 0)
        if ret <= 0:
            return 999
        return ret

    def hasOpened(self): return datetime.now() > self.regopened
    def hasClosed(self): return datetime.now() > self.regclosed
    def isOpen(self):    return self.hasOpened() and not self.hasClosed()
    def getCount(self):  return self.getval("SELECT count(carid) FROM registered WHERE eventid=%s", (self.eventid,))
    def getDriverCount(self): return self.getval("SELECT count(distinct(c.driverid)) FROM registered r JOIN cars c ON r.carid=c.carid WHERE r.eventid=%s", (self.eventid,))

    @classmethod
    def get(cls, eventid):
        return cls.getunique("select * from events where eventid=%s", (eventid,))

    @classmethod
    def byDate(cls):
        return cls.getall("select * from events order by date")



class Registration(AttrBase):

    @classmethod
    def getForEvent(cls, eventid):
        with g.db.cursor() as cur:
            cur.execute("SELECT d.*,c.*,r.* FROM cars c JOIN drivers d ON c.driverid=d.driverid JOIN registered r ON r.carid=c.carid WHERE r.eventid=%s ORDER BY c.number", (eventid,))
            return [Entrant(**x) for x in cur.fetchall()]

    @classmethod
    def getForDriver(cls, driverid):
        return cls.getall("SELECT r.* FROM registered r JOIN cars c on r.carid=c.carid WHERE c.driverid=%s", (driverid,))

    @classmethod
    def getForSeries(cls, series, driverid):
        return cls.getall("SELECT c.*,e.eventid,e.date,e.name FROM {}.registered r JOIN {}.cars c ON r.carid=c.carid JOIN {}.events e ON e.eventid=r.eventid WHERE c.driverid=%s ORDER BY e.date".format(series,series,series), (driverid,))

    @classmethod
    def update(cls, eventid, carids, verifyid):
        with g.db.cursor() as cur:
            cur.execute("DELETE from registered where eventid=%s and carid in (select carid from cars where driverid=%s)", (eventid, verifyid))
            for cid in carids:
                # FINISH ME, what is a one-liner to insert only if car with carid has driverid=verifyid
                cur.execute("INSERT INTO registered (eventid, carid) VALUES (%s, %s)", (eventid, cid))
            g.db.commit()


class Run(AttrBase):

    def feedFilter(self, key, value):
        if key in ('carid', 'eventid'):
            return None
        return value

    @classmethod
    def getLast(self, eventid, modified=0, classcodes=[]):
        base = "SELECT {} c.classcode,MAX(r.modified) as modified, r.carid FROM runs r JOIN cars c ON r.carid=c.carid " \
                "WHERE {} r.eventid=%s and r.modified > to_timestamp(%s) GROUP BY r.carid, c.classcode ORDER BY {} "
        if len(classcodes) > 0:
            sql = base.format("DISTINCT ON (c.classcode) ", "c.classcode IN %s AND ", "c.classcode,modified DESC")
            val = (tuple(classcodes), g.eventid, modified)
        else:
            sql = base.format("", "", "modified DESC LIMIT 1")
            val = (g.eventid, modified)
        with g.db.cursor() as cur:
            cur.execute(sql, val)
            return [Run(**x) for x in cur.fetchall()]


class RunOrder(AttrBase):

    @classmethod
    def getNextCarIdInOrder(cls, carid, eventid, course=1):
        """ returns the carid of the next car in order after the given carid """
        with g.db.cursor() as cur:
            cur.execute("SELECT carid FROM runorder WHERE eventid=%s AND course=%s AND rungroup=" +
                        "(SELECT rungroup FROM runorder WHERE carid=%s AND eventid=%s AND course=%s LIMIT 1) " +
                        "ORDER BY row", (eventid, course, carid, eventid, course))
            order = [x[0] for x in cur.fetchall()]
            for ii, rid in enumerate(order):
                if rid == carid:
                    return order[(ii+1)%len(order)]

    @classmethod
    def getNextRunOrder(cls, carid, eventid, course=1):
        """ Returns a list of objects (classcode, carid, row) for the next cars in order after carid """
        with g.db.cursor() as cur:
            cur.execute("SELECT c.classcode,r.carid,r.row FROM runorder r JOIN cars c on r.carid=c.carid " +
                        "WHERE eventid=%s AND course=%s AND rungroup=" +
                        "(SELECT rungroup FROM runorder WHERE carid=%s AND eventid=%s AND course=%s LIMIT 1) " +
                        "ORDER BY r.row", (eventid, course, carid, eventid, course))
            order = [RunOrder(**x) for x in cur.fetchall()]
            ret = []
            for ii, row in enumerate(order):
                if row.carid == carid:
                    for jj in range(ii+1, ii+4):
                        ret.append(order[jj%len(order)])
                    break
            return ret

