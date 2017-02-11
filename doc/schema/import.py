#!/usr/bin/env python

import sqlite3
import psycopg2
import psycopg2.extras
import json
import uuid
import re
import sys


class MyObj(object):
	def __init__(self, **kwargs):
		for k,v in kwargs.iteritems():
			setattr(self, k, v)

class AttrWrapper(object):
	def __init__(self, tup, headers):
		for k,v in zip(headers, tup):
			setattr(self, k, v)


ONLYW = re.compile("^\w+$")
MINDIG = re.compile("\d{4}")
nummatch = re.compile('^(\d{6}_\d)|(\d{6})$')

def convert(sourcefile):
	global gTables, gMatchDrivers

	remapdriver = dict()
	remapcar = dict()
	remapevent = dict()
	remapchallenge = dict()

	old = sqlite3.connect(sourcefile)
	old.row_factory = sqlite3.Row

	new = psycopg2.connect("dbname='scorekeeper' user='ww2017' host='127.0.0.1' password='ww2017'")
	cur = new.cursor()
	psycopg2.extras.register_uuid()

	"""
	#DRIVERS, add to global list and remap ids as necessary
	for r in old.execute('select * from drivers'):
		d = AttrWrapper(r, r.keys())

		newd = dict()
		newd['driverid']   = uuid.uuid1()
		newd['firstname']  = d.firstname.strip()
		newd['lastname']   = d.lastname.strip()
		newd['email']      = d.email.strip()
		newd['password']   = d.email.strip()
		newd['membership'] = d.membership and d.membership.strip() or ""
		newd['attr']       = dict()
		for a in ('alias', 'address', 'city', 'state', 'zip', 'phone', 'brag', 'sponsor', 'notes'):
			if hasattr(d, a) and getattr(d, a) is not None:
				newd['attr'][a] = getattr(d, a).strip()

		cur.execute("insert into drivers values (%s, %s, %s, %s, %s, %s, %s, now())", 
			(newd['driverid'], newd['firstname'], newd['lastname'], newd['email'], newd['password'], newd['membership'], json.dumps(newd['attr'])))
		remapdriver[d.id] = newd['driverid']


	#INDEXLIST (put into its own index group)
	cur.execute("insert into indexlist values ('', 'No Index', 1.000, now())")
	for r in old.execute("select * from indexlist"):
		i = AttrWrapper(r, r.keys())
		cur.execute("insert into indexlist values (%s, %s, %s, now())", 	
					(i.code, i.descrip, i.value))

	#CLASSLIST (map seriesid)
	for r in old.execute("select * from classlist"):
		c = AttrWrapper(r, r.keys())
		c.numorder = c.numorder and int(c.numorder) or 0
		c.usecarflag = c.usecarflag and True or False
		c.carindexed = c.carindexed and True or False
		c.eventtrophy = c.eventtrophy and True or False
		c.champtrophy = c.champtrophy and True or False
		cur.execute("insert into classlist values (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, now())", 
					(c.code, c.descrip, c.classindex, c.caridxrestrict, c.classmultiplier, c.carindexed, c.usecarflag, c.eventtrophy, c.champtrophy, c.numorder, c.countedruns))


	#CARS (all the same fields, need to map carid, driverid and seriesid)
	for r in old.execute("select * from cars"):
		c = AttrWrapper(r, r.keys())
		if c.driverid < 0:
			continue
		newc = dict()
		newc['carid']     = uuid.uuid1()
		newc['driverid']  = remapdriver[c.driverid]
		newc['classcode'] = c.classcode
		newc['indexcode'] = c.indexcode or ''
		newc['number']    = c.number or 999
		newc['attr']      = dict()
		for a in ('year', 'make', 'model', 'color', 'tireindexed'):
			if hasattr(c, a) and getattr(c, a) is not None:
				newc['attr'][a] = getattr(c, a)

		cur.execute("insert into cars values (%s, %s, %s, %s, %s, %s, now())", 
			(newc['carid'], newc['driverid'], newc['classcode'], newc['indexcode'], newc['number'], json.dumps(newc['attr'])))
		remapcar[c.id] = newc['carid']

		
	#EVENTS (all the same fields, change segments, perlimit to personallimit, totlimit to totalimit, need to map eventid and seriesid)
	for r in old.execute("select * from events"):
		e = AttrWrapper(r, r.keys())

		newe = dict()
		newe['eventid']     = uuid.uuid1()
		newe['name']        = e.name
		newe['date']        = e.date
		newe['regopened']   = e.regopened
		newe['regclosed']   = e.regclosed
		newe['courses']     = e.courses
		newe['runs']        = e.runs
		newe['countedruns'] = e.countedruns
		newe['perlimit']    = e.perlimit
		newe['totlimit']    = e.totlimit
		newe['conepen']     = e.conepen
		newe['gatepen']     = e.gatepen
		newe['ispro']       = e.ispro and True or False
		newe['ispractice']  = e.practice and True or False
		newe['attr']        = dict()

		for a in ('location', 'sponsor', 'host', 'chair', 'designer', 'segments', 'paypal', 'snail', 'cost', 'notes', 'doublespecial'):
			if hasattr(e, a) and getattr(e, a) is not None:
				newe['attr'][a] = getattr(e, a)

		cur.execute("insert into events values (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, now())", 
			(newe['eventid'], newe['name'], newe['date'], newe['regopened'], newe['regclosed'], newe['courses'], newe['runs'], newe['countedruns'],
			newe['perlimit'], newe['totlimit'], newe['conepen'], newe['gatepen'], newe['ispro'], newe['ispractice'], json.dumps(newe['attr'])))
		remapevent[e.id] = newe['eventid']

	#REGISTERED (map eventid, carid)
	for r in old.execute("select * from registered"):
		oldr = AttrWrapper(r, r.keys())
		cur.execute("insert into registered values (%s, %s, %s, now())", (remapevent[oldr.eventid], remapcar[oldr.carid], oldr.paid and True or False))


	#RUNORDER 
	for r in old.execute("select * from runorder"):
		oldr = AttrWrapper(r, r.keys())
		cur.execute("insert into runorder values (%s, %s, %s, %s, %s, now())", (remapevent[oldr.eventid], oldr.course, oldr.rungroup, oldr.row, remapcar[oldr.carid]))


	#RUNS (map eventid, carid)
	for r in old.execute("select * from runs"):
		oldr = AttrWrapper(r, r.keys())
		attr = dict()
		attr['reaction'] = oldr.reaction or 0.0
		attr['sixty'] = oldr.sixty or 0.0
		for ii in range(1,6):
			seg = getattr(oldr, 'seg%d'%ii)
			if seg > 0:
				attr['seg%d'%ii] = seg
		cur.execute("insert into runs values (%s, %s, %s, %s, %s, %s, %s, %s, %s, now())",
			(remapevent[oldr.eventid], remapcar[oldr.carid], oldr.course, oldr.run, oldr.cones, oldr.gates, oldr.raw, oldr.status, json.dumps(attr)))

	"""

	#SETTINGS
	settings = dict()
	for r in old.execute("select name,val from settings"):
		cur.execute("insert into settings values (%s, %s, now())", (r[0], r[1]))

		
	"""
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
	"""

	old.close()
	new.commit()
	new.close()

convert('../../series/ww2016.db')

