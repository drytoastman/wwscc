import logging
import cStringIO
import os
import glob
import time
import operator
from decorator import decorator

from pylons import request, response, session, config, tmpl_context as c
from pylons.controllers.util import url_for

from pylons.templating import render_mako, render_mako_def
from nwrsc.lib.base import BaseController
from nwrsc.lib.bracket import Bracket
from nwrsc.lib.rungroups import *
from nwrsc.model import *

log = logging.getLogger(__name__)

@decorator
def checklist(func, self, *args, **kwargs):
	"""Decorator Wrapper function"""
	if 'list' not in request.str_GET or request.str_GET['list'] == "":
		log.error("Got request byclass without list (ref: %s)" % request.environ.get('HTTP_REFERER', ""))
		return "<h2>Missing list of classes to get</h2>"
	return func(self, *args, **kwargs)


class ResultsController(BaseController):

	def __before__(self):
		c.title = 'Scorekeeper Results'
		c.seriesname = self.settings.seriesname
		c.stylesheets = []
		c.javascript = []
		if self.database is not None:
			c.stylesheets.append(url_for(controller='db', name='results.css', eventid=None))
			self.eventid = self.routingargs.get('eventid', None)
			if self.eventid:
				c.classdata = ClassData(self.session)
				c.event = self.session.query(Event).get(self.eventid)
				c.active = Class.activeClasses(self.session, self.eventid)


	def index(self):
		if c.event:
			c.challenges = self.session.query(Challenge).filter(Challenge.eventid==c.event.id).all()
			return render_mako('/resultsindex.mako')
		elif self.database is not None:
			c.events = self.session.query(Event).all()
			return render_mako('/eventselect.mako')
		else:
			c.files = map(os.path.basename, glob.glob('%s/*.db' % (config['seriesdir'])))
			return render_mako('/databaseselect.mako')


	@checklist
	def byclass(self):
		c.title = 'Results for Class %s' % (request.str_GET['list'])
		c.header = '<h2>%s</h2>' % (c.title)
		return self._classresults(request.str_GET['list'].split(','))


	@checklist
	def bygroup(self):
		list = request.str_GET['list'].split(',')
		course = request.str_GET['course']
		c.title = 'Results for Group %s' % (request.str_GET['list'])
		c.header = '<h2>%s</h2>' % (c.title)
		codes = self.session.query(Car.classcode).join(RunOrder).distinct() \
					.filter(RunOrder.eventid == self.eventid) \
					.filter(RunOrder.course == course) \
					.filter(RunOrder.rungroup.in_(list)) \
					.all()
		return self._classresults([x.classcode for x in codes])

	def all(self):
		c.title = 'Results for All Classes'
		c.header = '<h2>Results for All Classes</h2>'
		return self._classresults([cls.code for cls in c.active])

	def post(self):
		c.results = getClassResults(self.session, c.event, c.classdata, [cls.code for cls in c.active])
		c.entrantcount = sum([len(data) for code,data in c.results.iteritems()])
		c.toptimes = self._loadTopIndexTimes() + self._loadTopTimes()
		return render_mako('db:event.mako')


	def audit(self):
		course = int(request.GET.get('course', 1))
		group = int(request.GET.get('group', 1))
		c.order = request.GET.get('order', 'firstname')
		c.entrants = getAuditResults(self.session, c.event, course, group)

		if c.order in ['firstname', 'lastname']:
			c.entrants.sort(key=lambda obj: str.lower(str(getattr(obj, c.order))))
		if c.order in ['runorder']:
			c.entrants.sort(key=lambda obj: obj.row)

		if c.event.courses > 1:
			c.title = "Audit (Course %d/Run Group %d)" % (course, group)
		else:
			c.title = "Audit (Run Group %d)" % (group)

		c.header = "<h3>%s</h3>\n" % (c.title)
		return render_mako('audit.mako')


	def grid(self):
		classmap = dict()
		groups = [RunGroupList(0)]
		for item in self.session.query(RunGroup).order_by(RunGroup.rungroup, RunGroup.gorder).filter(RunGroup.eventid==self.eventid):
			if groups[-1].groupnum != item.rungroup:
				groups.append(RunGroupList(item.rungroup))
			classmap[item.classcode] = ClassOrder(item.classcode)
			groups[-1].addClass(classmap[item.classcode])

		if request.GET.get('order', 'number') == 'position':
			for (driver,car,res) in self.session.query(Driver,Car,EventResult).join('cars', 'results').filter(EventResult.eventid==self.eventid).order_by(EventResult.position):
				if car.classcode in classmap:
					car.position = res.position
					car.sum = res.sum
					classmap[car.classcode].add(driver,car, 'position')
		else:
			for (driver,car,reg) in self.session.query(Driver,Car,Registration).join('cars', 'registration').filter(Registration.eventid==self.eventid).order_by(Car.number):
				if car.classcode in classmap:
					car.sum = 0.0
					classmap[car.classcode].add(driver,car, 'number')

		# Create the actual list of matched left,right cars
		c.groups = list()
		for group in groups[1:]:
			index = 1
			list1 = []
			list2 = []
			for cls in group.classes:
				if len(cls.first) > 0:
					xiter = iter(cls.first + [None])
					list1.extend(zip(xiter, xiter))
				if len(cls.second) > 0:
					xiter = iter(cls.second + [None])
					list2.extend(zip(xiter, xiter))
			c.groups.append((list1, list2))

		# Collapse back/back rows of single drivers
		for group in c.groups:
			for subgroup in group:
				jj = 0
				while jj < len(subgroup)-1: 
					if subgroup[jj][1] is None and subgroup[jj+1][1] is None:
						subgroup[jj] = (subgroup[jj][0], subgroup[jj+1][0])
						del subgroup[jj+1]
					jj += 1

		return render_mako('/grid.mako')


	def topreport(self):
		c.toptimes = self._loadTopTimes() + self._loadTopIndexTimes()
		if c.event.getCountedRuns() < c.event.runs:
			c.toptimes += self._loadTopTimes(True) + self._loadTopIndexTimes(True)
		return render_mako('db:toptimes.mako')

	def topindex(self):
		c.toptimes = self._loadTopIndexTimes()
		return render_mako('db:toptimes.mako')

	def topindexall(self):
		c.toptimes = self._loadTopIndexTimes(True)
		return render_mako('db:toptimes.mako')

	def topraw(self):
		c.toptimes = self._loadTopTimes()
		return render_mako('db:toptimes.mako')

	def toprawall(self):
		c.toptimes = self._loadTopTimes(True)
		return render_mako('db:toptimes.mako')

	def topseg(self):
		c.toptimes = self._loadTopSegTimes()
		return render_mako('db:toptimes.mako')


	def champ(self):
		c.events = self.session.query(Event).filter(Event.practice==False).all()
		c.results = getChampResults(self.session, self.settings)
		return render_mako('db:champ.mako')


	def challenge(self):
		challengeid = int(request.GET.get('id', 1))
		c.challenge = self.session.query(Challenge).get(challengeid)
		c.rounds = dict()
		for rnd in self.session.query(ChallengeRound).filter(ChallengeRound.challengeid == challengeid).all():
			c.rounds[rnd.round] = rnd
		loadChallengeResults(self.session, c.challenge.id, c.rounds)
		return render_mako('/challenge/challengereport.mako')


	def round(self):
		challengeid = int(request.GET.get('id', -1))
		round = int(request.GET.get('round', -1))
		c.challenge = self.session.query(Challenge).get(challengeid)
		c.round = self.session.query(ChallengeRound) \
						.filter(ChallengeRound.challengeid == challengeid) \
						.filter(ChallengeRound.round == round).first() 
		loadSingleRoundResults(self.session, c.challenge, c.round)
		return render_mako_def('/challenge/challengereport.mako', 'roundReport', round=c.round)


	def bracket(self):
		c.javascript.append('/js/jquery-1.4.2.min.js');
		challenge = self.session.query(Challenge).get(int(request.GET.get('id', 0)))
		b = Bracket(challenge.depth)  # Just getting the coords, no drawing takes place
		b.getImage()
		c.coords = b.getCoords()
		c.cid = challenge.id
		return render_mako('/challenge/bracketbase.mako')
		

	def bracketimg(self):
		id = int(request.GET.get('id', 0))
		challenge = self.session.query(Challenge).get(id)
		if challenge is None:
			return ''

		rounds = dict()
		for rnd in self.session.query(ChallengeRound).filter(ChallengeRound.challengeid == id).all():
			rounds[rnd.round] = rnd

		try:
			response.headers['Content-type'] = 'image/png'
			return Bracket(challenge.depth, rounds).getImage()
		except Exception, e:
			response.headers['Content-type'] = 'text/plain'
			return "Failed to draw bracket, did you install PIL? (%s)" % e


	def blankbracket(self):
		try:
			response.headers['Content-type'] = 'image/png'
			return Bracket(int(request.GET.get('depth', 2))).getImage()
		except Exception, e:
			response.headers['Content-type'] = 'text/plain'
			return "Failed to draw bracket, did you install PIL? (%s)" % e


	def dialins(self):
		c.order = request.GET.get('order', 'Net')
		c.filter = request.GET.get('filter', 'All')

		dialins = Dialins(self.session, self.eventid)
		if c.filter == 'Ladies':
			c.dialins = [x for x in dialins if x.classcode[0] == 'L']
		elif c.filter == 'Open':
			c.dialins = [x for x in dialins if x.classcode[0] != 'L']
		else:
			c.dialins = dialins

		if c.order == 'Diff':
			c.dialins.sort(key=operator.attrgetter('diff'))
		elif c.order == 'Net':
			c.dialins.sort(key=operator.attrgetter('net'))
			
		return render_mako('/challenge/dialins.mako')


	def _loadTopTimes(self, all=False):
		times = []
		times.append(loadTopRawTimes(self.session, c.event, c.classdata, all))
		if c.event.courses > 1:
			for ii in range(c.event.courses):
				times.append(loadTopCourseRawTimes(self.session, c.event, ii+1, c.classdata, all))
		return times

	def _loadTopIndexTimes(self, all=False):
		times = []
		times.append(loadTopNetTimes(self.session, c.event, c.classdata, all))
		if c.event.courses > 1:
			for ii in range(c.event.courses):
				times.append(loadTopCourseNetTimes(self.session, c.event, ii+1, c.classdata, all))
		return times


	def _loadTopSegTimes(self):
		times = []
		for ii in range(c.event.courses):
			for jj in range(c.event.getSegmentCount()):
				times.append(loadTopSegRawTimes(self.session, c.event, ii+1, jj+1, c.classdata))
		return times


	def _classresults(self, codes):
		c.results = getClassResults(self.session, c.event, c.classdata, codes)
		c.ismobile = False
		return render_mako('db:classresult.mako')

