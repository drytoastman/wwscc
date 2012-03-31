import logging
import os
import glob
import time
from datetime import datetime

from pylons import request, response, session, config, tmpl_context as c
from pylons.templating import render_mako
from pylons.decorators import jsonify
from nwrsc.lib.base import BaseController, BeforePage
from nwrsc.model import *

log = logging.getLogger(__name__)

class AnnouncerController(BaseController):

	def __before__(self):
		if not config['nwrsc.private']:
			raise BeforePage("Announcer currently only available if server is configured private")

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
			c.files = map(os.path.basename, glob.glob('%s/*.db' % (config['seriesdir'])))
			return render_mako('/databaseselect.mako')


	@jsonify
	def last(self):
		timestamp = datetime.fromtimestamp(int(request.GET.get('time', 0)))
		data = [{'updated':time.mktime(row[0].timetuple()), 'carid':row[1]} \
				for row in self.session.query(EventResult.updated, EventResult.carid)\
				.filter(EventResult.eventid==self.eventid).filter(EventResult.updated > timestamp)\
				.order_by(EventResult.updated.desc()).limit(4).all()]
		return {'data': data}


	def runorder(self):
		c.order = loadNextRunOrder(self.session, c.event, int(request.GET.get('carid', 0)))
		return render_mako('/announcer/runorder.mako')


	@jsonify
	def toptimes(self):
		c.classdata = ClassData(self.session)
		c.highlight = int(request.GET.get('carid', 0))

		ret = {}
		ret['updated'] = int(request.GET.get('updated', 0)) # Return it

		c.title = "Raw"
		c.toptimes = loadTopRawTimes(self.session, c.event, c.classdata)
		ret['topraw'] = render_mako('/announcer/toptimes.mako').replace('\n', '')

		c.title = "Net"
		c.toptimes = loadTopNetTimes(self.session, c.event, c.classdata)
		ret['topnet'] = render_mako('/announcer/toptimes.mako').replace('\n', '')

		if c.event.getSegmentCount() > 0:
			for ii in range(1, c.event.getSegmentCount()+1):
				c.title = "Seg %d" % (ii)
				c.toptimes = loadTopSegRawTimes(self.session, c.event, 1, ii, c.classdata)
				ret['topseg%d' % ii] = render_mako('/announcer/toptimes.mako').replace('\n', '')

		return ret



	@jsonify
	def results(self):
		carid = int(request.GET.get('carid', 0))

		(c.driver,c.car) = self.session.query(Driver,Car).join('cars').filter(Car.id==carid).first()
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

