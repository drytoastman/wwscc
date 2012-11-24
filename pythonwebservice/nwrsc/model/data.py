from sqlalchemy import Table, Column, ForeignKey, Index, UniqueConstraint
from sqlalchemy.orm import mapper, relation, session
from sqlalchemy.types import Integer, SmallInteger, String, Boolean, Float, Binary, DateTime

from meta import metadata
from driver import Driver
from classlist import Class
from cars import Car
import datetime

## Formats table
t_data = Table('data', metadata,
	Column('name', String(32), primary_key=True),
	Column('mime', String(16)),
	Column('mod', DateTime),
	Column('data', Binary),
	)
	
class Data(object):
	@classmethod
	def set(cls, sess, name, data, mime=None):
		obj = sess.query(Data).get(name)
		if obj is None:
			obj = Data()
			obj.name = name
			sess.add(obj)
		obj.data = data
		obj.mod = datetime.datetime.utcnow()
		if mime is not None:
			obj.mime = mime

mapper(Data, t_data)

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
	Column('diffpoints', Float),
	Column('pospoints', SmallInteger),
	Column('updated', DateTime),
	UniqueConstraint('eventid', 'carid', name='eridx_2')
	)
Index('eridx_1', t_eventresults.c.eventid)

class EventResult(object):
	def __init__(self, **kwargs):
		for k, v in kwargs.iteritems():
			if hasattr(self, k):
				setattr(self, k, v)

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
