from sqlalchemy import Table, Column, ForeignKey
from sqlalchemy.orm import mapper, relation
from sqlalchemy.types import Integer, String, Boolean, Float

from pylons import config
from meta import metadata
from driver import Driver
from cars import Car, t_cars
from runs import Run

import logging
log = logging.getLogger(__name__)

## Challenges table
t_challenges = Table('challenges', metadata,
	Column('id', Integer, primary_key=True, autoincrement=True),
	Column('eventid', Integer, ForeignKey('events.id')),
	Column('name', String(32)),
	Column('depth', Integer)
	)

class Challenge(object):
	def __init__(self, **kwargs):
		for k, v in kwargs.iteritems():
			if hasattr(self, k):
				setattr(self, k, v)

	def getFeed(self):
		ret = dict()
		ret['id'] = self.id
		ret['name'] = self.name
		ret['depth'] = self.depth
		return ret


mapper(Challenge, t_challenges) #, properties={'event':relation(Event)})


## Challenge Rounds table
t_challengerounds = Table('challengerounds', metadata,
	Column('id', Integer, primary_key=True, autoincrement=True),
	Column('challengeid', Integer, ForeignKey('challenges.id')),
	Column('round', Integer),
	Column('swappedstart', Boolean, default=False),
	Column('car1id', Integer, ForeignKey('cars.id')),
	Column('car1dial', Float),
	Column('car1result', Float),
	Column('car1newdial', Float),
	Column('car2id', Integer, ForeignKey('cars.id')),
	Column('car2dial', Float),
	Column('car2result', Float),
	Column('car2newdial', Float)
	)

class ChallengeRound(object):

	def getFeed(self):
		d = dict()
		for k,v in self.__dict__.iteritems():
			if v is None or k in ['_sa_instance_state']:
				continue
			if isinstance(v, float):
				if v != 0:
					d[k] = "%0.3f" % (v)
			else:
				d[k] = v
		return d



	def getHalfResult(self):
		tl = getattr(self, 'car1leftrun', None)
		tr = getattr(self, 'car1rightrun', None)
		bl = getattr(self, 'car2leftrun', None)
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

mapper(ChallengeRound, t_challengerounds, properties = {
			'challenge':relation(Challenge),
			'car1':relation(Car, primaryjoin=t_challengerounds.c.car1id==t_cars.c.id),
			'car2':relation(Car, primaryjoin=t_challengerounds.c.car2id==t_cars.c.id)})


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

