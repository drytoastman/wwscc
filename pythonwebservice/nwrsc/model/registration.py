from sqlalchemy import Table, Column, ForeignKey, UniqueConstraint
from sqlalchemy.orm import mapper, relation
from sqlalchemy.types import Integer

from meta import metadata
from runs import Run

## Registered table
t_registered = Table('registered', metadata,
	Column('id', Integer, primary_key=True),
	Column('eventid', Integer, ForeignKey('events.id')),
	Column('carid', Integer, ForeignKey('cars.id')),
	UniqueConstraint('eventid', 'carid', name='regindex_1')
	)

class Registration(object):
	def __init__(self, eventid, carid):
		self.eventid = eventid
		self.carid = carid

	@classmethod
	def updateFromRuns(cls, session):
		# Remove all registerations for events that already have runs associated
		session.execute("delete from registered where eventid in (select distinct eventid from runs)")
		# Reinsert registrations for those cars that actually ran
		session.execute("insert into registered (carid, eventid) select distinct carid, eventid from runs")
		session.commit()


from cars import Car
from event import Event
mapper(Registration, t_registered, properties = {'car':relation(Car, backref='registration'), 'event':relation(Event)})


