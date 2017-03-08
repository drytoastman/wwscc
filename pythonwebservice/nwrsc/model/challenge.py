import logging
from flask import g
from .base import AttrBase, Entrant

log = logging.getLogger(__name__)

class Challenge(AttrBase):

    @classmethod
    def getForEvent(cls, eventid):
        with g.db.cursor() as cur:
            cur.execute("select * from challenges where eventid=%s", (eventid,))
            return [cls(**x) for x in cur.fetchall()]

    @classmethod
    def getResults(cls, challengeid, round=-1):
        rounds = dict()
        with g.db.cursor() as cur:
            getrounds = "SELECT x.*, " \
                    "d1.firstname as e1fn, d1.lastname as e1ln, c1.classcode as e1cc, c1.indexcode as e1ic, " \
                    "d2.firstname as e2fn, d2.lastname as e2ln, c2.classcode as e2cc, c2.indexcode as e2ic  " \
                    "FROM challengerounds x " \
                    "JOIN cars    c1 ON x.car1id=c1.carid       JOIN cars    c2 ON x.car2id=c2.carid " \
                    "JOIN drivers d1 ON c1.driverid=d1.driverid JOIN drivers d2 ON c2.driverid=d2.driverid " \
                    "WHERE challengeid=%s "
    
            getruns = "select * from challengeruns where challengeid=%s "
    
            if round >= 0:
                cur.execute(getrounds+"and round=%s", (challengeid,round))
            else:
                cur.execute(getrounds, (challengeid,))
    
            for rnd in [ChallengeRound(**x) for x in cur.fetchall()]:
                rnd.e1.firstname = rnd.e1fn
                rnd.e1.lastname  = rnd.e1ln
                rnd.e1.classcode = rnd.e1cc
                rnd.e1.indexcode = rnd.e1ic
                rnd.e2.firstname = rnd.e2fn
                rnd.e2.lastname  = rnd.e2ln
                rnd.e2.classcode = rnd.e2cc
                rnd.e2.indexcode = rnd.e2ic
                rounds[rnd.round] = rnd
    
            if round >= 0:
                cur.execute(getruns + "and round=%s", (challengeid, round))
            else:
                cur.execute(getruns, (challengeid,))
    
            for run in [ChallengeRun(**x) for x in cur.fetchall()]:
                rnd = rounds[run.round]
                if   rnd.car1id == run.carid: rnd.e1runs[run.course-1] = run
                elif rnd.car2id == run.carid: rnd.e2runs[run.course-1] = run
    
        return rounds


class ChallengeRound(AttrBase):

    LEFT = 0
    RIGHT = 1

    def __init__(self, *args, **kwargs):
        AttrBase.__init__(self, *args, **kwargs)
        self.e1 = Entrant()
        self.e1runs = [None, None]
        self.e2 = Entrant()
        self.e2runs = [None, None]

    def getHalfResult(self):
        tl = self.e1runs[0]
        tr = self.e1runs[1]
        bl = self.e2runs[0]
        br = self.e2runs[1]
        
        if tl and br:
            tdiff = tl.net - self.car1dial
            bdiff = br.net - self.car2dial
        elif tr and bl:
            tdiff = tr.net - self.car1dial
            bdiff = bl.net - self.car2dial
        else:
            tdiff = 0.0
            bdiff = 0.0

        return tdiff - bdiff


class ChallengeRun(AttrBase):

    @property
    def net(self):
        # FINISH ME, get the event cone/gate penalties someday (if they ever change)
        if self.status == "OK":
            return self.raw + (self.cones * 2) + (self.gates * 10)
        return 999.999


