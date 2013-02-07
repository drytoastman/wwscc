
from pylons.templating import render_mako
from pylons import request, tmpl_context as c

from nwrsc.model import *
from nwrsc.controllers.lib.base import BaseController
from sqlalchemy import create_engine

from collections import defaultdict
import operator
import string
import pprint
import re
import logging
import simplejson
import datetime
import time
log = logging.getLogger(__name__)

seriesyear = re.compile('(\w+?)(\d+)')


class Result(object):
	__slots__ = ['last', 'first', 'years', 'series', 'isttotal', 'istavg', 'istqualify', 'pcchamp', 'pcevents', 'pcqualify']
	def __init__(self, last, first):
		self.last = last
		self.first = first
		self.years = set()
		self.series = set()
		self.isttotal = 0
		self.istavg = 0
		self.istqualify = True
		self.pcchamp = 0
		self.pcevents = defaultdict(int)
		self.pcqualify = True


def maxitem(d):
	if len(d) == 0:
		return 0
	return max(d.itervalues())


class HistoryController(BaseController):

	def __before__(self):
		c.stylesheets = ['/css/history.css']
		c.recentyear = datetime.date.today().year - 2

	def index(self):
		return render_mako('/history/main.mako')

	def report(self):
		""" 
			POST:
			isttotal, istavg
			pcseries, pcyearmax, pcsinceyear, pcchamp
			exclusionsonly, selection
		"""

		isttotal = int(request.POST['isttotal'])
		istavg = int(request.POST['istavg'])
		pcseries = map(string.lower, map(string.strip, request.POST['pcseries'].split(',')))
		pcyearmax = int(request.POST['pcyearmax'])
		pcsinceyear = int(request.POST['pcsinceyear'])
		pcchamp = 'pcchamp' in request.POST

		results = self._getData(pcseries=pcseries, pcyearmax=pcyearmax, pcsinceyear=pcsinceyear, pcchamp=pcchamp, isttotal=isttotal, istavg=istavg)
		c.results = results.itervalues()
		if 'exclusionsonly' in request.POST:
			c.results = [x for x in c.results if not (x.istqualify and x.pcqualify)]
		c.results = sorted(c.results, key=operator.attrgetter('last', 'first'))

		c.names = ['last', 'first']
		for key in ('years', 'series', 'isttotal', 'istavg', 'pcchamp', 'pcevents', 'istqualify', 'pcqualify'):
			if 'col'+key in request.POST:
				c.names.append(key)

		selection = request.POST['selection']
		if selection == 'CSV':
			return self.csv("cards", c.names, c.results)
		
		return render_mako('/history/tableoutput.mako')


	def _getData(self, pcseries = ['nwr','pro'], pcyearmax = 4, pcsinceyear = 2008, pcchamp = True, isttotal = 10, istavg = 3):
		"""
			punchcard
				championship in NWR/PRO disqualifies
				> 4 events in NWR/PRO in last 4 years disqualifies

			IST
				total events in all series
				average where #year == active years
		"""

		results = dict()

		for db in self._databaseList():
			x = time.time()
			path = self.databasePath(db.name)
			match = seriesyear.match(db.name)
			if match is None:
				continue # non standard series, skip for now
			series = match.group(1)
			year = int(match.group(2))

			engine = create_engine('sqlite:///%s' % path)
			self.session.bind = engine
			settings = Settings()
			settings.load(self.session)

			for row in self.session.execute("select d.firstname, d.lastname, count(distinct r.eventid) from runs as r, cars as c, drivers as d where r.carid=c.id and c.driverid=d.id and r.eventid < 100 group by d.id"):
				first = row[0].lower().strip()
				last = row[1].lower().strip()
				count = int(row[2])

				if (last, first) not in results:
					results[last,first] = Result(last, first)

				result = results[last,first]
				result.series.add(series.lower())
				result.years.add(year)
				result.isttotal += count

				if series.lower() in pcseries:
					if count > settings.useevents:
						result.pcchamp += 1
						if pcchamp:
							result.pcqualify = False
					if year >= pcsinceyear:
						result.pcevents[year] += count
						if result.pcevents[year] > pcyearmax:
							result.pcqualify = False


		for result in results.itervalues():
			result.istavg = result.isttotal * 1.0 / len(result.years)
			result.istqualify = result.isttotal < isttotal and result.istavg < istavg
	
			# collapse items into strings
			result.years = ' '.join(map(str, sorted(result.years)))
			result.series = ','.join(sorted(result.series))
			result.pcevents = ','.join(map(str, result.pcevents.values()))
			result.istavg = "%0.1lf" % (result.istavg)

		return results


