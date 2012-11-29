from math import ceil
from runs import Run
from pylons import config

class Result(object):
	""" Contains driver name, car description, overall result and 2D array of runs by course and run # """

	def __init__(self, row, **kwargs): # kwargs = classdata, usepospoints
		for k in row.keys():
			setattr(self, k, getattr(row,k))

		if 'classdata' in kwargs:
			self.indexstr = kwargs['classdata'].getIndexStr(self.classcode, self.indexcode)
			self.indexval = kwargs['classdata'].getEffectiveIndex(self.classcode, self.indexcode)
		else:
			self.indexstr = self.indexcode
			self.indexval = 1.0

		if type(getattr(self, 'toptime', None)) is not float:
			self.toptime = 0.0

		if self.alias and not config['nwrsc.private']:
			self.firstname = self.alias
			self.lastname = ""

		if 'usepospoints' in kwargs:
			if kwargs['usepospoints']:
				self.points = self.pospoints
			else:
				self.points = self.diffpoints


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


auditList = """select r.*,d.firstname,d.lastname,d.alias,c.year,c.number,c.make,c.model,c.color,c.classcode,c.indexcode 
				from runorder as r, cars as c, drivers as d  
				where r.carid=c.id and c.driverid=d.id and 
				r.eventid=:eventid and r.course=:course and r.rungroup=:group """

def getAuditResults(session, settings, event, course, rungroup):
	ret = list()
	reshold = dict()
	for row in session.execute(auditList, params={'eventid':event.id, 'course':course, 'group':rungroup}):
		r = Result(row)
		r.runs = [None] * event.runs
		ret.append(r)
		reshold[r.carid] = r

	for run in session.query(Run).filter_by(eventid=event.id).filter(Run.course==course).filter(Run.carid.in_(reshold.iterkeys())):
		r = reshold[run.carid]
		r.runs[run.run-1] = run

	return ret



classResult = """select r.*, c.year, c.make, c.model, c.color, c.number, c.indexcode, d.firstname, d.lastname, d.alias
				from eventresults as r, cars as c, drivers as d 
				where r.carid=c.id and c.driverid=d.id and r.eventid=%d and r.classcode in (%s)
				order by r.position"""

def getClassResultsShort(session, settings, event, cls):
	"""
		Similar to getClassResults but but doesn't include any run information.
		For display of final points, nettime, only, this will be quicker and use less memory
	"""
	ret = []
	for row in session.execute(classResult % (event.id, "'%s'" % cls.code)):
		ret.append(Result(row, usepospoints=settings.usepospoints))

	trophydepth = ceil(len(ret) / 3.0)
	for ii, result in enumerate(ret):
		result.trophy = (cls.eventtrophy) and (ii < trophydepth)

	return ret



def getClassResults(session, settings, event, classdata, codes):
	"""
		Get a dictionary of results that looks like:
		{classcode}[orderedlistof Results]
		The Result class will include all their runs as well as trophy indicators
	"""
	ret = dict()
	reshold = dict()

	for code in codes:
		ret[code] = []

	## Outside normal alchemy use, mass one time selects using IN clause, much faster 
	for row in session.execute(classResult % (event.id, ','.join(["'%s'" % x for x in codes]))):
		r = Result(row, classdata=classdata, usepospoints=settings.usepospoints)
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


top1 = "select d.firstname as firstname, d.lastname as lastname, d.alias as alias, c.classcode as classcode, c.indexcode as indexcode, c.id as carid "
top2 = "from runs as r, cars as c, drivers as d where r.carid=c.id and c.driverid=d.id and r.eventid=:eventid "


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
		self.carids = list()
		self.rows = list()

	def add(self, entrant, *vals):
		if entrant.alias and not config['nwrsc.private']:
			firstname = entrant.alias
			lastname = ""
		else:
			firstname = entrant.firstname
			lastname = entrant.lastname
		self.carids.append(entrant.carid)
		self.rows.append((firstname + " " + lastname,) + vals)


class TopTimesStorage(object):

	def __init__(self, session, event, classdata):
		self.session = session
		self.event = event
		self.classdata = classdata
	
		if self.event.courses > 1:
			coursecnt = self.event.courses + 1
		else:
			coursecnt = 1  # all courses == course 1

		# first tuple is allruns vs counted runs
		# next tuple is raw times vs net times
		# last step is a list with 0=all_courses, 1=course1, ...
		self.store = (([None]*coursecnt, [None]*coursecnt), ([None]*coursecnt, [None]*coursecnt))  

		self.segs = [[None]*self.event.getSegmentCount()]*self.event.courses

		self.back = list()


	# for backwards compatibility with what old templates where getting
	def __getitem__(self, key):
		""" call list with backAppend arguments """
		return self.getList(*self.back[key])

	def __len__(self):
		""" just return length of back appended list """
		return len(self.back)

	def __iter__(self):
		""" Allow iteration as we are backwards compatible with list """
		for ii in range(len(self.back)):
			yield self[ii]

	def backAppend(self, allruns=False, raw=False, course=0):
		""" Save the arguments to use at the next index """
		self.back.append((allruns, raw, course))
		
	def getList(self, allruns=False, raw=False, course=0, settitle=None):
		if self.store[allruns][raw][course] is None:
			if course == 0:
				if raw:
					ttl = loadTopRawTimes(self.session, self.event, self.classdata, allruns)
				else:
					ttl = loadTopNetTimes(self.session, self.event, self.classdata, allruns)
			else:
				if raw:
					ttl = loadTopCourseRawTimes(self.session, self.event, self.course, self.classdata, allruns)
				else:
					ttl = loadTopCourseNetTimes(self.session, self.event, self.course, self.classdata, allruns)
	
			self.store[allruns][raw][course] = ttl

		if settitle is not None:
			self.store[allruns][raw][course].title = settitle
		return self.store[allruns][raw][course]


	def getSegmentList(self, course, seg):
		if self.segs[course][seg] is None:
			self.segs[course][seg] = loadTopSegRawTimes(self.session, self.event, course, seg)
		return self.segs[course][seg]
	


def loadTopSegRawTimes(session, event, course, seg):
	getcol = ", MIN(r.seg%d) as toptime " % (seg)
	topSegRaw = top1 + getcol + top2 + " and r.course=:course and r.seg%d > %d group by r.carid order by toptime " % (seg, event.getSegments()[seg-1])

	ttl = TopTimesList("Top Segment Times (Course %d)" % course, "Name", "Class", "Time")
	for row in session.execute(topSegRaw, params={'eventid':event.id, 'course':course}):
		ttl.add(row, row.classcode, "%0.3f" % row.toptime)
	return ttl
			

def loadTopCourseRawTimes(session, event, course, classdata, allruns=False):
	if allruns:
		sql = topCourseRawAll
	else:
		sql = topCourseRaw

	ttl = TopTimesList("Top Times (Course %d)" % course, "Name", "Class", "Time")
	for row in session.execute(sql, params={'eventid':event.id, 'course':course, 'conepen':event.conepen, 'gatepen':event.gatepen}):
		ttl.add(row, row.classcode, "%0.3f" % row.toptime)
	return ttl
		

def loadTopCourseNetTimes(session, event, course, classdata, allruns=False):
	if allruns:
		sql = topCourseNetAll
	else:
		sql = topCourseNet

	ttl = TopTimesList("Top Index Times (Course %d)" % course, "Name", "Index", "", "Time")
	for row in session.execute(sql, params={'eventid':event.id, 'course':course}):
		eis = classdata.getIndexStr(row.classcode, row.indexcode)
		eiv = classdata.getEffectiveIndex(row.classcode, row.indexcode)
		ttl.add(row, "%0.3f" % eiv, eis, "%0.3f" % row.toptime)
	return ttl


def loadTopRawTimes(session, event, classdata, allruns=False):
	if allruns:
		sql = topRawAll
		title = "Top Times (All)"
	else:
		sql = topRaw
		title = "Top Times (Counted)"

	ttl = TopTimesList(title, "Name", "Class", "Time")
	for row in session.execute(sql, params={'eventid':event.id,'conepen':event.conepen,'gatepen':event.gatepen}):
		ttl.add(row, row.classcode, "%0.3f" % row.toptime)
	return ttl


def loadTopNetTimes(session, event, classdata, allruns=False):
	if allruns:
		sql = topNetAll
		title = "Top Index Times (All)"
	else:
		sql = topNet
		title = "Top Index Times (Counted)"

	ttl = TopTimesList(title, "Name", "Index", "", "Time")
	for row in session.execute(sql, params={'eventid':event.id}):
		eis = classdata.getIndexStr(row.classcode, row.indexcode)
		eiv = classdata.getEffectiveIndex(row.classcode, row.indexcode)
		ttl.add(row, "%0.3f" % eiv, eis, "%0.3f" % row.toptime)
	return ttl

