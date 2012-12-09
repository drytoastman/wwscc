from pylons import request, response
from pylons.templating import render_mako
from pylons.decorators import jsonify
from nwrsc.model import *
from nwrsc.controllers.lib.base import BeforePage, BaseController
from nwrsc.lib.helpers import t3
from simplejson import JSONEncoder
import time
import operator


class JEncoder(JSONEncoder):
	def default(self, o):
		if hasattr(o, 'getFeed'):
			return o.getFeed()
		else:
			return ""

def _extract(obj, *keys):
	if type(obj) is dict:
		ret = dict([(k, obj[k]) for k in keys])
	else:
		ret = dict([(k, getattr(obj,k)) for k in keys])
	for k, v in ret.iteritems():
		if type(v) is float:
			ret[k] = t3(v)
	return ret

def _convertTTS(tts):
	ret = []
	position = 1
	cols = tts.cols
	for row in tts.rows:
		ret.append(dict([(cols[ii].lower() or 'indexcode', row[ii]) for ii in range(len(cols))]))
		ret[-1]['position'] = position
		position += 1
	return ret


class MobileController(BaseController):

	def __before__(self):
		self.eventid = self.routingargs.get('eventid', None)
		if int(self.eventid) > 0:
			self.eventid = int(self.eventid)
			self.event = self.session.query(Event).get(self.eventid)

		carid = self.routingargs.get('other', None)
		if carid is not None:
			self.carid = int(carid)
	
		self._loaded = False


	def _encode(self, head, o):
		response.headers['Content-type'] = 'text/javascript'
		return JEncoder(indent=1).encode(o)

	def index(self):
		if self.database is None:
			return self._encode("serieslist", self._databaseList())
		elif self.eventid is None:
			return self._encode("events", self.session.query(Event).all())
		elif self.eventid == 'classes':
			return self._encode("classes", self.session.query(Class).all())
		elif self.eventid == 'indexes':
			return self._encode("indexes", self.session.query(Index).all())
		else:
			return self._encode("nothing", [])


	@jsonify
	def last(self):
		query = self.session.query(AnnouncerData.updated, AnnouncerData.carid)
		query = query.filter(AnnouncerData.eventid==self.eventid)
		query = query.join(Class).filter(Class.code == request.GET['class'])
		query = query.order_by(AnnouncerData.updated.desc())
		row = query.first()
		if row is None:
			return {}
		return {'updated':time.mktime(row[0].timetuple()), 'carid':row[1]}


	def toptimes(self):
		ret = {}
		classdata = ClassData(self.session)
		toptimes = TopTimesStorage(self.session, self.event, classdata)
		ret['topraw'] = _convertTTS(toptimes.getList(allruns=False, raw=True, course=0))
		ret['topnet'] = _convertTTS(toptimes.getList(allruns=False, raw=False, course=0))
		return self._encode("toptimes", ret)


	def entrant(self):
		(self.driver, self.car) = self.session.query(Driver,Car).join('cars').filter(Car.id==self.carid).first()
		if self.driver.alias and not config['nwrsc.private']:
			self.driver.firstname = self.driver.alias
			self.driver.lastname = ""

		self.cls = self.session.query(Class).filter(Class.code==self.car.classcode).first()
		self.announcer = self.session.query(AnnouncerData).filter(AnnouncerData.eventid==self.eventid).filter(AnnouncerData.carid==self.carid).first()
		if self.announcer is None:
			raise BeforePage('')

		ret = {}
		ret['runlist'] = self._runlist()
		ret['classlist'] = self._classlist()
		ret['champlist'] = self._champlist()
		return self._encode("entrant", ret)
		
	def _runlist(self):
		query = self.session.query(Run).filter(Run.carid==self.carid).filter(Run.eventid==self.eventid)
		query = query.filter(Run.course==1).filter(Run.course==self.announcer.lastcourse).order_by(Run.run)
		return query.all()

	def _classlist(self):
		ret = []
		for res in getClassResultsShort(self.session, self.settings, self.event, self.cls):
			ret.append(_extract(res, 'sum', 'pospoints', 'diffpoints', 'carid', 'firstname', 'lastname', 'indexstr', 'position', 'trophy'))
		if self.announcer.oldsum > 0:
			ret.append({'sum': t3(self.announcer.oldsum), 'firstname':self.driver.firstname, 'lastname':self.driver.lastname, 'position':'old'})
		if self.announcer.potentialsum > 0:
			ret.append({'sum': t3(self.announcer.potentialsum), 'firstname':self.driver.firstname, 'lastname':self.driver.lastname, 'position':'raw'})
		ret.sort(key=operator.itemgetter('sum'))
		return ret

	def _champlist(self):
		ret = []
		pos = 1
		for res in getChampResults(self.session, self.settings, self.cls.code).get(self.cls.code, []):
			entry = dict()
			entry['points'] = res.points.total
			entry['carid'] = res.carid
			entry['firstname'] = res.firstname
			entry['lastname'] = res.lastname
			entry['events'] = res.events
			entry['position'] = pos
			ret.append(entry)
			pos += 1

			if res.carid != self.carid:
				continue
			
			if res.points == res.pospoints:
				if self.announcer.oldpospoints > 0:
					entry = entry.copy()
					entry['position'] = "old"
					entry['points'] = res.pospoints.theory(self.eventid, self.announcer.oldpospoints)
					ret.append(entry)
				if self.announcer.potentialpospoints > 0:
					entry = entry.copy()
					entry['position'] = "raw"
					entry['points'] = res.pospoints.theory(self.eventid, self.announcer.potentialpospoints)
					ret.append(entry)

			if res.points == res.diffpoints:
				if self.announcer.olddiffpoints > 0:
					entry = entry.copy()
					entry['position'] = "old"
					entry['points'] = t3(res.diffpoints.theory(self.eventid, self.announcer.olddiffpoints))
					ret.append(entry)
				if self.announcer.potentialdiffpoints > 0:
					entry = entry.copy()
					entry['position'] = "raw"
					entry['points'] = t3(res.diffpoints.theory(self.eventid, self.announcer.potentialdiffpoints))
					ret.append(entry)

		ret.sort(key=operator.itemgetter('points'), reverse=True)
		return ret

