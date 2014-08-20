#!/usr/bin/env python

import sys
import os
import csv
import datetime
import re
import sqlite3
from nwrsc.model import *
from sqlalchemy import create_engine

class Tables(object):
	def __init__(self):
		self.drivers = []
		self.passwords = []
		self.roles = []	
		self.series = []
		self.data = dict()
		self.events = []
		self.cars = []
		self.classlist = []
		self.indexlist = []
		self.payments = []
		self.registered = []
		self.rungroups = []
		self.runorder = []
		self.runs = []
		self.results = []
		self.segments = dict()
		self.challenges = []
		self.challengeroundentries = []
		self.challengeruns = []
		self.history = dict()

		self.seriesid = 0
		self.eventid = 0
		self.driverid = 0
		self.carid = 0
		self.challengeid = 0
		self.roundid = 0

	def seriesId(self):
		self.seriesid +=1
		return self.seriesid
	def eventId(self):
		self.eventid +=1
		return self.eventid
	def driverId(self):
		self.driverid += 1
		return self.driverid
	def carId(self):
		self.carid += 1
		return self.carid
	def challengeId(self):
		self.challengeid += 1
		return self.challengeid
	def roundId(self):
		self.roundid += 1
		return self.roundid

defaulttemplate = """
<div id="seriesimage"><img src="seriesimage.jpg" /></div>
<div id="seriestitle">${series.name}</div>
<div id="eventtitle">${event.name} - ${event.date}</div>
<div id="hosttitle">Hosted By: <span class="host">${event.host}</span></div>
<div id="entrantcount">(${entrantcount} Entrants)</div>
<div class="info">For Indexed Classes Times in Brackets [] Is Raw Time</div> 
<hr />
<div id="classlinks">
<#list active as code>
<a href="#${code}">${code}</a>
</#list>
</div>

<#include "results_basic/classtables.ftl">

<br/><br/>

<#include "results_basic/toptimes.ftl">

<!--#include virtual="/wresfooter.html" -->

"""

resultscss = """

/** post header **/
#seriestitle { 
	font-size: 1.5em;
	font-weight: bold;
}

#eventtitle {
	font-size: 1.5em;
}

#hosttitle {
}

#host {
}

#entrantcount {
}

div.info {
	font-size: 0.9em;
}

#classlinks {
}


/** basic headers **/

div, h1, h2, h3, h4 { text-align:center; }
a img { border: none; }


/** Center tables with collapsed borders **/

table {
	margin: 0px auto;
	border-collapse: collapse;
}

table.centercols { 
	margin-top: 20px;
}

table.classresults, table.toptimes, table.auditreport, table.champ {
	font-size:0.9em; 
}

table.toptimes {
	display: inline;
}


/** Various TD types and their span internals **/

td {
	text-align:left; 
	padding: 5px 3px 3px;
}

td.run {
	text-align:center;
}

td.drop {
	color: #BBB;
	text-decoration: line-through;
}

td.carnum, td.points {
	text-align:right;
}

td.bestraw .net {
	text-decoration: underline;
}

td.bestnet { 
	font-weight: bold; 
}

td.bestnet .net {
	text-decoration: underline;
}

td.attend {
	text-align: center;
}

tr.entrantrow td {
	border-top: 1px solid #999;
}

span { 
	padding: 2px; 
}

span.net {
	display: block;
}

span.raw {
	display: block; 
	font-size: 0.9em;
}

span.reaction {
	display: block;
	font-size: 0.8em;
}

/** toptimes columns */

td.numcol { 
	border-left: 1px solid #BBB; 
	text-align: right; 
}

tr.brgrey { 
	border-right: 1px solid #BBB 
}

/** Table header entries **/

th { 
	background-color: #dcdcdc; 
	text-align: center;
}

th.classhead { 
	text-align: left; 
	background-color: #4682B4; 
	color: white; 
	border-top: 1px solid black;
	padding-left: 6px;
}

table.champ th {
	padding: 0px 5px;
}

"""


gTables = Tables()
gMatchDrivers = dict()

class MyObj(object):
	def __init__(self, **kwargs):
		for k,v in kwargs.iteritems():
			setattr(self, k, v)

def objs2lists(objs, fields):
	ret = list()
	for o in objs:
		x = list()
		for f in fields:
			if hasattr(o,f):
				x.append(getattr(o,f))
			else:
				x.append("")
		ret.append(x)
	return ret


class MyDialect(csv.excel):
	quoting = csv.QUOTE_NONNUMERIC

def writecsv(dirp, name, objs, fields, newfields = None):
	csvfile = open(os.path.join(dirp, name), 'w')
	w = csv.writer(csvfile, dialect=MyDialect())
	w.writerow(fields)
	w.writerows(objs2lists(objs, fields))
	csvfile.close()


ONLYW = re.compile("^\w+$")
MINDIG = re.compile("\d{4}")
nummatch = re.compile('^(\d{6}_\d)|(\d{6})$')

def convertattendance(sourcefile):
	conn = sqlite3.connect(sourcefile)
	conn.row_factory = sqlite3.Row
	cur = conn.cursor()
	for row in cur.execute("select * from attendance"):
		f = row['first'].strip()
		l = row['last'].strip()
		driverid = -1
		for driver in gMatchDrivers.values():
			if driver.firstname.lower() == f.lower() and driver.lastname.lower() == l.lower():
				driverid = driver.id
				break

		if driverid < 0:
			driverid = gTables.driverId()
			d = Driver()
			d.driverid = driverid
			d.firstname = f
			d.lastname = l
			gTables.drivers.append(d)
			gMatchDrivers[driverid] = d

		# insert (driverid, prefix, year, attended, champ)
		obj = MyObj(driverid=driverid, prefix=row['series'], year=row['year'], attended=row['attended'], champ=row['champ'])
		gTables.history[(obj.driverid, obj.prefix, obj.year)] = obj


def convert(sourcefile):
	global gTables, gMatchDrivers

	remapdriver = dict()
	remapcar = dict()
	remapevent = dict()
	remapchallenge = dict()

	print "Read in %s" % sourcefile
	metadata.bind = create_engine('sqlite:///%s' % sourcefile)
	session = Session()
	session.bind = metadata.bind

	# Extra cleaning
	if not '2014' in sourcefile:
		session.execute("delete from cars where id not in (select distinct carid from runs)")
		session.execute("delete from drivers where id not in (select distinct driverid from cars)")
		session.execute("delete from cars where driverid not in (select distinct id from drivers)")
		session.execute("delete from registered where carid not in (select distinct id from cars)")
		session.execute("delete from runorder where carid not in (select distinct id from cars)")
		session.execute("delete from challengerounds where challengeid not in (select distinct id from challenges)")

	# querys to get all the data
	drivers = session.query(Driver).all()
	cars = session.query(Car).all()
	settings = session.query(Setting).all()
	data = session.query(Data).all()
	events = session.query(Event).all()
	classes = session.query(Class).all()
	indexes = session.query(Index).all()
	payments = session.query(Payment).all()
	registered = session.query(Registration).all()
	rungroups = session.query(RunGroup).all()
	runorder = session.query(RunOrder).all()
	runs = session.query(Run).all()
	results = session.query(EventResult).all()
	challenges = session.query(Challenge).all()
	challengerounds = session.query(ChallengeRound).all()
	session.close()

	# Create Series (rename some settings, create active)
	seriesid = gTables.seriesId()
	series = MyObj(seriesid=seriesid, prefix=os.path.basename(sourcefile)[:-7], year=sourcefile[-7:-3])
	for s in settings:
		setattr(series, s.name, s.val)
	series.name = series.seriesname
	series.usepositionpoints = series.usepospoints
	series.positionpoints = series.pospointlist
	series.champuseevents = series.useevents
	series.champminevents = series.minevents
	series.posttemplate = defaulttemplate
	series.resultscss = resultscss
	series.posttoptimes = True
	series.postindextimes = True
	series.active = 0
	series.parentid = -1
	series.conepen = 2
	series.gatepen = 10
	series.indexgroup = "%s%s PAX" % (series.prefix, series.year)
	series.stylegroup = "basic"
	gTables.series.append(series)


	#DRIVERS, add to global list and remap ids as necessary
	for d in drivers:
		d.phone = d.phone and d.phone[:16]
		d.brag = d.brag and d.brag[:128]
		d.sponsor = d.sponsor and d.sponsor[:128]

		key = d.firstname.strip().lower() + d.lastname.strip().lower() + d.email.strip().lower()
		if key in gMatchDrivers:
			remapdriver[d.id] = gMatchDrivers[key].id
			d.id = gMatchDrivers[key].id
		else:
			remapdriver[d.id] = gTables.driverId()
			d.id = remapdriver[d.id]
			d.emergencyname = ""
			d.emergencycontact = ""
			gTables.drivers.append(d)
			gMatchDrivers[key] = d

		d.driverid = d.id
		d.username = d.driverid
		d.password = d.username
		d.scca = ""
		d.other = ""
		d.other2 = ""
		if d.membership:
			if nummatch.match(d.membership):
				d.scca = d.membership
			elif ONLYW.search(d.membership):
				d.other = d.membership

	#CARS (all the same fields, need to map carid, driverid and seriesid)
	for c in cars:
		try:
			remapcar[c.id] = gTables.carId()
			c.seriesid = seriesid
			c.carid = remapcar[c.id]
			c.driverid = remapdriver[c.driverid]
			c.tireindexed = c.tireindexed or False
			c.number = c.number and int(c.number) or 999
			gTables.cars.append(c)
		except:
			print "invalid driverid %s, skipping" % c.driverid

	#DATA (all the same fields need to map seriesid)
	for d in data:
		d.data = str(d.data).encode("hex")
		d.type = "unknown"
		gTables.data[d.name] = d

	#EVENTS (all the same fields, change segments, perlimit to personallimit, totlimit to totalimit, need to map eventid and seriesid)
	for e in events:
		remapevent[e.id] = gTables.eventId()
		e.id = remapevent[e.id]
		e.eventid = e.id
		e.cost = e.cost and int(e.cost) or 0
		e.countedruns = e.countedruns and int(e.countedruns) or 0
		e.seriesid = seriesid
		if e.segments is not None and e.segments.strip() != "":
			e.segments = 2
		else:
			e.segments = 0
		e.personallimit = e.perlimit and int(e.perlimit) or 0
		e.totallimit = e.totlimit and int(e.totlimit) or 0
		gTables.events.append(e)

	#CLASSLIST (map seriesid)
	for c in classes:
		c.description = c.descrip
		c.seriesid = seriesid
		c.numorder = c.numorder and int(c.numorder) or 0
		c.usecarflag = c.usecarflag or False
		gTables.classlist.append(c)

	#INDEXLIST (put into its own index group)
	for i in indexes:
		i.indexgroup = "%s%s PAX" % (series.prefix, series.year)
		i.description = i.descrip
		gTables.indexlist.append(i)

	#PAYMENTS (just leave blank, don't really need old payment data)

	#REGISTERED (map eventid, carid)
	for r in registered:
		if r.eventid > 1000 or not r.carid:  # weird registered for a challenge junk from the old pros
			continue
		try:
			r.carid = remapcar[r.carid]
			r.eventid = remapevent[r.eventid]
			r.paid = r.paid or False
			r.mod = "2000-01-01 00:00:00"
			gTables.registered.append(r)
		except:
			print "missing carid %s for registered, skipping" % r.carid

	#RUNGROUPS (map eventid, change gorder to position)
	for r in rungroups:
		if not r.classcode: 
			continue
		r.eventid = remapevent[r.eventid]
		r.position = r.gorder
		gTables.rungroups.append(r)

	#RUNORDER (map eventid, carid)
	for r in runorder:
		try:
			r.eventid = remapevent[r.eventid]
			r.carid = remapcar[r.carid]
			gTables.runorder.append(r)
		except:
			print "missing carid %s for runorder, skipping" % r.carid

	#RUNS (map eventid, carid)
	challengeruns = list()
	for r in runs:
		if not r.carid or not r.eventid or not r.course or not r.run:
			continue

		r.mod = "2000-01-01 00:00:00"
		r.decibel = 0
		r.reaction = r.reaction and float(r.reaction) or 0.0
		r.sixty = r.sixty and float(r.sixty) or 0.0
		if r.carid in remapcar:
			r.carid = remapcar[r.carid]
		else:
			print "unknown car id in runs (%s)" % r.carid
			continue

		if r.eventid > 0x0FFFF:
			r.roundentryid = -1
			challengeruns.append(r)
		else:
			r.eventid = remapevent[r.eventid]
			gTables.runs.append(r)

		for ii in range(1,6):
			seg = getattr(r, 'seg%d'%ii)
			if seg > 0:
				sg = MyObj(**r.__dict__)
				sg.segment = ii
				sg.raw = seg
				k = (sg.eventid, sg.carid, sg.course, sg.run, sg.segment)
				if k in gTables.segments:
					print "duplicate segment key: %s, would replace %.3f with %.3f" % (k, gTables.segments[k].raw, seg)
				else:
					gTables.segments[k] = sg


	#EVENTRESULTS (remap eventid, carid)
	for r in results:
		if r.carid in remapcar:
			r.carid = remapcar[r.carid]
		else:
			print "unknown car id in results (%s)" % r.carid
			continue
		r.eventid = remapevent[r.eventid]
		r.positionpoints = r.pospoints
		gTables.results.append(r)

	#CHALLENGES (remap challengeid, eventid)
	for c in challenges:
		remapchallenge[c.id] = gTables.challengeId()
		c.id = remapchallenge[c.id]
		c.challengeid = c.id
		c.eventid = remapevent[c.eventid]
		gTables.challenges.append(c)

	def rewriteruns(challengeid, round, carid, entryid):
		for run in challengeruns:
			run.side = run.course
			if remapchallenge[run.challengeid] == challengeid and run.carid == carid and run.round == round:
				run.roundentryid = entryid

	#CHALLENGEROUNDS (remap roundid, challengeid, carid)
	for r in challengerounds:
		if r.round == 0:  # convert round into a round location
			upper = 1
			lower = 101
		elif r.round == 99:
			upper = 103
			lower = 102
		else:
			lower = r.round * 2
			upper = lower + 1

		r.challengeid = remapchallenge[r.challengeid]
		r.car1id = r.car1id > 0 and remapcar[r.car1id] or -1
		r.car2id = r.car2id > 0 and remapcar[r.car2id] or -1
		r.car1dial = r.car1dial and float(r.car1dial) or 0.0
		r.car2dial = r.car2dial and float(r.car2dial) or 0.0
		r.car1newdial = r.car1newdial and float(r.car1newdial) or 0.0
		r.car2newdial = r.car2newdial and float(r.car2newdial) or 0.0
		r.car1result = r.car1result and float(r.car1result) or 0.0
		r.car2result = r.car2result and float(r.car2result) or 0.0

		entryid = gTables.roundId()
		gTables.challengeroundentries.append(MyObj(roundentryid=entryid, challengeid=r.challengeid, location=upper, carid=r.car1id, indial=r.car1dial, result=r.car1result, outdial=r.car1newdial)) 
		rewriteruns(r.challengeid, r.round, r.car1id, entryid)

		entryid = gTables.roundId()
		gTables.challengeroundentries.append(MyObj(roundentryid=entryid, challengeid=r.challengeid, location=lower, carid=r.car2id, indial=r.car2dial, result=r.car2result, outdial=r.car2newdial)) 
		rewriteruns(r.challengeid, r.round, r.car2id, entryid)


	#CHALLENGRUNS
	gTables.challengeruns.extend(challengeruns)
	

if len(sys.argv) < 3:
	print "\nUsage: %s <sourcedirectory> <outputdirectory>\n" % sys.argv[0]
	print "\tExtracts the data from all databases in <sourcedirectory> into <outputdirectory>\n\n"
	sys.exit(0)

sourcedir = sys.argv[1]
destdir = sys.argv[2]

for dbfile in sorted(os.listdir(sourcedir), key=lambda x: x[-7:-3]):
	if dbfile.endswith('.db'):
		convert(os.path.join(sourcedir,dbfile))

print "processing attendance files"
#for otherfile in os.listdir(sourcedir):
#	if otherfile.endswith('.attendance'):
#		convertattendance(os.path.join(sourcedir,otherfile))

print "generating csv"

writecsv(destdir, 'series.csv', gTables.series, ['seriesid', 'parentid', 'prefix', 'year', 'name', 'password', 'locked', 'active', 'superuniquenumbers', 'indexafterpenalties', 'usepositionpoints', 'indexgroup', 'stylegroup', 'positionpoints', 'sponsorlink', 'champuseevents', 'champminevents', 'largestcarnumber', 'conepen', 'gatepen', 'posttemplate', 'resultscss', 'posttoptimes', 'postindextimes'])

writecsv(destdir, 'drivers.csv', gTables.drivers, ['driverid', 'username', 'password', 'firstname', 'lastname', 'alias', 'email', 'address', 'city', 'state', 'zip', 'phone', 'emergencyname', 'emergencycontact', 'sponsor', 'brag', 'scca', 'other', 'other2'])
writecsv(destdir, 'cars.csv', gTables.cars, ['carid', 'seriesid', 'driverid', 'year', 'make', 'model', 'color', 'number', 'classcode', 'indexcode', 'tireindexed'])
writecsv(destdir, 'events.csv', gTables.events,  ['eventid', 'seriesid', 'courses', 'runs', 'countedruns', 'segments', 'personallimit', 'totallimit', 'cost', 'name', 'location', 'sponsor', 'host', 'designer', 'paypal', 'notes', 'date', 'regopened', 'regclosed', 'ispro', 'practice', 'doublespecial' ])

writecsv(destdir, 'classlist.csv', gTables.classlist, ['seriesid', 'code', 'description', 'carindexed', 'classindex', 'classmultiplier', 'eventtrophy', 'champtrophy', 'numorder', 'countedruns', 'usecarflag', 'caridxrestrict'])
writecsv(destdir, 'indexlist.csv', gTables.indexlist, ['indexgroup', 'code', 'description', 'value'])
writecsv(destdir, 'registered.csv', gTables.registered, ['eventid', 'carid', 'paid', 'mod'])
writecsv(destdir, 'rungroups.csv', gTables.rungroups, ['eventid', 'classcode', 'rungroup', 'position'])
writecsv(destdir, 'runorder.csv', gTables.runorder, ['eventid', 'course', 'carid', 'rungroup', 'row'])
writecsv(destdir, 'runs.csv', gTables.runs, ['eventid', 'carid', 'course', 'run', 'reaction', 'sixty', 'raw', 'cones', 'gates', 'status', 'decibel', 'mod'])
writecsv(destdir, 'champpoints.csv', gTables.results, ['eventid', 'classcode', 'carid', 'position', 'positionpoints', 'diff', 'diffpoints'])
writecsv(destdir, 'segments.csv', gTables.segments.values(), ['eventid', 'carid', 'course', 'run', 'segment', 'raw'])
writecsv(destdir, 'challenges.csv', gTables.challenges, ['challengeid', 'eventid', 'name', 'depth'])
writecsv(destdir, 'challengeroundentries.csv', gTables.challengeroundentries, ['roundentryid', 'challengeid', 'location', 'carid', 'indial', 'result', 'outdial'])
writecsv(destdir, 'challengeruns.csv', gTables.challengeruns, ['roundentryid', 'side', 'reaction', 'sixty', 'raw', 'cones', 'gates', 'status'])


print "done."

