from pylons import tmpl_context as c
from pylons.templating import render_mako, render_mako_def
from nwrsc.model import *
from mobile import MobileController

class LiveController(MobileController):

	def index(self):
		if self.eventid:
			c.event = self.event
			c.classes = self.session.query(Class).all()
			return render_mako('/live/selector.mako')
		elif self.database is not None:
			c.events = self.session.query(Event).all()
			return render_mako('/results/eventselect.mako')
		else:
			return self.databaseSelector()

	def browser(self):
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

