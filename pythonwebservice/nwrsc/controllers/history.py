
from pylons.templating import render_mako
from pylons import request, config, tmpl_context as c

from nwrsc.model import *
from nwrsc.lib.attendance import loadAttendance, loadAttendanceFromDB
from nwrsc.controllers.lib.base import BaseController
from sqlalchemy import create_engine

from collections import defaultdict
import operator
import string
import re
import os
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


class HistoryController(BaseController):

	def __before__(self):
		c.stylesheets = ['/css/history.css']
		c.recentyear = datetime.date.today().year - 2

	def index(self):
		return render_mako('/history/main.mako')

	
	def attendance(self):
		""" eventually, this will be the only thing present, no processing server side, let use configure what they want to view """
		data = loadAttendance(config['archivedir'])
		for db in self._databaseList(archived=False):
			data.extend(loadAttendanceFromDB(os.path.join(config['seriesdir'], db.name+'.db')))
		return self.csv("attendance", ('series', 'year', 'first', 'last', 'attended', 'champ'), data)


	def report(self):
		""" 
			POST:
			isttotal, istavg
			pcseries, pcyearmax, pcsinceyear, pcchamp
			exclusionsonly, selection
		"""

		limits = {
			'isttotal': int(request.POST['isttotal']),
			'istavg': int(request.POST['istavg']),
			'pcseries': map(string.lower, map(string.strip, request.POST['pcseries'].split(','))),
			'pcyearmax': int(request.POST['pcyearmax']),
			'pcsinceyear': int(request.POST['pcsinceyear']),
			'pcchamp': 'pcchamp' in request.POST
		}

		self._getData(limits)
		c.results = self.results.itervalues()
		if 'exclusionsonly' in request.POST:
			c.results = [x for x in c.results if not (x.istqualify and x.pcqualify)]
		c.results = sorted(c.results, key=operator.attrgetter('last', 'first'))

		c.names = ['last', 'first']
		for key in ('years', 'series', 'isttotal', 'istavg', 'pcchamp', 'pcevents', 'istqualify', 'pcqualify'):
			if 'col'+key in request.POST:
				c.names.append(key)

		# format some items into strings
		for result in c.results:
			result.years = ' '.join(map(str, sorted(result.years)))
			result.series = ','.join(sorted(result.series))
			result.pcevents = ','.join(map(str, result.pcevents.values()))
			result.istavg = "%0.1lf" % (result.istavg)
			result.istqualify = result.istqualify and "yes" or "NO"
			result.pcqualify = result.pcqualify and "yes" or "NO"

		selection = request.POST['selection']
		if selection == 'CSV':
			return self.csv("history", c.names, c.results)
		
		return render_mako('/history/tableoutput.mako')


	def _getData(self, limits):
		"""
			punchcard
				championship in NWR/PRO disqualifies
				> 4 events in NWR/PRO in last 4 years disqualifies

			IST
				total events in all series
				average where #year == active years
		"""
		self.results = dict()

		for db in self._databaseList():
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
				count = int(row[2])
				self._processData(limits, row[0].lower().strip(), row[1].lower().strip(), series, year, count, count > settings.useevents)

		# pull in the screen scraped history
		import sqlite3
		historyfile = os.path.join(config.get('archivedir', 'missing'), 'old.history')
		try:
			conn = sqlite3.connect(historyfile)
			for row in conn.execute('select first, last, series, year, attended, champ from history'):
				self._processData(limits, row[0].lower().strip(), row[1].lower().strip(), row[2].strip(), int(row[3]), int(row[4]), bool(int(row[5])))
			conn.close()
		except Exception, e:
			raise Exception("unable to process history %s: %s" % (historyfile, e))

		for result in self.results.itervalues():
			result.istavg = result.isttotal * 1.0 / len(result.years)
			result.istqualify = result.isttotal < limits['isttotal'] and result.istavg < limits['istavg']



	def _processData(self, limits, first, last, series, year, count, champ):
			if (last, first) not in self.results:
				self.results[last,first] = Result(last, first)

			result = self.results[last,first]
			result.series.add(series.lower())
			result.years.add(year)
			result.isttotal += count

			if series.lower() in limits['pcseries']:
				if champ:
					result.pcchamp += 1
					if limits['pcchamp']:
						result.pcqualify = False
				if year >= limits['pcsinceyear']:
					result.pcevents[year] += count
					if result.pcevents[year] > limits['pcyearmax']:
						result.pcqualify = False

		



