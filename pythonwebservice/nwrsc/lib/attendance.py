
import re
import os
import logging
import re
import glob
import sqlite3

from nwrsc.model import Session, Settings
from sqlalchemy import create_engine

log = logging.getLogger(__name__)

# All data is a list of iterables (series, year, first, last, attended, champ)

def loadAttendanceFromDB(dbpath):
	ret = list()
	match = re.search('/(\w+?)(\d+)\.', dbpath)
	if match is None:
		return ret
	series = match.group(1)
	year = int(match.group(2))

	session = Session()
	session.bind = create_engine('sqlite:///%s' % dbpath)
	settings = Settings()
	settings.load(session)

	for row in session.execute("select d.firstname, d.lastname, count(distinct r.eventid) from runs as r, cars as c, drivers as d where r.carid=c.id and c.driverid=d.id and r.eventid < 100 group by d.id"):
		count = int(row[2])
		ret.append((series.lower(), year, row[0].lower().strip(), row[1].lower().strip(), count, count > settings.useevents))

	return ret


def loadAttendance(directory):
	ret = list()
	for db in glob.glob(os.path.join(directory, '*.attendance')):
		try:
			conn = sqlite3.connect(db)
			for row in conn.execute('select series, year, first, last, attended, champ from attendance'):
				ret.append(row[:])
			conn.close()
		except Exception, e:
			log.warning("unable to load from attendance %s: %s" % (db, e))
	return ret


def writeAttendance(dbpath, data):
	try:
		conn = sqlite3.connect(dbpath)
		conn.execute("create table attendance(series STRING, year INT, first STRING, last STRING, attended INT, champ BOOLEAN)")
		conn.executemany("insert into attendance(series, year, first, last, attended, champ) values(?,?,?,?,?,?)", data)
		conn.commit()
		conn.close()
	except Exception, e:
		raise Exception("unable to write attendance: %s" % (e))

	
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

