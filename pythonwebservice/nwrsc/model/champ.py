from event import Event
from pylons import config
from classlist import ClassData
from collections import defaultdict
import logging
import operator

log = logging.getLogger(__name__)

class PointStorage(object):

	def __init__(self):
		self.storage = {}
		self.total = 0
		self.drop = []
		self.usingbest = 0

	def get(self, eventid):
		return self.storage.get(eventid, None)

	def set(self, eventid, points):
		self.storage[eventid] = points

	def theory(self, eventid, points):
		save = self.storage[eventid]
		self.storage[eventid] = points
		self.calc(self.usingbest)
		ret = self.total
		self.storage[eventid] = save
		self.calc(self.usingbest)
		return ret
		
	def calc(self, bestof):
		self.total = 0
		self.drop = []
		self.usingbest = bestof
		for ii, points in enumerate(sorted(self.storage.iteritems(), key=lambda x:x[1], reverse=True)):
			if ii < bestof:
				self.total += points[1]  # Add to total points
			else:
				self.drop.append(points[0])  # Otherwise this is a drop event, mark eventid

	def __cmp__(self, other):
		""" provides the comparison for sorting """
		return cmp(other.total, self.total)


class XXEntrant(object):

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
		self.diffpoints = PointStorage()
		self.pospoints = PointStorage()
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
		self.events = len(self.diffpoints.storage)
		self.diffpoints.calc(best)
		self.pospoints.calc(best)
		if usepospoints:
			self.points = self.pospoints
		else:
			self.points = self.diffpoints



champResult = """select r.position,r.diffpoints,r.pospoints,r.eventid,r.classcode,r.carid,d.id,d.firstname,d.lastname,d.alias
				from eventresults as r, cars as c, drivers as d, events as e
				where r.eventid=e.id and e.practice=0 and r.carid=c.id and c.driverid=d.id and r.classcode like :codeglob
				order by r.classcode,d.firstname COLLATE NOCASE,d.lastname COLLATE NOCASE,r.eventid"""

activeEvents = """select count(date) from events where date <= date('now') and practice=0"""

def getChampResults(session, settings, codeglob = '%'):

	try:
		todrop = settings.useevents # reusing old field for new purpose, can't rename in sqlite
		eventcount = session.execute(activeEvents).fetchone()[0]
		bestof = max(todrop, eventcount - todrop)

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

		store[r.classcode][r.id].addResults(r.eventid, r.position, r.diffpoints, r.pospoints)

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

