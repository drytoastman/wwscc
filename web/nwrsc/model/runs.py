from sqlalchemy import Table, Column, ForeignKey, Index, UniqueConstraint
from sqlalchemy.orm import mapper, relation, session
from sqlalchemy.types import Integer, SmallInteger, String, Boolean, Float, Binary, DateTime

from meta import metadata
from cars import Car

## Runs 
t_runs = Table('runs', metadata,
	Column('id', Integer, primary_key=True),
	Column('eventid', Integer, ForeignKey('events.id'), index=True),
	Column('carid', Integer, ForeignKey('cars.id'), index=True),
	Column('course', SmallInteger, default=1),
	Column('run', SmallInteger, default=1),
	Column('cones', SmallInteger, default=0),
	Column('gates', SmallInteger, default=0),
	Column('status', String(8), default='DNS'),
	Column('reaction', Float, default=0),
	Column('sixty', Float, default=0),
	Column('seg1', Float, default=0),
	Column('seg2', Float, default=0),
	Column('seg3', Float, default=0),
	Column('seg4', Float, default=0),
	Column('seg5', Float, default=0),
	Column('raw', Float, default=0),
	Column('net', Float, default=0),
	Column('rorder', SmallInteger, default=0),
	Column('norder', SmallInteger, default=0),
	Column('brorder', SmallInteger, default=0),
	Column('bnorder', SmallInteger, default=0),
	UniqueConstraint('eventid', 'carid', 'course', 'run', name='runidx_3')
	)
Index('runidx_1', t_runs.c.eventid)
Index('runidx_2', t_runs.c.carid)

class Run(object):
	def getChallengeId(self): return self.eventid >> 16
	def getChallengeRound(self): return eventid & 0x0FFF
	challengeid = property(getChallengeId)
	round = property(getChallengeRound)

	def getSegment(self, idx):
		return getattr(self, "seg%d" % (idx))

	def validSegments(self, segmentlist):
		segments = list()
		for ii, segmin in enumerate(segmentlist):
			segx = getattr(self, "seg%d" % (ii+1))
			if segx < segmin:
				return [None] * len(segmentlist)
			segments.append(segx)
		return segments

	def getFeed(self):
		d = dict()
		for k,v in self.__dict__.iteritems():
			if v is None or k in ['_sa_instance_state', 'id', 'carid', 'eventid']:
				continue
			if isinstance(v, float):
				if v != 0:
					d[k] = "%0.3f" % (v)
			elif isinstance(v, int):
				if v >= 0:
					d[k] = v
			else:
				d[k] = v
		return d


mapper(Run, t_runs, properties = {
#		'event':relation(Event),
		'car':relation(Car)}) 

