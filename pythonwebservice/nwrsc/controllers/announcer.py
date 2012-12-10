import time
import operator
from datetime import datetime

from pylons import request, response, session, config, tmpl_context as c
from pylons.templating import render_mako
from pylons.decorators import jsonify
from nwrsc.controllers.lib.base import BaseController, BeforePage
from nwrsc.model import *
from mobile import MobileController


CONVERT = {'old':'improvedon', 'raw':'couldhave', 'current':'highlight'}

class AnnouncerController(MobileController):

	def index(self):
		if self.eventid:
			c.event = self.event
			return render_mako('/announcer/main.mako')
		elif self.database is not None:
			c.events = self.session.query(Event).all()
			return render_mako('/results/eventselect.mako')
		else:
			return self.databaseSelector()


	def e2label(self, e):
		if hasattr(e, 'label'):
			return CONVERT.get(getattr(e, 'label',''), '') 
		if hasattr(e, '__getitem__'):
			return CONVERT.get(e.get('label',''), '') 
		return ''

	@jsonify
	def last(self):
		"""
			Get the timestamps of the last 2 updated run entries, announcer panel periodically calls this
			to see if there is any real data to get
		"""
		query = self.session.query(AnnouncerData.updated, AnnouncerData.carid)
		query = query.filter(AnnouncerData.eventid==self.eventid)
		if 'time' in request.GET:
			query = query.filter(AnnouncerData.updated > datetime.fromtimestamp(int(request.GET['time'])))
		query = query.order_by(AnnouncerData.updated.desc())

		data = []
		for row in query.limit(2).all():
			data.append({'updated':time.mktime(row[0].timetuple()), 'carid':row[1]})

		return {'data': data}


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
		data = self._toptimes(int(request.GET.get('carid', 0)))
		c.e2label = self.e2label

		ret = {}
		ret['updated'] = int(request.GET.get('updated', 0)) # Return it

		c.toptimes = data['topnet']
		ret['topnet'] = render_mako('/announcer/topnettimes.mako').replace('\n', '')

		c.toptimes = data['topraw']
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

