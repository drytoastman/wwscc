from sqlalchemy import Table, Column, ForeignKey, UniqueConstraint
from sqlalchemy.orm import mapper, relation
from sqlalchemy.types import Integer, String, Date

from meta import metadata
from event import Event
from data import Driver

## Payments table
t_payments = Table('payments', metadata,
	Column('txid', String(32), primary_key=True),
	Column('date', Date),
	Column('type', String(10)),
	Column('status', String(10)),
	Column('driverid', Integer, ForeignKey('drivers.id')),
	Column('eventid', Integer, ForeignKey('events.id')),
	Column('amount', Integer),
	UniqueConstraint('txid', name='paymentindex')
	)

class Payment(object):
	pass

mapper(Payment, t_payments, properties={'driver':relation(Driver), 'event':relation(Event)})

