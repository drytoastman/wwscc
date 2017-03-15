
from flask import g

class RunOrder():
	pass

class RunGroup():

    @classmethod
    def getClassesForRunGroup(cls, eventid, rungroups):
        with g.db.cursor() as cur:
            cur.execute("select distinct(classcode) from cars where carid in " +
                        "(select distinct(carid) from runorder where eventid=%s and rungroup in %s) " +
                        "order by classcode", (eventid, tuple(rungroups)))
            return [x[0] for x in cur.fetchall()]


## Getting particular runorder details for announcer
getCarRunOrder = """select carid from runorder where eventid=:eventid and course=:course and rungroup=( 
				select rungroup from runorder where eventid=:eventid and carid=:carid
				) order by row"""

getEntrant = """select d.firstname, d.lastname, d.alias, c.*
				from drivers as d, cars as c
				where c.driverid=d.id and c.id=:carid """

getStatus = """select re.diff, re.position, ru.* 
				from eventresults as re, runs as ru 
				where re.carid=:carid and re.eventid=:eventid and 
				ru.carid=:carid and ru.eventid=:eventid and ru.norder=1"""


class ModifiableWrapper(object):
	""" Change RowProxy into something we reference the same way but can also modify """
	def __init__(self, items):
		self.__dict__ = dict(items)


def getNextCarIdInOrder(session, event, carid):
	order = [x.carid for x in session.execute(getCarRunOrder, params={'eventid':event.id, 'carid':carid, 'course':1}).fetchall()]
	for ii, row in enumerate(order):
		if row == carid:
			return order[(ii+1)%len(order)]


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
				ret.append((ModifiableWrapper(entrant.fetchone()),result.fetchone()))
				entrant.close()
				result.close()
			break

	for (entrant, result) in ret:
		if entrant.alias and not config['nwrsc.private']:
			entrant.firstname = entrant.alias
			entrant.lastname = ""
	return ret


