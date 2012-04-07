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
			return self._encode("classlist", getClassResults(self.session, event, classdata, [cls.code for cls in active]))
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

