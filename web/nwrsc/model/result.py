from math import ceil
from data import Run

class Result(object):
	""" Contains driver name, car description, overall result and 2D array of runs by course and run # """

	def __init__(self, row, classdata):
		for k in row.keys():
			setattr(self, k, getattr(row,k))

		if classdata is not None:
			self.indexstr = classdata.getIndexStr(self.classcode, self.indexcode)
			self.indexval = classdata.getEffectiveIndex(self.classcode, self.indexcode)
		else:
			self.indexstr = self.indexcode
			self.indexval = 1.0

		if type(getattr(self, 'toptime', None)) is not float:
			self.toptime = 0.0

	def getFeed(self):
		ret = dict()
		for k,v in self.__dict__.iteritems():
			if k in ['id', 'updated']: continue
			if v is None or v == '': continue
			if isinstance(v, float):
				ret[k] = "%0.3f" % (v)
			else:
				ret[k] = v
		return ret


auditList = """select r.*,d.firstname,d.lastname,c.year,c.number,c.make,c.model,c.color,c.classcode,c.indexcode 
				from runorder as r, cars as c, drivers as d  
				where r.carid=c.id and c.driverid=d.id and 
				r.eventid=:eventid and r.course=:course and r.rungroup=:group """

def getAuditResults(session, event, course, rungroup):
	ret = list()
	reshold = dict()
	for row in session.execute(auditList, params={'eventid':event.id, 'course':course, 'group':rungroup}):
		r = Result(row, None)
		r.runs = [None] * event.runs
		ret.append(r)
		reshold[r.carid] = r

	for run in session.query(Run).filter_by(eventid=event.id).filter(Run.course==course).filter(Run.carid.in_(reshold.iterkeys())):
		r = reshold[run.carid]
		r.runs[run.run-1] = run

	return ret



classResult = """select r.*, c.year, c.make, c.model, c.color, c.number, c.indexcode, d.firstname, d.lastname
				from eventresults as r, cars as c, drivers as d 
				where r.carid=c.id and c.driverid=d.id and r.eventid=%d and r.classcode in (%s)
				order by r.position"""

def getClassResultsShort(session, event, cls):
	ret = []
	#lasttime = 0

	for row in session.execute(classResult % (event.id, "'%s'" % cls.code)):
		ret.append(Result(row, None))

	trophydepth = ceil(len(ret) / 3.0)
	for ii, result in enumerate(ret):
		result.trophy = (cls.eventtrophy) and (ii < trophydepth)
#		if result.updated > lasttime:
#			lasttime = result.updated

	return ret #, lasttime)



def getClassResults(session, event, classdata, codes):
	ret = dict()
	reshold = dict()

	for code in codes:
		ret[code] = []

	## Outside normal alchemy use, mass one time selects using IN clause, much faster 
	for row in session.execute(classResult % (event.id, ','.join(["'%s'" % x for x in codes]))):
		r = Result(row, classdata)
		r.runs = [[None for i in range(event.runs)] for j in range(event.courses)]
		ret[r.classcode].append(r)
		reshold[r.carid] = r

	for run in session.query(Run).filter_by(eventid=event.id).filter(Run.carid.in_(reshold.iterkeys())):
		r = reshold[run.carid]
		if run.course > event.courses or run.run > event.runs:
			continue
		r.runs[run.course-1][run.run-1] = run
		for attr in ('raw', 'net', 'reaction', 'sixty', 'seg1', 'seg2', 'seg3', 'seg4', 'seg5'):
			if getattr(run, attr, None) is None:
				setattr(run, attr, 0.0)

	for code, clsresult in ret.iteritems():
		trophydepth = ceil(len(clsresult) / 3.0)
		for ii, result in enumerate(clsresult):
			result.trophy = (classdata.classlist[code].eventtrophy) and (ii < trophydepth)

	return ret


top1 = "select d.firstname as firstname, d.lastname as lastname, c.classcode as classcode, c.indexcode as indexcode, c.id as carid "
top2 = "from runs as r, cars as c, drivers as d where r.carid=c.id and c.driverid=d.id and r.eventid=:eventid "


def loadTopSegRawTimes(session, event, course, seg, classdata):
	getcol = ", MIN(r.seg%d) as toptime " % (seg)
	topSegRaw = top1 + getcol + top2 + " and r.course=:course and r.seg%d > %d group by r.carid order by toptime " % (seg, event.getSegments()[seg-1])
	return [Result(row,classdata) for row in session.execute(topSegRaw, params={'eventid':event.id, 'course':course})]

			
topCourseRaw    = top1 + ", (r.raw+:conepen*r.cones+:gatepen*r.gates) as toptime " + top2 + " and r.course=:course and r.norder=1 order by toptime"
topCourseRawAll = top1 + ", (r.raw+:conepen*r.cones+:gatepen*r.gates) as toptime " + top2 + " and r.course=:course and r.bnorder=1 order by toptime"
topCourseNet    = top1 + ", r.net as toptime " + top2 + " and r.course=:course and r.norder=1 order by toptime"
topCourseNetAll = top1 + ", r.net as toptime " + top2 + " and r.course=:course and r.bnorder=1 order by toptime"
topRaw    = top1 + ", SUM(r.raw+:conepen*r.cones+:gatepen*r.gates) as toptime " + top2 + " and r.norder=1 group by c.id order by toptime"
topRawAll = top1 + ", SUM(r.raw+:conepen*r.cones+:gatepen*r.gates) as toptime " + top2 + " and r.bnorder=1 group by c.id order by toptime"
topNet    = top1 + ", SUM(r.net) as toptime " + top2 + " and r.norder=1 group by c.id order by toptime"
topNetAll = top1 + ", SUM(r.net) as toptime " + top2 + " and r.bnorder=1 group by c.id order by toptime"


class TopTimesList(object):
	def __init__(self, title, *cols):
		self.title = title
		self.cols = cols
		self.rows = list()

	def add(self, *vals):
		self.rows.append(vals)


def loadTopCourseRawTimes(session, event, course, classdata, all=False):
	if all:
		sql = topCourseRawAll
	else:
		sql = topCourseRaw

	ttl = TopTimesList("Top Times (Course %d)" % course, "Name", "Class", "Time")
	for row in session.execute(sql, params={'eventid':event.id, 'course':course, 'conepen':event.conepen, 'gatepen':event.gatepen}):
		ttl.add(row.firstname + " " + row.lastname, row.classcode, "%0.3f" % row.toptime)
	return ttl
		

def loadTopCourseNetTimes(session, event, course, classdata, all=False):
	if all:
		sql = topCourseNetAll
	else:
		sql = topCourseNet

	ttl = TopTimesList("Top Index Times (Course %d)" % course, "Name", "Index", "", "Time")
	for row in session.execute(sql, params={'eventid':event.id, 'course':course}):
		eis = classdata.getIndexStr(row.classcode, row.indexcode)
		eiv = classdata.getEffectiveIndex(row.classcode, row.indexcode)
		ttl.add(row.firstname + " " + row.lastname, "%0.3f" % eiv, eis, "%0.3f" % row.toptime)
	return ttl


def loadTopRawTimes(session, event, classdata, all=False):
	if all:
		sql = topRawAll
	else:
		sql = topRaw

	title = "Top Times"
	if all: title += " (All Runs)"
	ttl = TopTimesList(title, "Name", "Class", "Time")
	for row in session.execute(sql, params={'eventid':event.id,'conepen':event.conepen,'gatepen':event.gatepen}):
		ttl.add(row.firstname + " " + row.lastname, row.classcode, "%0.3f" % row.toptime)
	return ttl


def loadTopNetTimes(session, event, classdata, all=False):
	if all:
		sql = topNetAll
	else:
		sql = topNet

	title = "Top Index Times"
	if all: title += " (All Runs)"
	ttl = TopTimesList(title, "Name", "Index", "", "Time")
	for row in session.execute(sql, params={'eventid':event.id}):
		eis = classdata.getIndexStr(row.classcode, row.indexcode)
		eiv = classdata.getEffectiveIndex(row.classcode, row.indexcode)
		ttl.add(row.firstname + " " + row.lastname, "%0.3f" % eiv, eis, "%0.3f" % row.toptime)
	return ttl

