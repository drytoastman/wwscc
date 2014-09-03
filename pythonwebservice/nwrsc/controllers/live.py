from pylons import tmpl_context as c, request
from pylons.controllers.util import redirect, url_for
from pylons.templating import render_mako, render_mako_def
from nwrsc.model import *
from mobile import MobileController

class LiveController(MobileController):

	def index(self):
		if self.eventid:
			return self._browser()
		elif self.database is not None:
			return self._events()
		else:
			return self._database()


	def _database(self):
		c.dblist = self._databaseList(archived=False)
		return render_mako('/live/database.mako')

	def _events(self):
		c.events = self.session.query(Event).all()
		return render_mako('/live/events.mako')

	def _browser(self):
		c.event = self.event
		c.classes = [x[0] for x in self.session.query(Class.code).all()]
		return render_mako('/live/browser.mako')


	def Event(self):
		carid = int(self.routingargs.get('other', 0))
		c.results = self._classlist(carid)
		c.e2label = self.e2label
		return render_mako_def('/live/tables.mako', 'classlist')

	def Champ(self):
		carid = int(self.routingargs.get('other', 0))
		c.champ = self._champlist(carid)
		c.cls = self.cls
		c.e2label = self.e2label
		return render_mako_def('/live/tables.mako', 'champlist')

	def PAX(self):
		carid = int(self.routingargs.get('other', 0))
		c.e2label = self.e2label
		c.toptimes = self._loadTopTimes(carid, raw=False)
		return render_mako('/announcer/topnettimes.mako').replace('\n', '')

	def Raw(self):
		carid = int(self.routingargs.get('other', 0))
		c.e2label = self.e2label
		c.toptimes = self._loadTopTimes(carid, raw=True)
		return render_mako('/announcer/toprawtimes.mako').replace('\n', '')

