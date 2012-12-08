from pylons import request, tmpl_context as c
from pylons.templating import render_mako
from pylons.decorators import jsonify
from pylons.controllers.util import redirect, url_for
from paste.fileapp import FileApp
from announcer import AnnouncerController
from json import JsonController
from nwrsc.model import Event

class MobileController(JsonController, AnnouncerController): 

	# One duplicate between both parents that we prefer announcer for
	__before__ = AnnouncerController.__before__

	@jsonify
	def last(self):
		"""
			Get the timestamps of the last 2 updated run entries, announcer panel periodically calls this
			to see if there is any real data to get
		"""
		query = self.session.query(AnnouncerData.updated, AnnouncerData.carid)
		query = query.filter(AnnouncerData.eventid==self.eventid)
		if 'class' in request.GET:
			query = query.join(Class).filter(Class.code == request.GET['class'])
		if 'time' in request.GET:
			query = query.filter(AnnouncerData.updated > datetime.fromtimestamp(int(request.GET['time'])))
		query = query.order_by(AnnouncerData.updated.desc())

		data = []
		for row in query.limit(2).all():
			data.append({'updated':time.mktime(row[0].timetuple()), 'carid':row[1]})

		return {'data': data}

	def topraw(self):
		return self._loadTopTimes()['topraw']

	def topnet(self):
		return self._loadTopTimes()['topnet']

	def runlist(self):
		self._loadResults(int(request.GET.get('carid', 0)))
		return render_mako("/announcer/runs.mako")

	def classlist(self):
		self._loadResults(int(request.GET.get('carid', 0)))
		return render_mako("/announcer/class.mako")

	def champlist(self):
		self._loadResults(int(request.GET.get('carid', 0)))
		return render_mako("/announcer/champ.mako")

