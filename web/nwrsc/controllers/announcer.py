import time
from datetime import datetime

from pylons import request, response, session, config, tmpl_context as c
from pylons.templating import render_mako
from pylons.decorators import jsonify
from nwrsc.controllers.lib.base import BaseController, BeforePage
from nwrsc.model import *

class AnnouncerController(BaseController):

	def __before__(self):
		self.eventid = self.routingargs.get('eventid', None)
		if self.eventid:
			c.event = self.session.query(Event).get(self.eventid)


	def index(self):
		if self.eventid:
			return render_mako('/announcer/main.mako')
		elif self.database is not None:
			c.events = self.session.query(Event).all()
			return render_mako('/eventselect.mako')
		else:
			return databaseSelector()


	@jsonify
	def last(self):
		"""
			Get the timestamps of the last 2 updated run entries, announcer panel periodically calls this
			to see if there is any real data to get
		"""
		timestamp = datetime.fromtimestamp(int(request.GET.get('time', 0)))
		data = [{'updated':time.mktime(row[0].timetuple()), 'carid':row[1]} \
				for row in self.session.query(EventResult.updated, EventResult.carid)\
				.filter(EventResult.eventid==self.eventid).filter(EventResult.updated > timestamp)\
				.order_by(EventResult.updated.desc()).limit(2).all()]
		return {'data': data}


	def runorder(self):
		"""
			Returns the HTML to render the NextToFinish box
		"""
		c.order = loadNextRunOrder(self.session, c.event, int(request.GET.get('carid', 0)))
		return render_mako('/announcer/runorder.mako')


	@jsonify
	def toptimes(self):
		"""
			Returns the top times tables that are shown in the announer panel
		"""
		c.classdata = ClassData(self.session)
		c.highlight = int(request.GET.get('carid', 0))

		ret = {}
		ret['updated'] = int(request.GET.get('updated', 0)) # Return it

		tts = TopTimesStorage(self.session, c.event, c.classdata)

		c.toptimes = tts.getList(allruns=False, raw=True, course=0)
		c.toptimes.title = "Raw"
		ret['topraw'] = render_mako('/announcer/toptimes.mako').replace('\n', '')

		c.toptimes = tts.getList(allruns=False, raw=False, course=0)
		c.toptimes.title = "Net"
		ret['topnet'] = render_mako('/announcer/toptimes.mako').replace('\n', '')

		if c.event.getSegmentCount() > 0:
			for ii in range(1, c.event.getSegmentCount()+1):
				c.title = "Seg %d" % (ii)
				c.toptimes = loadTopSegRawTimes(self.session, c.event, 1, ii, c.classdata)
				ret['topseg%d' % ii] = render_mako('/announcer/toptimes.mako').replace('\n', '')

		return ret


	def nexttofinish(self):
		carid = getNextCarIdInOrder(self.session, c.event, int(request.GET.get('carid', 0)))
		return self._results(carid)


	def results(self):
		return self._results(int(request.GET.get('carid', 0)))


	@jsonify
	def _results(self, carid):
		"""
			Returns the collection of tables shown for a single entrant
		"""
		(c.driver,c.car) = self.session.query(Driver,Car).join('cars').filter(Car.id==carid).first()
		if c.driver.alias and not config['nwrsc.private']:
			c.driver.firstname = c.driver.alias
			c.driver.lastname = ""

		c.cls = self.session.query(Class).filter(Class.code==c.car.classcode).first()
		c.highlight = carid
		c.marker = time.strftime('%I:%M:%S')

		c.runs = {}
		for r in self.session.query(Run).filter(Run.carid==carid).filter(Run.eventid==self.eventid): 
			c.runs[r.run] = r
		c.results = getClassResultsShort(self.session, c.event, c.cls)

		self._createInfo(carid)

		c.champresults = getChampResults(self.session, self.settings, c.cls.code).get(c.cls.code, [])

		ret = {}
		ret['updated'] = int(request.GET.get('updated', 0)) # Return it
		ret['entrantresult'] = render_mako('/announcer/entrant.mako').replace('\n', '')
		return ret


	def _createInfo(self, carid):
		c.rdiff = 0
		c.ndiff = 0
		c.change = ''
		c.theory = ''

		one = two = last = c.runs[len(c.runs)]
		for r in c.runs.itervalues():
			if r.norder == 1:
				one = r
			elif r.norder == 2:
				two = r

		# Find newpos
		for r in c.results:
			if r.carid == carid:
				newpos = r.position
				break

		# Find diffs and position change
		if last.run != one.run:
			c.ndiff = last.net - one.net
			c.rdiff = last.raw - one.raw
			judge = one.net
		else:
			c.ndiff = last.net - two.net
			c.rdiff = last.raw - two.raw
			judge = two.net

		origpos = 1
		for r in c.results:
			if round(judge,3) < round(r.sum,3):
				origpos = r.position - 1
				if newpos != origpos:
					c.change = "%s to %s" % (origpos, newpos)
				break

		# Find any theoretical changes
		if c.rdiff > 0 or last.cones == 0:
			return

		newnet = last.net - (c.event.conepen * last.cones)
		tpos = origpos;
		for r in c.results:
			if newnet < r.sum:
				tpos = r.position
				break

		if tpos < newpos:
			c.theory = "%s to %s" % (origpos, tpos)

