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

