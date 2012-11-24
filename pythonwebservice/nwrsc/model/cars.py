from sqlalchemy import Table, Column, ForeignKey, Index, UniqueConstraint
from sqlalchemy.orm import mapper, relation, session
from sqlalchemy.types import Integer, SmallInteger, String, Boolean, Float, DateTime

from meta import metadata
from driver import Driver

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
	def __init__(self, **kwargs):
		for k, v in kwargs.iteritems():
			if hasattr(self, k):
				setattr(self, k, v)

mapper(Car, t_cars, properties = { 'driver' : relation(Driver, backref='cars')})


