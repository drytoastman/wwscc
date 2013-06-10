#!/usr/bin/env python

import sys
import os
import csv
import datetime
import re
from nwrsc.model import *
from sqlalchemy import create_engine

globaldrivers = list()
globalmemberships = list()
globaldriverid = 1

class Membership(object):
	def __init__(self, driverid=0, club="", membership=""):
		self.driverid = driverid
		self.club = club
		self.membership = membership

def objs2lists(objs, fields):
	ret = list()
	for o in objs:
		ret.append([getattr(o, f) for f in fields])
	return ret

def writecsv(dirp, name, objs, fields, newfields = None):
	with open(os.path.join(dirp, name), 'w') as csvfile:
		w = csv.writer(csvfile)
		w.writerow(fields)
		w.writerows(objs2lists(objs, fields))


ONLYW = re.compile("^\w+$")
MINDIG = re.compile("\d{4}")

def convert(sourcefile, destdir):
	global globaldriverid, globaldrivers, globalmemberships

	try:
		os.makedirs(destdir)
	except OSError:
		pass

	remaplocal = dict()

	print "Convert %s to %s" % (sourcefile, destdir)
	metadata.bind = create_engine('sqlite:///%s' % sourcefile)
	session = Session()
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
	challenges = session.query(Challenge).all()
	challengerounds = session.query(ChallengeRound).all()
	session.close()

	#DRIVERS, add to global list and remap ids as necessary
	for d in drivers:
		remaplocal[d.id] = globaldriverid
		d.id = globaldriverid
		d.emergencyname = ""
		d.emergencycontact = ""
		globaldrivers.append(d)
		if d.membership and ONLYW.search(d.membership) and MINDIG.search(d.membership):
			globalmemberships.append(Membership(d.id, 'SCCA', d.membership))
		globaldriverid += 1

	#CARS (all the same fields, need to remap driverid though)
	validcars = list()
	for c in cars:
		try:
			c.driverid = remaplocal[c.driverid]
			validcars.append(c)
		except:
			print "invalid driverid %s, skipping" % c.driverid
	writecsv(destdir, 'cars.csv', validcars, ['id', 'year', 'make', 'model', 'color', 'number', 'driverid', 'classcode', 'indexcode', 'tireindexed'])

	#CONFIG (all the same fields as settings)
	writecsv(destdir, 'config.csv', settings, ['name', 'val'])

	#DATA (all the same fields)
	writecsv(destdir, 'data.csv', data,  ['name', 'mime', 'mod', 'data'])

	#EVENTS (all the same fields, change segments to segmentsetup, perlimit to personallimit, totlimit to totalimit, new blank field infopage)
	for e in events:
		e.segmentsetup = e.segments
		e.personallimit = e.perlimit
		e.totallimit = e.totlimit
		e.infopage = ""
	writecsv(destdir, 'events.csv', events,  ['id', 'name', 'date', 'location', 'sponsor', 'host', 'chair', 'designer', 'ispro', 'courses', 'runs', 'countedruns', 'conepen', 'gatepen', 'regopened', 'regclosed', 'paypal', 'snail', 'cost', 'practice', 'doublespecial', 'segmentsetup', 'personallimit', 'totallimit', 'infopage'])

	#CLASSLIST
	for c in classes:
		c.description = c.descrip
	writecsv(destdir, 'classlist.csv', classes, ['code', 'description', 'carindexed', 'classindex', 'classmultiplier', 'eventtrophy', 'champtrophy', 'numorder', 'countedruns', 'usecarflag', 'caridxrestrict'])

	#INDEXLIST
	for i in indexes:
		i.description = i.descrip
	writecsv(destdir, 'indexlist.csv', indexes, ['code', 'description', 'value'])

	#PAYMENTS (just leave blank, don't really need old payment data)

	#REGISTERED
	writecsv(destdir, 'registered.csv', registered, ['eventid', 'carid', 'paid'])

	#RUNGROUPS
	for r in rungroups:
		r.position = r.gorder
	writecsv(destdir, 'rungroups.csv', rungroups, ['eventid', 'classcode', 'rungroup', 'position'])

	#RUNORDER
	writecsv(destdir, 'runorder.csv', runorder, ['eventid', 'course', 'rungroup', 'carid', 'row'])

	#RUNS
	regularruns = list()
	challengeruns = list()
	for r in runs:
		if r.eventid > 100:
			challengeruns.append(r)
		else:
			regularruns.append(r)
	writecsv(destdir, 'runs.csv', regularruns, ['eventid', 'carid', 'course', 'run', 'reaction', 'sixty', 'seg1', 'seg2', 'seg3', 'seg4', 'seg5', 'raw', 'cones', 'gates', 'status'])

	#CHALLENGES
	writecsv(destdir, 'challenges.csv', challenges, ['id', 'eventid', 'name', 'depth'])
	
	#CHALLENGEROUNDS
	entries = list()
	for r in challengerounds:
		r.topcar = 'what'
		r.bottomcar = 'who'
	writecsv(destdir, 'challengerounds.csv', challengerounds, ['id', 'challengeid', 'round', 'swappedstart', 'topcar', 'bottomcar'])

	writecsv(destdir, 'challengeroundentries.csv', entries, ['id', 'carid', 'indial', 'result', 'outdial'])

	#CHALLENGRUNS
	for r in challengeruns:
		r.roundentryid = 'X'
		r.side = r.course
	writecsv(destdir, 'challengeruns.csv', challengeruns, ['roundentryid', 'side', 'reaction', 'sixty', 'raw', 'cones', 'gates', 'status'])
	

if len(sys.argv) < 3:
	print "\nUsage: %s <sourcedirectory> <outputdirectory>\n" % sys.argv[0]
	print "\tExtracts the data from all databases in <sourcedirectory> into <outputdirectory>\n\n"
	sys.exit(0)

sourcedir = sys.argv[1]
destdir = sys.argv[2]

for dbfile in os.listdir(sourcedir):
	if not dbfile.endswith('.db'):
		continue
	outdir = os.path.join(destdir, os.path.basename(dbfile)[:-3])
	convert(os.path.join(sourcedir,dbfile), outdir)

writecsv('.', 'drivers.csv', globaldrivers, ['id', 'firstname', 'lastname', 'alias', 'email', 'address', 'city', 'state', 'zip', 'phone', 'brag', 'sponsor', 'emergencyname', 'emergencycontact'])
writecsv('.', 'memberships.csv', globalmemberships, ['driverid', 'club', 'membership'])

