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
        if key in ('paypal', 'snail'):
            return None
        return value

    def getCountedRuns(self):
        ret = getattr(self, 'counted', 0)
        if ret <= 0:
            return 999
        return ret

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


class Run(AttrBase):

    def feedFilter(self, key, value):
        if key in ('carid', 'eventid'):
            return None
        return value

class LastRun(AttrBase):
    """ Separate from run as we have a different limited set of attributes from a regular run and include carid """
    @classmethod
    def getLast(self, eventid, lasttime, classcodes=[]):
        base = "SELECT {} c.classcode,MAX(r.modified) as modified, r.carid FROM runs r JOIN cars c ON r.carid=c.carid " \
                "WHERE {} r.eventid=%s and r.modified > %s GROUP BY r.carid, c.classcode ORDER BY {} "
        if len(classcodes) > 0:
            sql = base.format("DISTINCT ON (c.classcode) ", "c.classcode IN %s AND ", "c.classcode,modified DESC")
            val = (tuple(classcodes), g.eventid, lasttime)
        else:
            sql = base.format("", "", "modified DESC LIMIT 1")
            val = (g.eventid, lasttime)
        with g.db.cursor() as cur:
            cur.execute(sql, val)
            return [LastRun(**x) for x in cur.fetchall()]


