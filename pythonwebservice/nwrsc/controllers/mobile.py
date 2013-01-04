from pylons import request, response, tmpl_context as c
from pylons.templating import render_mako, render_mako_def
from pylons.decorators import jsonify
from nwrsc.model import *
from nwrsc.controllers.lib.base import BeforePage, BaseController
from nwrsc.lib.helpers import t3
from simplejson import JSONEncoder
import time
import operator

CONVERT = {'old':'improvedon', 'raw':'couldhave', 'current':'highlight'}

class JEncoder(JSONEncoder):
	def default(self, o):
		if hasattr(o, 'getFeed'):
			return o.getFeed()
		else:
			return str(o)

def _extract(obj, *keys):
	if type(obj) is dict:
		ret = dict([(k, obj[k]) for k in keys])
	else:
		ret = dict([(k, getattr(obj,k)) for k in keys])
	for k, v in ret.iteritems():
		if type(v) is float:
			ret[k] = t3(v)
	return ret

class MobileController(BaseController):

	def __before__(self):
		self.eventid = self.routingargs.get('eventid', None)
		try:
			self.eventid = int(self.eventid)
			self.event = self.session.query(Event).get(self.eventid)
		except (ValueError, TypeError):
			pass

	def _encode(self, head, o):
		response.headers['Content-type'] = 'text/javascript'
		return JEncoder(indent=1).encode(o)

	def e2label(self, e):
		if hasattr(e, 'label'):
			return CONVERT.get(getattr(e, 'label',''), '') 
		if hasattr(e, '__getitem__'):
			return CONVERT.get(e.get('label',''), '') 
		return ''

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


	def browser(self):
		return render_mako('/announcer/mobilebrowser.mako')

	def Event(self):
		carid = int(self.routingargs.get('other', 0))
		c.results = self._classlist(carid)
		c.e2label = self.e2label
		return render_mako_def('/announcer/mobiletables.mako', 'classlist')

	def Champ(self):
		carid = int(self.routingargs.get('other', 0))
		c.champ = self._champlist(carid)
		c.cls = self.cls
		c.e2label = self.e2label
		return render_mako_def('/announcer/mobiletables.mako', 'champlist')

	def PAX(self):
		carid = int(self.routingargs.get('other', 0))
		c.e2label = self.e2label
		c.toptimes = self._loadTopTimes(carid, raw=False)
		return render_mako('/announcer/topnettimes.mako').replace('\n', '')

	def Raw(self):
		carid = int(self.routingargs.get('other', 0))
		c.e2label = self.e2label
		c.toptimes = self._loadTopTimes(carid, raw=True)
		return render_mako('/announcer/toprawtimes.mako').replace('\n', '')



	def last(self):
		query = self.session.query(AnnouncerData.updated, AnnouncerData.carid)
		query = query.filter(AnnouncerData.eventid==self.eventid)
		query = query.order_by(AnnouncerData.updated.desc())
		classes = request.GET.get('classcodes', '*').split(',')
		ret = []
		for clazz in classes:
			if clazz != '*':
				row = query.join(Class).filter(Class.code == clazz).first()
			else:
				row = query.first()

			if row is None:
				ret.append({'classcode':clazz, 'updated': 0})
			else:
				ret.append({'classcode':clazz, 'updated':time.mktime(row[0].timetuple()), 'carid':row[1]})

		return self._encode("last", ret)


	def topnet(self):
		carid = int(self.routingargs.get('other', 0))
		return self._encode("toptimes", self._loadTopTimes(carid, False))


	def topraw(self):
		carid = int(self.routingargs.get('other', 0))
		return self._encode("toptimes", self._loadTopTimes(carid, True))


	def _loadTopTimes(self, carid, raw=False):
		ret = {}
		classdata = ClassData(self.session)
		car = self.session.query(Car).get(carid)
		self.announcer = self.session.query(AnnouncerData).filter(AnnouncerData.eventid==self.eventid).filter(AnnouncerData.carid==carid).first()
		if self.announcer is None:
			raise BeforePage('')
		index = classdata.getEffectiveIndex(car)

		toptimes = TopTimesStorage(self.session, self.event, classdata)
		if raw:
			ret = self._convertTTS(toptimes.getList(allruns=False, raw=True, course=0), carid, self.announcer.oldsum/index, self.announcer.potentialsum/index)
		else:
			ret = self._convertTTS(toptimes.getList(allruns=False, raw=False, course=0), carid, self.announcer.oldsum, self.announcer.potentialsum)
		return ret


	def _convertTTS(self, tts, carid, oldsum, rawsum):
		position = 1
		additions = list()
		
		for entry in tts.rows:
			entry.position = position
			position += 1
			if entry.carid == carid:
				entry.label = 'current'
				if oldsum is not None and oldsum > 0:
					additions.append(entry.copyWith(position='old', toptime=t3(oldsum), label='old'))
				if rawsum is not None and rawsum > 0:
					additions.append(entry.copyWith(position='raw', toptime=t3(rawsum), label='raw'))
	
		if additions:
			tts.rows.extend(additions)
			tts.rows.sort(key=operator.attrgetter('toptime'))
			tts.rows.sort(key=operator.attrgetter('courses'), reverse=True)

		return tts.rows


	def _entrant(self, carid):
		ret = {}
		ret['runlist'] = self._runlist(carid)
		ret['classlist'] = self._classlist(carid)
		ret['champlist'] = self._champlist(carid)
		return ret
		

	def runlist(self):
		carid = int(self.routingargs.get('other', 0))
		return self._encode("runlist", self._runlist(int(carid)))

	def _runlist(self, carid):
		self.announcer = self.session.query(AnnouncerData).filter(AnnouncerData.eventid==self.eventid).filter(AnnouncerData.carid==carid).first()
		if self.announcer is None:
			raise BeforePage('')

		query = self.session.query(Run).filter(Run.carid==carid).filter(Run.eventid==self.eventid)
		query = query.filter(Run.course==self.announcer.lastcourse).filter(Run.course==self.announcer.lastcourse).order_by(Run.run)
		runs = query.all()
		if self.announcer.rawdiff:
			runs[-1].rawdiff = self.announcer.rawdiff
		if self.announcer.netdiff:
			runs[-1].netdiff = self.announcer.netdiff
		for r in runs:
			if r.norder == 1: r.label = 'current'
			if r.norder == 2 and self.announcer.oldsum > 0: r.label = 'old'
		if runs[-1].norder != 1 and self.announcer.potentialsum > 0:
			runs[-1].label = 'raw'
			
		return runs


	def classlist(self):
		carid = int(self.routingargs.get('other', 0))
		return self._encode("classlist", self._classlist(int(carid)))

	def _classlist(self, carid):
		(self.driver, self.car) = self.session.query(Driver,Car).join('cars').filter(Car.id==carid).first()
		if self.driver.alias and not config['nwrsc.private']:
			self.driver.firstname = self.driver.alias
			self.driver.lastname = ""
		self.announcer = self.session.query(AnnouncerData).filter(AnnouncerData.eventid==self.eventid).filter(AnnouncerData.carid==carid).first()
		if self.announcer is None:
			raise BeforePage('')

		ret = []
		classdata = ClassData(self.session)
		savecourses = 0
		for res in getClassResultsShort(self.session, self.settings, self.event, classdata, self.car.classcode):
			ret.append(_extract(res, 'sum', 'pospoints', 'diffpoints', 'carid', 'firstname', 'lastname', 'indexstr', 'position', 'trophy', 'diff', 'courses'))
			if res.carid == carid:
				ret[-1]['label'] = "current"
				savecourses = ret[-1]['courses']

		if self.announcer.oldsum > 0:
			ret.append({'sum': t3(self.announcer.oldsum), 'firstname':self.driver.firstname, 'lastname':self.driver.lastname, 'position':'old', 'label':'old', 'courses':savecourses})
		if self.announcer.potentialsum > 0:
			ret.append({'sum': t3(self.announcer.potentialsum), 'firstname':self.driver.firstname, 'lastname':self.driver.lastname, 'position':'raw', 'label':'raw', 'courses':savecourses})
		ret.sort(key=operator.itemgetter('sum'))
		ret.sort(key=operator.itemgetter('courses'), reverse=True)
		return ret


	def champlist(self):
		carid = int(self.routingargs.get('other', 0))
		return self._encode("champlist", self._champlist(int(carid)))

	def _champlist(self, carid):
		self.car = self.session.query(Car).get(carid)
		self.cls = self.session.query(Class).filter(Class.code==self.car.classcode).first()
		self.announcer = self.session.query(AnnouncerData).filter(AnnouncerData.eventid==self.eventid).filter(AnnouncerData.carid==carid).first()
		if self.announcer is None:
			raise BeforePage('')

		ret = []
		pos = 1
		for res in getChampResults(self.session, self.settings, self.cls.code).get(self.cls.code, []):
			entry = dict()
			entry['points'] = t3(res.points.total)
			entry['carid'] = res.carid
			entry['driverid'] = res.id
			entry['firstname'] = res.firstname
			entry['lastname'] = res.lastname
			entry['events'] = res.events
			entry['position'] = pos
			ret.append(entry)
			pos += 1

			if res.id != self.car.driverid:
				continue
			
			entry['label'] = 'current'
			if res.points == res.pospoints:
				if self.announcer.oldpospoints > 0:
					entry = entry.copy()
					entry['position'] = "old"
					entry['label'] = "old"
					entry['points'] = res.pospoints.theory(self.eventid, self.announcer.oldpospoints)
					ret.append(entry)
				if self.announcer.potentialpospoints > 0:
					entry = entry.copy()
					entry['position'] = "raw"
					entry['label'] = "raw"
					entry['points'] = res.pospoints.theory(self.eventid, self.announcer.potentialpospoints)
					ret.append(entry)

			if res.points == res.diffpoints:
				if self.announcer.olddiffpoints > 0:
					entry = entry.copy()
					entry['position'] = "old"
					entry['label'] = "old"
					entry['points'] = t3(res.diffpoints.theory(self.eventid, self.announcer.olddiffpoints))
					ret.append(entry)
				if self.announcer.potentialdiffpoints > 0:
					entry = entry.copy()
					entry['position'] = "raw"
					entry['label'] = "raw"
					entry['points'] = t3(res.diffpoints.theory(self.eventid, self.announcer.potentialdiffpoints))
					ret.append(entry)

		ret.sort(key=lambda x: float(x['points']), reverse=True)
		return ret

