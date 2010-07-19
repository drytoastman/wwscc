from sqlalchemy import Table, Column, ForeignKey, Index, UniqueConstraint
from sqlalchemy.orm import mapper, relation
from sqlalchemy.types import Integer, SmallInteger, String, Boolean, Float, Binary, DateTime

from meta import metadata
from classlist import Class
import datetime
#from event import Event

## Settings table
t_settings = Table('settings', metadata,
	Column('name', String(32), primary_key=True),
	Column('val', String(128)),
	)

class Setting(object):
	@classmethod
	def loadDict(cls, session):
		d = dict()
		for s in session.query(Setting):
			d[s.name] = s.val
		return d

	@classmethod
	def saveDict(cls, session, d):
		for k, v in d.iteritems():
			K = str(k)
			V = str(v)
			s = session.query(Setting).get(K)
			if s is None:
				s = Setting()
				s.name = K
				session.add(s)
			s.val = V
			
mapper(Setting, t_settings)


## Formats table
t_data = Table('data', metadata,
	Column('name', String(32), primary_key=True),
	Column('mime', String(16)),
	Column('mod', DateTime),
	Column('data', Binary),
	)
	
class Data(object):
	@classmethod
	def set(cls, session, name, data, mime=None):
		obj = session.query(Data).get(name)
		if obj is None:
			obj = Data()
			obj.name = name
			session.add(obj)
		obj.data = data
		obj.mod = datetime.datetime.utcnow()
		if mime is not None:
			obj.mime = mime

mapper(Data, t_data)


## Drivers table
t_drivers = Table('drivers', metadata,
	Column('id', Integer, primary_key=True, autoincrement=True),
	Column('firstname', String(32)),
	Column('lastname', String(32)),
	Column('email', String(64)),
	Column('address', String(64)),
	Column('city', String(32)),
	Column('state', String(12)),
	Column('zip', String(12)),
	Column('homephone', String(16)),
	Column('workphone', String(16)),
	Column('clubs', String(128)),
	Column('brag', String(128)),
	Column('sponsor', String(64)),
	Column('membership', String(32)),
	)
Index('driveridx_1', t_drivers.c.firstname)
Index('driveridx_2', t_drivers.c.lastname)

class Driver(object):
	def getFeed(self):
		d = dict()
		d['firstname'] = self.firstname
		d['lastname'] = self.lastname
		return d

mapper(Driver, t_drivers)


## Cars Table
t_cars = Table('cars', metadata,
	Column('id', Integer, primary_key=True, autoincrement=True),
	Column('year', String(16)),
	Column('make', String(64)),
	Column('model', String(64)),
	Column('color', String(64)),
	Column('number', Integer),
	Column('driverid', Integer, ForeignKey('drivers.id')),
	Column('classcode', String(16)),
	Column('indexcode', String(16)),
	)
Index('caridx_1', t_cars.c.driverid)

class Car(object):
	pass

mapper(Car, t_cars, properties = { 'driver' : relation(Driver, backref='cars')})


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

## Event Results table
t_eventresults = Table('eventresults', metadata,
	Column('id', Integer, primary_key=True),
	Column('eventid', Integer, ForeignKey('events.id')),
	Column('carid', Integer, ForeignKey('cars.id')),
	Column('classcode', String(16), ForeignKey('classlist.code')),
	Column('position', SmallInteger),
	Column('courses', SmallInteger),
	Column('sum', Float),
	Column('diff', Float),
	Column('points', Float),
	Column('ppoints', SmallInteger),
	Column('updated', DateTime),
	UniqueConstraint('eventid', 'carid', name='eridx_2')
	)
Index('eridx_1', t_eventresults.c.eventid)

class EventResult(object):
	pass

mapper(EventResult, t_eventresults, properties = {'car':relation(Car, backref='results'), 'class':relation(Class)})


## Previous table
t_prevlist = Table('prevlist', metadata,
	Column('id', Integer, primary_key=True),
	Column('firstname', String(32)),
	Column('lastname', String(32))
	)

class PrevEntry(object):
	def __init__(self, first, last):
		self.firstname = first
		self.lastname = last

mapper(PrevEntry, t_prevlist)


## Changes Table
t_changes = Table('changes', metadata,
	Column('id', Integer, primary_key=True, autoincrement=True),
	Column('type', String(32)),
	Column('args', Binary)
	)

class Change(object):
	pass

mapper(Change, t_changes)
