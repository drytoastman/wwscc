import time
import operator
from datetime import datetime

from pylons import request, response, session, config, tmpl_context as c
from pylons.templating import render_mako
from pylons.decorators import jsonify
from nwrsc.controllers.lib.base import BaseController, BeforePage
from nwrsc.model import *


class ExtraResult(object):
	def __init__(self, copy, **kwargs):
		self.__dict__ = copy.__dict__.copy()
		for k, v in kwargs.iteritems():
			setattr(self, k, v)

	def __getattr__(self, k):
		return 42


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
			return render_mako('/results/eventselect.mako')
		else:
			return self.databaseSelector()


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
		c.order = loadNextRunOrder(self.session, c.event, int(request.GET.get('carid', 0)))
		return render_mako('/announcer/runorder.mako')


	@jsonify
	def toptimes(self):
		"""
			Returns the top times tables that are shown in the announer panel
		"""
		return self._loadTopTimes()


	def _loadTopTimes(self):
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
		return self._allentrant(getNextCarIdInOrder(self.session, c.event, int(request.GET.get('carid', 0))))

	def results(self):
		return self._allentrant(int(request.GET.get('carid', 0)))

	@jsonify
	def _allentrant(self, carid):
		self._loadResults(carid)
		ret = {}
		ret['updated'] = int(request.GET.get('updated', 0)) # Return it
		ret['entrantresult'] = render_mako('/announcer/entrant.mako').replace('\n', '')
		return ret


	def _loadResults(self, carid):
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
		c.announcer = self.session.query(AnnouncerData).filter(AnnouncerData.eventid==self.eventid).filter(AnnouncerData.carid==carid).first()
		if c.announcer is None:
			raise BeforePage('')

		c.results = getClassResultsShort(self.session, self.settings, c.event, c.cls)
		for r in c.results:
			if r.carid == carid:
				activeentrant = r

		# Just get runs from last course that was recorded
		c.runs = {}
		for r in self.session.query(Run).filter(Run.carid==carid).filter(Run.eventid==self.eventid).filter(Run.course==c.announcer.lastcourse): 
			r.rdiff = None
			r.ndiff = None
			c.runs[r.run] = r

		runlist = sorted(c.runs.keys())
		lastrun = c.runs[runlist[-1]]
		if c.announcer.rawdiff or c.announcer.netdiff:
			lastrun.rdiff = c.announcer.rawdiff
			lastrun.ndiff = c.announcer.netdiff

		if c.announcer.oldsum > 0:
			c.improvedon = ExtraResult(activeentrant, position='old', sum=c.announcer.oldsum, diff=0)
			c.results.append(c.improvedon)

		if c.announcer.potentialsum > 0:
			c.couldhave = ExtraResult(activeentrant, position='raw', sum=c.announcer.potentialsum, diff=0)
			c.couldhaverun = lastrun
			c.results.append(c.couldhave)

		c.results.sort(key=operator.attrgetter('sum'))
		c.champresults = getChampResults(self.session, self.settings, c.cls.code).get(c.cls.code, [])

