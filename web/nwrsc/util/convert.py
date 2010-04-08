#!/usr/bin/env python

# Used to convert older nwrsc database schemas into the new version.
# This script can be retired once everything is stable on the new version

from nwrsc.model import metadata
from sqlalchemy import create_engine
from datetime import datetime
import sys, os

try:
	import sqlite3
except:
	from pysqlite2 import dbapi2 as sqlite3

def formatdate(d):
	try:
		dt = datetime.fromtimestamp(int(d)/1000.0)
		return "%4.4d-%2.2d-%2.2d" % (dt.year, dt.month, dt.day)
	except:
		return "2000-01-01"

def formatdatetime(d):
	try:
		dt = datetime.fromtimestamp(int(d)/1000.0)
		return "%4.4d-%2.2d-%2.2d %2.2d:%2.2d:%2.2d.0" % (dt.year, dt.month, dt.day, dt.hour, dt.minute, dt.second)
	except:
		return "2000-01-01 12:01:01.0"

def formattime(d):
	try:
		dt = datetime.fromtimestamp(int(d)/1000.0)
		return "%2.2d:%2.2d:%2.2d.0" % (dt.hour, dt.minute, dt.second)
	except:
		return "12:01:01.0"


old = sys.argv[1]
new = sys.argv[2]

try:
	os.remove(new)
except OSError, x:
	if x.errno != 2:
		raise

metadata.bind = create_engine('sqlite:///' + new)
metadata.create_all()

conn = sqlite3.connect(':memory:')
conn.row_factory = sqlite3.Row
getcur = conn.cursor()
putcur = conn.cursor()


# Create table
putcur.execute("attach '%s' as old" % old)
putcur.execute("attach '%s' as new" % new)
putcur.execute("insert into new.settings select * from old.settings")
putcur.execute("""insert into new.classlist (code, descrip, carindexed, classmultiplier, eventtrophy, champtrophy, numorder)
                                    select   code, descrip, indexed,    clsindex,        etrophy,     ctrophy,     xorder from old.classes """)
putcur.execute("insert into new.indexlist select * from old.indexes")
putcur.execute("insert into new.events select * from old.events")
putcur.execute("insert into new.drivers select * from old.drivers")
putcur.execute("insert into new.cars select * from old.cars")
putcur.execute("""insert into new.eventresults (eventid, carid, classcode, position, courses, sum, diff, points, ppoints, updated)
					select * from old.eventresults""")

putcur.execute("insert into new.registered select * from old.registered")
putcur.execute("insert into new.runorder (eventid, course, rungroup, carid, row) select eventid,course,rungroup,carid,row from old.runorder")
putcur.execute("insert into new.prevlist (firstname, lastname) select firstname,lastname from old.prevlist")
putcur.execute("insert into new.payments select * from old.payments")
putcur.execute("insert into new.challenges select * from old.challenges")


getcur.execute("select * from new.events")
for e in getcur:
	date = formatdate(e['date'])
	regopened = formatdatetime(e['regopened'])
	regclosed = formatdatetime(e['regclosed'])
	putcur.execute("update new.events set date=?, regopened=?, regclosed=? where id=?", (date, regopened, regclosed, e['id']))

getcur.execute("select * from new.payments")
for p in getcur:
	date = formatdate(p['date'])
	putcur.execute("update new.payments set date=? where txid=?", (date, p['txid']))

getcur.execute("select * from new.eventresults")
for r in getcur:
	updated = formatdatetime(r['updated'])
	putcur.execute("update new.eventresults set updated=? where eventid=? and carid=?", (updated, r['eventid'], r['carid']))


putcur.execute("insert into new.challengerounds " +
	"(id, challengeid, round, car1id, car1dial, car1result, car1newdial, car2id, car2dial, car2result, car2newdial) " + 
	"select id, challengeid, round, car1id, car1dial, car1result, car1newdial, car2id, car2dial, car2result, car2newdial from old.challengerounds")

insrun = """insert into new.runs (eventid, carid, course, run, cones, gates, status, reaction, sixty, raw, net, rorder, norder)
		 values (?,?,?,?,?,?,?,?,?,?,?,?,?)"""

getcur.execute("select * from old.challengerounds")
for r in getcur:
	falseid = (r['challengeid']<<16) + r['round']
	
	for runkey, carkey, course in [('car1leftid','car1id',1), ('car1rightid','car1id',2), ('car2leftid','car2id',1), ('car2rightid','car2id',2)]:
		runid = r[runkey]
		putcur.execute("select * from old.challengeruns where id=?", (runid,))
		old = putcur.fetchone()
		if old is not None:
			putcur.execute(insrun, (falseid, r[carkey], course, 1, old['cones'], 0, old['status'],
								 old['reaction'], old['sixty'], old['raw'], old['net'], -1, -1))


getcur.execute("select * from old.runs")
for r in getcur:
	putcur.execute(insrun, (r['eventid'], r['carid'], r['course'], r['run'], r['cones'], r['gates'], r['status'],
							r['reaction'], r['sixty'], r['raw'], r['net'], r['iorder'], r['norder']))


# Save (commit) the changes
getcur.close()
putcur.close()
conn.commit()

