import logging
import os

from simplejson import JSONEncoder
from pylons import request, response, config
from nwrsc.controllers.lib.base import BaseController
from nwrsc.model import *

log = logging.getLogger(__name__)


class FeedController(BaseController):
	"""
		Generic feed controller that provides standard behaviour used by both the json and xml feeds
	"""

	def __before__(self):
		self.eventid = self.routingargs.get('eventid', None)

	def index(self):
		if self.database is None:
			return self._encode("serieslist", self._databaseList())

		event = None
		if self.eventid == 'classes':
			return self._encode("seriesclasses", self.session.query(Class).all())
		elif self.eventid == 'indexes':
			return self._encode("seriesindicies", self.session.query(Index).all())
		elif self.eventid:
			classdata = ClassData(self.session)
			event = self.session.query(Event).get(self.eventid)
			active = Class.activeClasses(self.session, self.eventid)
		
		if event:
			return self._encode("classlist", getClassResults(self.session, self.settings, event, classdata, [cls.code for cls in active]))
		else:
			return self._encode("eventlist", self.session.query(Event).all())

	def _encode(self, head, o):
		return "What?"

	def challenge(self, other = 0):
		challengeid = int(other)
		if challengeid == 0:
			return self._encode("challengelist", self.session.query(Challenge).filter(Challenge.eventid==self.eventid).all())

		rounds = dict()
		for rnd in self.session.query(ChallengeRound).filter(ChallengeRound.challengeid == challengeid).all():
			rounds[rnd.round] = rnd
		loadChallengeResults(self.session, challengeid, rounds)
		return self._encode("roundlist", rounds.values())



	class Entry(object):
		def __init__(self, **kwargs):
			for k,v in kwargs.iteritems():
				setattr(self, k, v)
		def getFeed(self):
			ret = dict()
			for k,v in self.__dict__.iteritems():
				if v is None or v == '': continue
				if isinstance(v, float):
					ret[k] = "%0.3f" % (v)
				else:
					ret[k] = v
			return ret


	def scca(self):
		classdata = ClassData(self.session)
		event = self.session.query(Event).get(self.eventid)
		active = Class.activeClasses(self.session, self.eventid)
		entries = list()
		results = getClassResults(self.session, self.settings, event, classdata, [cls.code for cls in active])
		for cls in results:
			for res in results[cls]:
				entries.append(self.Entry(FirstName=res.firstname, 
											LastName=res.lastname,
											MemberNo=res.membership,
											Class=res.classcode,
											Index=res.indexcode,
											Pos=res.position,
											CarModel="%s %s %s %s" % (res.year, res.make, res.model, res.color),
											CarNo="%s" % (res.number),
											TotalTm=res.sum))

		return self._encode("Entries", entries)

