import logging
from flask import g
from .base import AttrBase, Entrant

log = logging.getLogger(__name__)

class Challenge(AttrBase):

    @classmethod
    def get(cls, challengeid):
        with g.db.cursor() as cur:
            cur.execute("select * from challenges where challengeid=%s", (challengeid,))
            return cls(**cur.fetchone())

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
                    "LEFT JOIN cars c1 ON x.car1id=c1.carid LEFT JOIN drivers d1 ON c1.driverid=d1.driverid " \
                    "LEFT JOIN cars c2 ON x.car2id=c2.carid LEFT JOIN drivers d2 ON c2.driverid=d2.driverid " \
                    "WHERE challengeid=%s "
    
            getruns = "select * from challengeruns where challengeid=%s "
    
            if round >= 0:
                cur.execute(getrounds+"and round=%s", (challengeid,round))
            else:
                cur.execute(getrounds, (challengeid,))
    
            for obj in [AttrBase(**x) for x in cur.fetchall()]:
                # We organize ChallengeRound in a topological structure so we do custom setting here
                rnd = ChallengeRound()
                rnd.challengeid  = obj.challengeid
                rnd.round        = obj.round
                rnd.e1.carid     = obj.car1id
                rnd.e1.dial      = obj.car1dial
                rnd.e1.newdial   = obj.car1dial
                rnd.e1.firstname = obj.e1fn or ""
                rnd.e1.lastname  = obj.e1ln or ""
                rnd.e1.classcode = obj.e1cc
                rnd.e1.indexcode = obj.e1ic
                rnd.e1.left      = None
                rnd.e1.right     = None
                rnd.e2.carid     = obj.car2id
                rnd.e2.dial      = obj.car2dial
                rnd.e2.newdial   = obj.car2dial
                rnd.e2.firstname = obj.e2fn or ""
                rnd.e2.lastname  = obj.e2ln or ""
                rnd.e2.classcode = obj.e2cc
                rnd.e2.indexcode = obj.e2ic
                rnd.e2.left      = None
                rnd.e2.right     = None
                rounds[rnd.round] = rnd
    
            if round >= 0:
                cur.execute(getruns + "and round=%s", (challengeid, round))
            else:
                cur.execute(getruns, (challengeid,))
    
            for run in [ChallengeRun(**x) for x in cur.fetchall()]:
                rnd = rounds[run.round]
                if   rnd.e1.carid == run.carid:
                    setattr(rnd.e1, run.course==1 and 'left' or 'right', run)
                elif rnd.e2.carid == run.carid:
                    setattr(rnd.e2, run.course==1 and 'left' or 'right', run)

            for rnd in rounds.values():
                (rnd.winner, rnd.detail) = rnd.compute()
    
        return rounds


class ChallengeRound(AttrBase):

    def __init__(self, *args, **kwargs):
        AttrBase.__init__(self, *args, **kwargs)
        self.e1 = Entrant()
        self.e2 = Entrant()

    def compute(self):
        """ Computes the results of this round, returns (winner enum, detail), also sets newdial if breakout """
        
        tl = self.e1.left
        tr = self.e1.right
        bl = self.e2.left
        br = self.e2.right

        # Missing an entrant or no run data
        if self.e1.carid == 0 or self.e2.carid == 0: return (0, "No matchup yet")
        if tl is None and tr is None: return (0, 'No runs taken')

        # Some runs taken but there was non-OK status creating a default win
        if tl and tl.status != "OK":  return (-1, self.e2.firstname+" wins by default")
        if br and br.status != "OK":  return ( 1, self.e1.firstname+" wins by default")
        if tr and tr.status != "OK":  return (-1, self.e2.firstname+" wins by default")
        if bl and bl.status != "OK":  return ( 1, self.e1.firstname+" wins by default")

        # Some runs so present a half way status
        if not tl or not tr: 
            if tl and br:
                hr = (tl.net - self.e1.dial) - (br.net - self.e2.dial)
            elif tr and bl:
                hr = (tr.net - self.e1.dial) - (bl.net - self.e2.dial)
            else:
                hr = 0

            if hr > 0:
                return (0, '%s leads by %0.3f' % (self.e2.firstname, hr))
            elif hr < 0:
                return (0, '%s leads by %0.3f' % (self.e1.firstname, hr))
            else:
                return (0, 'Tied')

        # We have all the data, calculate who won
        e1result = self.e1.left.net + self.e1.right.net - (2*self.e1.dial)
        e2result = self.e2.left.net + self.e2.right.net - (2*self.e2.dial)
        if e1result < 0: self.e1.newdial = self.e1.dial + (e1result/2 * 1.5)
        if e2result < 0: self.e2.newdial = self.e2.dial + (e2result/2 * 1.5)

        if e1result < e2result: return ( 1, "%s wins by %0.3f" % (self.e1.firstname, (e2result - e1result)))
        if e2result < e1result: return (-1, "%s wins by %0.3f" % (self.e2.firstname, (e1result - e2result)))


class ChallengeRun(AttrBase):

    @property
    def net(self):
        # FINISH ME, get the event cone/gate penalties someday (if they ever change)
        if self.status == "OK":
            return self.raw + (self.cones * 2) + (self.gates * 10)
        return 999.999


