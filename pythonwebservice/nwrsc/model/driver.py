from sqlalchemy import Table, Column, ForeignKey, Index, UniqueConstraint
from sqlalchemy.orm import mapper, relation, session
from sqlalchemy.types import Integer, SmallInteger, String, Boolean, Float, DateTime

from meta import metadata

## List of extra fields user wants
t_driverfields = Table('driverfields', metadata,
	Column('id', Integer, primary_key=True, autoincrement=True),
	Column('name', String(32)),
	Column('type', String(32)),
	Column('title', String(32))
	)

class DriverField(object):
	def __init__(self, **kwargs):
		for k, v in kwargs.iteritems():
			if hasattr(self, k):
				setattr(self, k, v)

mapper(DriverField, t_driverfields)


## Drivers extra table
# common extras: clubs, membership number
t_driverextra = Table('driverextra', metadata,
	Column('id', Integer, primary_key=True, autoincrement=True),
	Column('driverid', Integer, ForeignKey('drivers.id')),
	Column('name', String(32)),
	Column('value', String(64))
	)

class DriverExtra(object):
	def __init__(self, **kwargs):
		for k, v in kwargs.iteritems():
			if hasattr(self, k):
				setattr(self, k, v)


mapper(DriverExtra, t_driverextra)


## Drivers table
t_drivers = Table('drivers', metadata,
	Column('id', Integer, primary_key=True, autoincrement=True),
	Column('firstname', String(32)),
	Column('lastname', String(32)),
	Column('alias', String(64)),
	Column('email', String(64)),
	Column('address', String(64)),
	Column('city', String(32)),
	Column('state', String(12)),
	Column('zip', String(12)),
	Column('phone', String(16)),
	Column('brag', String(128)),
	Column('sponsor', String(64)),
	Column('membership', String(16)),
	)
Index('driveridx_1', t_drivers.c.firstname)
Index('driveridx_2', t_drivers.c.lastname)

class Driver(object):
	def __init__(self, **kwargs):
		for k, v in kwargs.iteritems():
			if hasattr(self, k):
				setattr(self, k, v)

	def copy(self):
		ret = Driver()
		for col in t_drivers.c.keys():
			if col != 'id':
				setattr(ret, col, getattr(self, col))
		return ret
		
	def getFeed(self):
		d = dict()
		d['firstname'] = self.firstname
		d['lastname'] = self.lastname
		return d

	def getExtra(self, name):
		""" Get an 'extra' driver attribute if possible """
		for e in self._extras:
			if e.name == name:
				return e.value
		return ""

	def setExtra(self, name, value):
		for e in self._extras:
			if e.name == name:
				e.value = value
				return

		field = DriverExtra()
		field.driverid = self.id
		field.name = name
		field.value = value
		session.Session.object_session(self).add(field)
		self._extras.append(field)

	def delExtra(self, name):
		for e in self._extras:
			if e.name == name:
				session.Session.object_session(self).delete(e)
				return

	def clearExtra(self):
		self._extras[:] = []

		
mapper(Driver, t_drivers, properties = { '_extras' : relation(DriverExtra) } ) 


