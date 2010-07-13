from sqlalchemy import Table, Column, ForeignKey, Index, UniqueConstraint
from sqlalchemy.orm import mapper, relation
from sqlalchemy.types import Integer, SmallInteger, String

from meta import metadata
from data import Car

## Runorder table
t_runorder = Table('runorder', metadata,
    Column('id', Integer, primary_key=True),
	Column('eventid', Integer, ForeignKey('events.id')),
	Column('course', Integer),
	Column('rungroup', SmallInteger),
	Column('carid', Integer, ForeignKey('cars.id')),
	Column('row', SmallInteger),
	UniqueConstraint('eventid', 'course', 'carid', name='orderindex_2')
	)
Index('orderindex_1', t_runorder.c.eventid)

class RunOrder(object):
	pass

mapper(RunOrder, t_runorder, properties = {'car':relation(Car)})


## Getting particular runorder details for announcer
getCarRunOrder = """select carid from runorder where eventid=:eventid and course=:course and rungroup=( 
				select rungroup from runorder where eventid=:eventid and carid=:carid
				) order by row"""

getEntrant = """select d.firstname, d.lastname, c.*
				from drivers as d, cars as c
				where c.driverid=d.id and c.id=:carid """

getStatus = """select re.diff, re.position, ru.* 
				from eventresults as re, runs as ru 
				where re.carid=:carid and re.eventid=:eventid and 
				ru.carid=:carid and ru.eventid=:eventid and ru.norder=1"""

def loadNextRunOrder(session, event, carid):
	order = [x.carid for x in session.execute(getCarRunOrder, params={'eventid':event.id, 'carid':carid, 'course':1}).fetchall()]
	size = len(order)
	ret = []

	for ii, row in enumerate(order):
		if row == carid:
			for jj in range(ii+1, ii+4):
				carid = order[jj%size]
				entrant = session.execute(getEntrant, params={'carid':carid})
				result = session.execute(getStatus, params={'eventid':event.id, 'carid':carid})
				ret.append((entrant.fetchone(),result.fetchone()))
				entrant.close()
				result.close()
			break

	return ret


## Rungroup table - mapping code to a rungroup
t_rungroups = Table('rungroups', metadata,
    Column('id', Integer, primary_key=True),
	Column('eventid', Integer, ForeignKey('events.id')),
	Column('classcode', String(16), ForeignKey('classlist.code')),
	Column('rungroup', SmallInteger),
	Column('gorder', SmallInteger),
	UniqueConstraint('eventid', 'classcode', 'rungroup', name='rungroupindex_1'))

class RunGroup(object):
	pass

mapper(RunGroup, t_rungroups)


