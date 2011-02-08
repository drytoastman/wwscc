from event import Event
import logging

log = logging.getLogger(__name__)

class Points(object):

	def __init__(self):
		self.points = {}
		self.total = 0
		self.drop = []

	def get(self, eventid):
		return self.points.get(eventid, None)

	def set(self, eventid, points):
		self.points[eventid] = points

	def calc(self, bestof):
		self.total = 0
		for ii, points in enumerate(sorted(self.points.values(), reverse=True)):
			if ii < bestof:
				self.total += points  # Add to total points
			else:
				self.drop.append(points)  # Otherwise this is a drop event

	def __cmp__(self, other):
		return cmp(other.total, self.total)

class Entrant(object):

	def __init__(self, row):
		self.firstname = row.firstname
		self.lastname = row.lastname
		self.id = row.id
		self.carid = row.carid
		self.points = Points()
		self.ppoints = Points()
		self.events = -1

	def setPoints(self, eventid, points):
		self.points.set(eventid, points)

	def setPPoints(self, eventid, points):
		self.ppoints.set(eventid, points)

	def calc(self, best):
		self.events = len(self.points.points)
		self.points.calc(best)
		self.ppoints.calc(best)


champResult = """select r.points,r.ppoints,r.eventid,r.classcode,r.carid,d.id,d.firstname,d.lastname 
				from eventresults as r, cars as c, drivers as d, events as e
				where r.eventid=e.id and e.practice=0 and r.carid=c.id and c.driverid=d.id and r.classcode like :codeglob
				order by r.classcode,d.firstname COLLATE NOCASE,d.lastname COLLATE NOCASE,r.eventid"""

def getChampResults(session, settings, codeglob = '%'):

	try:
		bestof = settings.useevents
		minevent = settings.minevents
		results = session.execute(champResult, params={'codeglob':codeglob}).fetchall()
	except Exception, e:
		log.warning("Failed to load champ results: %s" % e)
		return {}

	store = {}
	for r in results:
		if r.classcode not in store:
			store[r.classcode] = {}
		if r.id not in store[r.classcode]:
			store[r.classcode][r.id] = Entrant(r)

		store[r.classcode][r.id].setPoints(r.eventid, r.points)
		store[r.classcode][r.id].setPPoints(r.eventid, r.ppoints)

	ret = {}
	for code, cls in store.iteritems():
		toadd = list()
		for entrant in cls.values():
			entrant.calc(bestof)
			if entrant.events >= minevent:
				toadd.append(entrant)
		ret[code] = toadd
	return ret

