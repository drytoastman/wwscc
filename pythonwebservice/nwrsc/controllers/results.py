import logging
import operator
from decorator import decorator

from pylons import request, response, session, config, tmpl_context as c
from pylons.controllers.util import url_for, abort

from pylons.templating import render_mako, render_mako_def
from nwrsc.lib.bracket import Bracket
from nwrsc.controllers.lib.base import BaseController, BeforePage
from nwrsc.lib.rungroups import *
from nwrsc.model import *

log = logging.getLogger(__name__)

def myint(val):
	try:
		return int(val)
	except:
		return -1

@decorator
def checklist(func, self, *args, **kwargs):
	"""Decorator Wrapper function"""
	if 'list' not in request.str_GET or request.str_GET['list'] == "":
		log.error("Got request byclass without list (ref: %s)" % request.environ.get('HTTP_REFERER', ""))
		return "<h2>Missing list of classes to get</h2>"
	return func(self, *args, **kwargs)


class ResultsController(BaseController):
	"""
		Provides all the unathenticated results, audit pages, grid printouts, etc
	"""

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
				if c.event is None:
					abort(404, "Invalid event id")


	def index(self):
		if c.event:
			c.challenges = self.session.query(Challenge).filter(Challenge.eventid==c.event.id).all()
			return render_mako('/results/resultsindex.mako')
		elif self.database is not None:
			c.events = self.session.query(Event).order_by(Event.date).all()
			return render_mako('/results/eventselect.mako')
		else:
			return self.databaseSelector(archived=True)


	@checklist
	def byclass(self):
		c.title = 'Results for Class %s' % (request.str_GET['list'])
		c.header = '<h2>%s</h2>' % (c.title)
		c.results = getClassResults(self.session, self.settings, c.event, c.classdata, request.str_GET['list'].split(','))
		return render_mako('db:classresult.mako')

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
		c.results = getClassResults(self.session, self.settings, c.event, c.classdata, [x.classcode for x in codes])
		return render_mako('db:classresult.mako')

	def all(self):
		c.title = 'Results for All Classes'
		c.header = '<h2>Results for All Classes</h2>'
		c.results = getClassResults(self.session, self.settings, c.event, c.classdata, [cls.code for cls in c.active])
		return render_mako('db:classresult.mako')


	def post(self):
		c.results = getClassResults(self.session, self.settings, c.event, c.classdata, [cls.code for cls in c.active])
		c.entrantcount = sum([len(data) for code,data in c.results.iteritems()])
		c.toptimes = TopTimesStorage(self.session, c.event, c.classdata)
		return render_mako('db:event.mako')


	def audit(self):
		if not config['nwrsc.private']:
			raise BeforePage("Audit only available if server configured private")

		course = myint(request.GET.get('course', 1))
		group = myint(request.GET.get('group', 1))
		c.order = request.GET.get('order', 'firstname')
		c.entrants = getAuditResults(self.session, self.settings, c.event, course, group)

		if c.order in ['firstname', 'lastname']:
			c.entrants.sort(key=lambda obj: str.lower(str(getattr(obj, c.order))))
		if c.order in ['runorder']:
			c.entrants.sort(key=lambda obj: obj.row)

		if c.event.courses > 1:
			c.title = "Audit (Course %d/Run Group %d)" % (course, group)
		else:
			c.title = "Audit (Run Group %d)" % (group)

		c.header = "<h3>%s</h3>\n" % (c.title)
		return render_mako('/results/audit.mako')


	def grid(self):
		if not config['nwrsc.private']:
			raise BeforePage("Grid for onsite use only")

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

		return render_mako('/results/grid.mako')


	def topindex(self):
		tt = TopTimesStorage(self.session, c.event, c.classdata)
		c.toptimes = [tt.getList(allruns=False, raw=False, course=0)]
		return render_mako('db:toptimes.mako')

	def topindexall(self):
		tt = TopTimesStorage(self.session, c.event, c.classdata)
		c.toptimes = [tt.getList(allruns=True, raw=False, course=0)] 
		return render_mako('db:toptimes.mako')

	def topraw(self):
		tt = TopTimesStorage(self.session, c.event, c.classdata)
		c.toptimes = [tt.getList(allruns=False, raw=True, course=0)] 
		return render_mako('db:toptimes.mako')

	def toprawall(self):
		tt = TopTimesStorage(self.session, c.event, c.classdata)
		c.toptimes = [tt.getList(allruns=True, raw=True, course=0)] 
		return render_mako('db:toptimes.mako')

	"""
	def topseg(self):
		c.toptimes = TopTimesStorage(self.session, c.event, c.classdata)
		return render_mako('db:toptimes.mako')
	"""


	def champ(self):
		c.events = self.session.query(Event).filter(Event.practice==False).order_by(Event.date).all()
		c.results = getChampResults(self.session, self.settings)
		return render_mako('db:champ.mako')


	def challenge(self):
		challengeid = myint(request.GET.get('id', 1))
		c.challenge = self.session.query(Challenge).get(challengeid)
		c.rounds = dict()
		for rnd in self.session.query(ChallengeRound).filter(ChallengeRound.challengeid == challengeid).all():
			c.rounds[rnd.round] = rnd
		loadChallengeResults(self.session, c.challenge.id, c.rounds)
		return render_mako('/challenge/challengereport.mako')


	def round(self):
		challengeid = myint(request.GET.get('id', -1))
		round = myint(request.GET.get('round', -1))
		c.challenge = self.session.query(Challenge).get(challengeid)
		if c.challenge is None:
			return "No challenge found"
		c.round = self.session.query(ChallengeRound) \
						.filter(ChallengeRound.challengeid == challengeid) \
						.filter(ChallengeRound.round == round).first() 
		if c.round is None:
			return "No round found"
		loadSingleRoundResults(self.session, c.challenge, c.round)
		return render_mako_def('/challenge/challengereport.mako', 'roundReport', round=c.round)


	def bracket(self):
		c.javascript.append('/js/external/jquery-1.9.0.js');
		challenge = self.session.query(Challenge).get(myint(request.GET.get('id', 0)))
		if challenge is None:
			abort(404, "Invalid or no challenge id")
		b = Bracket(challenge.depth)  # Just getting the coords, no drawing takes place
		b.getImage()
		c.coords = b.getCoords()
		c.cid = challenge.id
		return render_mako('/challenge/bracketbase.mako')
		

	def bracketimg(self):
		id = myint(request.GET.get('id', 0))
		challenge = self.session.query(Challenge).get(id)
		if challenge is None:
			return ''

		rounds = dict()
		for rnd in self.session.query(ChallengeRound).filter(ChallengeRound.challengeid == id).all():
			rounds[rnd.round] = rnd

		loadChallengeResults(self.session, challenge.id, rounds)

		try:
			response.headers['Content-type'] = 'image/png'
			return Bracket(challenge.depth, rounds).getImage()
		except Exception, e:
			response.headers['Content-type'] = 'text/plain'
			return "Failed to draw bracket, did you install PIL? (%s)" % e


	def blankbracket(self):
		try:
			response.headers['Content-type'] = 'image/png'
			return Bracket(myint(request.GET.get('depth', 2))).getImage()
		except Exception, e:
			response.headers['Content-type'] = 'text/plain'
			return "Failed to draw bracket, did you install PIL? (%s)" % e


	def dialins(self):
		c.order = request.GET.get('order', 'Net')
		c.filter = request.GET.get('filter', 'All')
		c.dialins = Dialins(self.session, self.eventid)

		if c.order == 'Diff':
			c.dialins.sort(key=operator.attrgetter('diff'))
		else:
			c.dialins.sort(key=operator.attrgetter('net'))
			
		return render_mako('/challenge/dialins.mako')



