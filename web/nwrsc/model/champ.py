from event import Event
from pylons import config
from classlist import ClassData
from collections import defaultdict
import logging
import operator

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
		self.drop = []
		for ii, points in enumerate(sorted(self.points.values(), reverse=True)):
			if ii < bestof:
				self.total += points  # Add to total points
			else:
				self.drop.append(points)  # Otherwise this is a drop event

	def __cmp__(self, other):
		""" provides the comparison for sorting """
		return cmp(other.total, self.total)


class Entrant(object):

	AvailableSubKeys = ['firsts', 'seconds', 'thirds', 'fourths', 'attended']

	def __init__(self, row):
		if row.alias and not config['nwrsc.private']:
			self.firstname = row.alias
			self.lastname = ""
		else:
			self.firstname = row.firstname
			self.lastname = row.lastname
		self.id = row.id
		self.carid = row.carid
		self.diffpoints = Points()
		self.pospoints = Points()
		self.finishes = defaultdict(int)
		self.events = 0

	def __getattr__(self, key):
		""" Implement getattr to match firsts, seconds, thirds, etc. """
		try:
			# 100 - turns ordering into reverse
			if key == 'attended': return 100 - self.events
			return 100 - self.finishes[{ 'firsts':1, 'seconds':2, 'thirds':3, 'fourths':4 }[key]]
		except:
			raise AttributeError("No known key %s" % key)

	def addResults(self, eventid, position, diffpoints, pospoints):
		self.finishes[position] += 1
		self.diffpoints.set(eventid, diffpoints)
		self.pospoints.set(eventid, pospoints)

	def calc(self, best, usepospoints):
		self.events = len(self.diffpoints.points)
		self.diffpoints.calc(best)
		self.pospoints.calc(best)
		if usepospoints:
			self.points = self.pospoints
		else:
			self.points = self.diffpoints



champResult = """select r.position,r.points,r.ppoints,r.eventid,r.classcode,r.carid,d.id,d.firstname,d.lastname,d.alias
				from eventresults as r, cars as c, drivers as d, events as e
				where r.eventid=e.id and e.practice=0 and r.carid=c.id and c.driverid=d.id and r.classcode like :codeglob
				order by r.classcode,d.firstname COLLATE NOCASE,d.lastname COLLATE NOCASE,r.eventid"""

def getChampResults(session, settings, codeglob = '%'):

	try:
		bestof = settings.useevents
		minevent = settings.minevents
		if settings.usepospoints:
			sortkeys = ['pospoints']
		else:
			sortkeys = ['diffpoints']
		sortkeys.extend([x for x in map(unicode.strip, settings.champsorting.split(',')) if x in Entrant.AvailableSubKeys])

		classdata = ClassData(session)
		results = session.execute(champResult, params={'codeglob':codeglob}).fetchall()
	except Exception, e:
		log.warning("Failed to load champ results: %s" % e)
		return {}

	store = {}
	for r in results:
		if not classdata.classlist[r.classcode].champtrophy:  # class doesn't get champ trophies, ignore
			continue
		if r.classcode not in store:
			store[r.classcode] = {}
		if r.id not in store[r.classcode]:
			store[r.classcode][r.id] = Entrant(r)

		store[r.classcode][r.id].addResults(r.eventid, r.position, r.points, r.ppoints)

	ret = {}
	for code, cls in store.iteritems():
		toadd = list()

		if len(cls) == 0:  # No entrants, don't bother adding it
			continue

		for entrant in cls.values():
			entrant.calc(bestof, settings.usepospoints)
			if entrant.events >= minevent:
				toadd.append(entrant)
		toadd.sort(key=operator.attrgetter(*sortkeys))
		ret[code] = toadd

	return ret

