
from flask import g

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

