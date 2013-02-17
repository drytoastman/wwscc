import time
import operator
from datetime import datetime

from pylons import request, response, session, config, tmpl_context as c
from pylons.templating import render_mako
from pylons.decorators import jsonify
from nwrsc.controllers.lib.base import BaseController, BeforePage
from nwrsc.model import *
from mobile import MobileController


class AnnouncerController(MobileController):

	def index(self):
		c.title = 'Scorekeeper Announcer'

		if self.eventid:
			c.javascript = ['/js/announcer.js']
			c.stylesheets = ['/css/announcer.css']
			c.event = self.event
			return render_mako('/announcer/main.mako')
		elif self.database is not None:
			c.events = self.session.query(Event).all()
			return render_mako('/results/eventselect.mako')
		else:
			return self.databaseSelector()

	def runorder(self):
		"""
			Returns the HTML to render the NextToFinish box
		"""
		c.order = loadNextRunOrder(self.session, self.event, int(request.GET.get('carid', 0)))
		return render_mako('/announcer/runorder.mako')


	@jsonify
	def toptimes(self):
		"""
			Returns the top times tables that are shown in the announer panel
		"""
		carid = int(request.GET.get('carid', 0))
		##data = self._toptimes(int(request.GET.get('carid', 0)))
		c.e2label = self.e2label

		ret = {}
		ret['updated'] = int(request.GET.get('updated', 0)) # Return it

		c.toptimes = self._loadTopTimes(carid, raw=False) #data['topnet']
		ret['topnet'] = render_mako('/announcer/topnettimes.mako').replace('\n', '')

		c.toptimes = self._loadTopTimes(carid, raw=True) #data['topraw']
		ret['topraw'] = render_mako('/announcer/toprawtimes.mako').replace('\n', '')

		if self.event.getSegmentCount() > 0:
			for ii in range(1, self.event.getSegmentCount()+1):
				c.toptimes = data['topseg%d' % ii]
				ret['topseg%d' % ii] = render_mako('/announcer/topsegtimes.mako').replace('\n', '')

		return ret


	def nexttofinish(self):
		nextid = getNextCarIdInOrder(self.session, self.event, int(request.GET.get('carid', 0)))
		return self._allentrant(nextid)

	def results(self):
		return self._allentrant(int(request.GET.get('carid', 0)))

	@jsonify
	def _allentrant(self, carid):
		"""
			Returns the collection of tables shown for a single entrant
		"""
		data = self._entrant(carid)
		c.event = self.event
		c.runs = data['runlist']
		c.results = data['classlist']
		c.champ = data['champlist']
		c.driver = self.driver
		c.cls = self.cls
		c.e2label = self.e2label
		
		ret = {}
		ret['updated'] = int(request.GET.get('updated', 0)) # Return it
		ret['entrantresult'] = render_mako('/announcer/entrant.mako').replace('\n', '')
		return ret

