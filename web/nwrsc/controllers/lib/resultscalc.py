"""
	Set of helper functions used by other controllers to complete the functionality from
	Java but more efficient as its all done on one side rather than sending data back
	and forth.
"""

import logging
import datetime
import time
import traceback
from nwrsc.model import *

log = logging.getLogger(__name__)

def UpdateClassResults(session, eventid, classcode, carid):
	cevals = [classcode, eventid]
	updateMap = dict()

	for r in session.sqlmap("GETUPDATED", cevals):
		updateMap[int(r['carid'])] = r['updated']
	if carid > 0:
		updateMap[carid] = datetime.datetime.now()

	# Delete current event results for the same event/class, then query runs table for new results 
	session.sqlmap("DELETECLASSRESULTS", cevals)

	# Now we will loop from 1st to last, calculating points and inserting new event results 
	position = 1
	first = True
	basis = 1.0
	prev = 1.0
	basecnt = 1

	PPOINTS = [int(x) for x in session.query(Setting).get('ppointlist').val.split(',')]
	lists = []
	for r in session.sqlmap("GETCLASSRESULTS", cevals):
		sum = float(r['sum'])
		cnt = int(r['coursecnt'])
		if (first):
			basis = sum
			prev = sum
			basecnt = cnt
			first = False
	
		rvals = [0]*10
		insertcarid = int(r['carid'])
		rvals[0] = eventid
		rvals[1] = insertcarid
		rvals[2] = classcode # classcode doesn't change
		rvals[3] = position
		rvals[4] = cnt
		rvals[5] = sum

		if cnt == basecnt:
			rvals[6] = sum-prev
			rvals[7] = basis/sum*100
			if position <= len(PPOINTS):
				rvals[8] = PPOINTS[position-1]
			else:
				rvals[8] = PPOINTS[-1]
		else:
			#This person ran less courses than the other people
			rvals[6] = 999.999
			rvals[7] = 0.0
			rvals[8] = 0
		
		rvals[9] = updateMap.get(insertcarid, datetime.datetime.now())
		session.sqlmap("INSERTCLASSRESULTS", rvals)
		position += 1
		prev = sum


def RecalculateResults(session, settings):
	yield "<pre>"
	try:
		classdata = ClassData(session)

		for event in session.query(Event).all():
			yield "Event: %d\n" % (event.id)
			session.execute("delete from eventresults where eventid=%d" % event.id) # If admin changes car class, this is needed
	
			for car in session.query(Run.carid).distinct().filter(Run.eventid==event.id):
				codes = session.query(Car.classcode,Car.indexcode).filter(Car.id==car.carid).first()
				if codes is None:
					yield "\tId: %s **** Missing car ****\n" % car.carid
					continue
				val = classdata.getEffectiveIndex(codes.classcode, codes.indexcode)
				counted = min(classdata.getCountedRuns(codes.classcode), event.getCountedRuns())
				istr = classdata.getIndexStr(codes.classcode, codes.indexcode)
				yield "\tId: %s (%s, %s, %s)\n" % (car.carid, val, istr, (counted < 100 and counted or "all"))

				for course in range(1, event.courses+1):
					UpdateRunTotals(session, event, car.carid, course, val, counted, settings.indexafterpenalties)
			session.commit()

			for cls in session.query(Car.classcode).distinct().join(Run).filter(Run.eventid==event.id):
				yield "\tCls: %s\n" % (cls)
				UpdateClassResults(session, event.id, cls.classcode, 0)

		session.commit()
	except Exception, e:
		yield traceback.format_exc()
		session.rollback()
	yield "</pre>"


def netsort(a, b):
	return int(a.net*1000 - b.net*1000)

def rawsort(a, b):
	if a.status != "OK" and b.status != "OK": return 0
	if a.status != "OK": return 1
	if b.status != "OK": return -1
	return int(a.raw*1000 - b.raw*1000)


def total(*args):
	tot = 0
	for a in args:
		if a is not None:
			tot += a
	return tot

def UpdateRunTotals(session, event, carid, course, index, counted, indexafterpenalties):
	min = 10
	runs = session.query(Run).filter(Run.eventid==event.id).filter(Run.carid==carid).filter(Run.course==course).all()
	for r in runs:
		if abs(r.raw - total(r.seg1, r.seg2, r.seg3, r.seg4, r.seg5)) > 0.001:
			r.seg1 = 0
			r.seg2 = 0
			r.seg3 = 0
			r.seg4 = 0
			r.seg5 = 0

		if r.sixty < 1.200:
			r.sixty = 0.0

		if r.status == "OK":
			if indexafterpenalties:
				r.net = (r.raw + (event.conepen * r.cones) + (event.gatepen * r.gates)) * index
			else:
				r.net = (r.raw * index) + (event.conepen * r.cones) + (event.gatepen * r.gates)
		else:
			r.net = 999.999


	for ii, r in enumerate(sorted(runs, netsort)):
		r.bnorder = (ii+1)
		r.norder = -1

	for ii, r in enumerate(sorted(runs, rawsort)):
		r.brorder = (ii+1)
		r.rorder = -1

	reduxruns = filter(lambda x: x.run <= counted, runs)
	for ii, r in enumerate(sorted(reduxruns, netsort)):
		r.norder = (ii+1)

	for ii, r in enumerate(sorted(reduxruns, rawsort)):
		r.rorder = (ii+1)

