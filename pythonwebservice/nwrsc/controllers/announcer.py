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
		query = self.session.query(EventResult.updated, EventResult.carid)
		query = query.filter(EventResult.eventid==self.eventid)
		if 'class' in request.GET:
			query = query.join(Class).filter(Class.code == request.GET['class'])
		if 'time' in request.GET:
			query = query.filter(EventResult.updated > datetime.fromtimestamp(int(request.GET['time'])))
		query = query.order_by(EventResult.updated.desc())

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


		c.results = getClassResultsShort(self.session, self.settings, c.event, c.cls)
		lastcourse = c.results[0].lastcourse or 1
		activeentrant = [x for x in c.results if x.carid==carid][0]

		# Just get runs from last course that was recorded
		c.runs = {}
		for r in self.session.query(Run).filter(Run.carid==carid).filter(Run.eventid==self.eventid).filter(Run.course==lastcourse): 
			r.rdiff = None
			r.ndiff = None
			c.runs[r.run] = r

		if len(c.runs) > 1:
			runlist = sorted(c.runs.keys())
			lastrun = c.runs[runlist[-1]]
			if lastrun.norder == 1:  # we improved our position
				# find run with norder = 2, create the old entry with sum - lastrun + prevrun
				prevbest = [x for x in c.runs.values() if x.norder == 2][0]
				lastrun.rdiff = lastrun.raw - prevbest.raw
				lastrun.ndiff = lastrun.net - prevbest.net
				theory = activeentrant.sum - lastrun.net + prevbest.net
				c.improvedon = ExtraResult(activeentrant, position='old', sum=theory, diff=0)
				c.results.append(c.improvedon)

			if lastrun.cones != 0 or lastrun.gates != 0:
				# add table entry with what could have been without penalties
				index = ClassData(self.session).getEffectiveIndex(activeentrant.classcode, activeentrant.indexcode)
				curbest = [x for x in c.runs.values() if x.norder == 1][0]
				theory = activeentrant.sum - curbest.net + ( lastrun.raw * index )
				if theory < activeentrant.sum:
					lastrun.rdiff = lastrun.raw - curbest.raw
					lastrun.ndiff = lastrun.net - curbest.net
					c.couldhave = ExtraResult(activeentrant, position='raw', sum=theory, diff=0)
					c.couldhaverun = lastrun
					c.results.append(c.couldhave)

		c.results.sort(key=operator.attrgetter('sum'))
		c.champresults = getChampResults(self.session, self.settings, c.cls.code).get(c.cls.code, [])

		ret = {}
		ret['updated'] = int(request.GET.get('updated', 0)) # Return it
		ret['entrantresult'] = render_mako('/announcer/entrant.mako').replace('\n', '')
		return ret


