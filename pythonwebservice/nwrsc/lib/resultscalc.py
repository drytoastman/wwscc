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

def UpdateClassResults(session, eventid, course, classcode, carid):
	cevals = [classcode, eventid]

	# Delete current event results for the same event/class, then query runs table for new results 
	session.sqlmap("DELETECLASSRESULTS", cevals)

	# Now we will loop from 1st to last, calculating points and inserting new event results 
	position = 1
	first = True
	basis = 1.0
	prev = 1.0
	basecnt = 1
	mysum = 0
	sumlist = []

	PPOINTS = [int(x) for x in session.query(Setting).get('pospointlist').val.split(',')]
	lists = []
	for r in session.sqlmap("GETCLASSRESULTS", cevals):
		thesum = float(r['sum'])
		insertcarid = int(r['carid'])

		sumlist.append(thesum)
		if insertcarid == carid:
			mysum = thesum

		cnt = int(r['coursecnt'])
		if (first):
			basis = thesum
			prev = thesum
			basecnt = cnt
			first = False
	
		rvals = [0]*10
		rvals[0] = eventid
		rvals[1] = insertcarid
		rvals[2] = classcode # classcode doesn't change
		rvals[3] = position
		rvals[4] = cnt
		rvals[5] = thesum

		if cnt == basecnt:
			rvals[6] = thesum-prev
			rvals[7] = basis/thesum*100
			rvals[8] = position >= len(PPOINTS) and PPOINTS[-1] or PPOINTS[position-1]
		else:
			#This person ran less courses than the other people
			rvals[6] = 999.999
			rvals[7] = 0.0
			rvals[8] = 0
		
		session.sqlmap("INSERTCLASSRESULTS", rvals)
		position += 1
		prev = thesum

	if course != 0 and carid != 0:
		UpdateAnnouncerDetails(session, eventid, course, carid, classcode, mysum, sumlist, PPOINTS)


def UpdateAnnouncerDetails(session, eventid, course, carid, classcode, mysum, sumlist, PPOINTS):
	"""
		Calculate from other sums based on old runs or clean runs, based on runs on currentCourse
	"""

	session.query(AnnouncerData).filter(AnnouncerData.carid==carid).filter(AnnouncerData.eventid==eventid).delete()

	data = AnnouncerData()  # This is our object to be updated
	session.add(data)
	data.eventid = eventid
	data.carid = carid
	data.classcode = classcode
	data.lastcourse = course
	data.updated = datetime.datetime.now()
	
	# Just get runs from last course that was recorded
	runs = {}
	for r in session.query(Run).filter(Run.carid==carid).filter(Run.eventid==eventid).filter(Run.course==course): 
		runs[r.run] = r

	if not len(runs):
		return  # Nothing to do

	runlist = sorted(runs.keys())
	lastrun = runs[runlist[-1]]

	if len(runs) > 1:
		if lastrun.norder == 1:  # we improved our position
			# find run with norder = 2, create the old entry with sum - lastrun + prevrun
			prevbest = [x for x in runs.values() if x.norder == 2][0]
			data.rawdiff = lastrun.raw - prevbest.raw
			data.netdiff = lastrun.net - prevbest.net
			data.oldsum = mysum - lastrun.net + prevbest.net
	
		if lastrun.cones != 0 or lastrun.gates != 0:
			# add table entry with what could have been without penalties
			car = session.query(Car).get(carid)
			index = ClassData(session).getEffectiveIndex(car)
			curbest = [x for x in runs.values() if x.norder == 1][0]
			theory = mysum - curbest.net + ( lastrun.raw * index )
			if theory < mysum:
				data.rawdiff = lastrun.raw - curbest.raw
				data.netdiff = lastrun.net - curbest.net
				data.potentialsum = theory


	sumlist.remove(mysum);
	if data.oldsum > 0:
		sumlist.append(data.oldsum);
		sumlist.sort();
		position = sumlist.index(data.oldsum)+1
		data.oldpospoints = position >= len(PPOINTS) and PPOINTS[-1] or PPOINTS[position-1]
		data.olddiffpoints = min(100, sumlist[0]/data.oldsum*100);
		sumlist.remove(data.oldsum);

	if data.potentialsum > 0:
		sumlist.append(data.potentialsum)
		sumlist.sort()
		position = sumlist.index(data.potentialsum)+1
		data.potentialpospoints = position >= len(PPOINTS) and PPOINTS[-1] or PPOINTS[position-1]
		data.potentialdiffpoints = min(100, sumlist[0]/data.potentialsum*100);

	session.commit()



def RecalculateResults(session, settings):
	yield "<pre>"
	try:
		classdata = ClassData(session)

		for event in session.query(Event).all():
			yield "Event: %d\n" % (event.id)
			session.execute("delete from eventresults where eventid=%d" % event.id) # If admin changes car class, this is needed
	
			for car in session.query(Run.carid).distinct().filter(Run.eventid==event.id):
				codes = session.query(Car.classcode,Car.indexcode,Car.tireindexed).filter(Car.id==car.carid).first()
				if codes is None:
					yield "\tId: %s **** Missing car ****\n" % car.carid
					continue
				val = classdata.getEffectiveIndex(codes) 
				counted = min(classdata.getCountedRuns(codes.classcode), event.getCountedRuns())
				istr = classdata.getIndexStr(codes)
				yield "\tId: %s (%s, %s, %s)\n" % (car.carid, val, istr, (counted < 100 and counted or "all"))

				for course in range(1, event.courses+1):
					UpdateRunTotals(session, event, car.carid, course, val, counted, settings.indexafterpenalties)
			session.commit()

			for cls in session.query(Car.classcode).distinct().join(Run).filter(Run.eventid==event.id):
				yield "\tCls: %s\n" % (cls)
				UpdateClassResults(session, event.id, 0, cls.classcode, 0)

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

