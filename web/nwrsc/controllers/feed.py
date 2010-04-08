import logging
import os
import glob

from simplejson import JSONEncoder
from pylons import request, response, config
from nwrsc.lib.base import BaseController
from nwrsc.model import *

log = logging.getLogger(__name__)


class Series(object):
	def __init__(self, val):
		self.val = val

	def getFeed(self):
		return {'name':self.val}

def corename(file):
	base = os.path.basename(file)
	return Series(os.path.splitext(base)[0])

class FeedController(BaseController):

	def __before__(self):
		self.eventid = self.routingargs.get('eventid', None)

	def index(self):
		if self.database is None:
			return self._encode("serieslist", map(corename, glob.glob('%s/*.db' % (config['seriesdir']))))

		event = None
		if self.eventid:
			classdata = ClassData(self.session)
			event = self.session.query(Event).get(self.eventid)
			active = Class.activeClasses(self.session, self.eventid)
		
		if event:
			return self._encode("classlist", getClassResults(self.session, event, classdata, [cls.code for cls in active]))
		else:
			return self._encode("eventlist", self.session.query(Event).all())

	def _encode(self, o):
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

