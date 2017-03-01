import logging
from flask import g
from .base import AttrBase

log = logging.getLogger(__name__)

class Challenge(AttrBase):

    @classmethod
    def getForEvent(cls, eventid):
        with g.db.cursor() as cur:
            cur.execute("select * from challenges where eventid=%s", (eventid,))
            return [cls(**x) for x in cur.fetchall()]
        

class ChallengeRound(AttrBase):

    def getHalfResult(self):
        tl = getattr(self, 'car1leftrun',  None)
        tr = getattr(self, 'car1rightrun', None)
        bl = getattr(self, 'car2leftrun',  None)
        br = getattr(self, 'car2rightrun', None)
        
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
    pass


def loadSingleRoundResults(session, challenge, rnd):
    rnd.driver1 = rnd.car1 and rnd.car1.driver or None
    rnd.car1leftrun = None
    rnd.car1rightrun = None
    rnd.driver2 = rnd.car2 and rnd.car2.driver or None
    rnd.car2leftrun = None
    rnd.car2rightrun = None 

    for dr in (rnd.driver1, rnd.driver2):
        if dr is not None and dr.alias and not config['nwrsc.private']:
            dr.firstname = dr.alias
            dr.lastname = ""

    for r in session.query(Run).filter(Run.eventid.op(">>")(16)==challenge.id).filter(Run.eventid.op("&")(255)==rnd.round):
        if rnd.car1id == r.carid:
            if r.course == 1:
                rnd.car1leftrun = r
            elif r.course == 2:
                rnd.car1rightrun = r
        elif rnd.car2id == r.carid:
            if r.course == 1:
                rnd.car2leftrun = r
            elif r.course == 2:
                rnd.car2rightrun = r
        else:
            log.warning("Can't match run to round car")


def loadChallengeResults(session, challengeid, rounds):

    carids = set()
    for rnd in rounds.itervalues():
        carids.add(rnd.car1id)
        carids.add(rnd.car2id)
        rnd.driver1 = None
        rnd.car1leftrun = None
        rnd.car1rightrun = None
        rnd.driver2 = None
        rnd.car2leftrun = None
        rnd.car2rightrun = None

    for r in session.query(Run).filter(Run.eventid.op(">>")(16)==challengeid):
        rnd = rounds.get(r.eventid & 0x0FF, None)
        if rnd is None:
            log.warning("Can't match run to round")
            continue

        if rnd.car1id == r.carid:
            if r.course == 1:
                rnd.car1leftrun = r
            elif r.course == 2:
                rnd.car1rightrun = r
        elif rnd.car2id == r.carid:
            if r.course == 1:
                rnd.car2leftrun = r
            elif r.course == 2:
                rnd.car2rightrun = r
        else:
            log.warning("Can't match run to round car")
        
    cars = dict()
    for cd in session.query(Driver,Car).join('cars').filter(Car.id.in_(carids)):
        cars[cd.Car.id] = cd
        if cd[0].alias and not config['nwrsc.private']:
            cd[0].firstname = cd[0].alias
            cd[0].lastname = ""

    for rnd in rounds.itervalues():
        if rnd.car1id > 0 and rnd.car1id in cars:
            rnd.driver1 = cars[rnd.car1id].Driver
        if rnd.car2id > 0 and rnd.car2id in cars:
            rnd.driver2 = cars[rnd.car2id].Driver

