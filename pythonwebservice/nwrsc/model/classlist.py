from sqlalchemy import Table, Column, ForeignKey, UniqueConstraint
from sqlalchemy.orm import mapper, relation, object_session
from sqlalchemy.types import Integer, String, Boolean, Float

from meta import metadata
from settings import Setting

import logging
import sys
import re

from collections import defaultdict

log = logging.getLogger(__name__)

## Classes table
t_classlist = Table('classlist', metadata,
	Column('code', String(16), primary_key=True),
	Column('descrip', String(128)),
	Column('carindexed', Boolean, default=False),
	Column('classindex', String(16), default=""),
	Column('classmultiplier', Float, default=1.0),
	Column('eventtrophy', Boolean, default=True),
	Column('champtrophy', Boolean, default=True),
	Column('numorder', Integer),
	Column('countedruns', Integer),
	Column('usecarflag', Boolean),
	Column('caridxrestrict', String(128)),
	UniqueConstraint('code', name='classidx_1')
	)

class Class(object):

	RINDEX = re.compile(r'([+-])\((.*?)\)')
	RFLAG = re.compile(r'([+-])\[(.*?)\]')

	def __init__(self, **kwargs):
		for k, v in kwargs.iteritems():
			if hasattr(self, k):
				setattr(self, k, v)

	def getCountedRuns(self):
		if self.countedruns <= 0:
			return sys.maxint
		else:
			return self.countedruns

	def getFeed(self):
		d = dict()
		for k,v in self.__dict__.iteritems():
			if v is None or k in ['_sa_instance_state']:
				continue
			if isinstance(v, float):
				if v != 0:
					d[k] = "%0.3f" % (v)
			else:
				d[k] = v
		return d


	def _globItem(self, item, full):
	    tomatch = '^' + item.strip().replace('*', '.*') + '$'
	    ret = set()
	    for x in full:
	        if re.search(tomatch, x):
	            ret.add(x)
	    return ret

	def _processList(self, results, fullset):
	    ret = set(fullset)
	    for ii, pair in enumerate(results):
	        ADD = (pair[0] == '+')
	        if ii == 0 and ADD:
	            ret = set()
	        for item in pair[1].split(','):
	            if ADD:
	                ret |= self._globItem(item, fullset)
	            else:
	                ret -= self._globItem(item, fullset)
	    return fullset - ret


	def restrictedIndexes(self):
		if not self.caridxrestrict:
			return ([], [])
		full = self.caridxrestrict.replace(" ", "")
		idxlist = set([x[0] for x in object_session(self).query(Index.code).all()])
		indexrestrict = self._processList(self.RINDEX.findall(full), idxlist)
		flagrestrict = self._processList(self.RFLAG.findall(full), idxlist)

		return (indexrestrict, flagrestrict)


	@classmethod
	def activeClasses(cls, session, eventid):
		sql = "select distinct x.* from classlist as x, cars as c, runs as r where r.eventid=:id and r.carid=c.id and c.classcode=x.code"
		return list(session.execute(sql, params={'id':eventid}, mapper=Class))


mapper(Class, t_classlist)

## Indexes table
t_indexlist = Table('indexlist', metadata,
	Column('code', String(16), primary_key=True),
	Column('descrip', String(128)),
	Column('value', Float),
	UniqueConstraint('code', name='indexidx_1')
	)

class Index(object):

	def __init__(self, **kwargs):
		for k, v in kwargs.iteritems():
			if hasattr(self, k):
				setattr(self, k, v)

	def getFeed(self):
		d = dict()
		for k,v in self.__dict__.iteritems():
			if v is None or k in ['_sa_instance_state']:
				continue
			if isinstance(v, float):
				if v != 0:
					d[k] = "%0.3f" % (v)
			else:
				d[k] = v
		return d


mapper(Index, t_indexlist)


class PlaceHolder(object):
	def __init__(self):
		self.code = 'UKNWN'
		self.descrip = "Class for unknown scanned drivers"
		self.carindexed = False
		self.classindex = ""
		self.classmultiplier = 1.0
		self.eventtrophy = False
		self.champtrophy = False
		self.numorder = 0
		self.countedruns = 0
		self.usecarflag = False
		self.caridxrestrict = ""

	def restrictedIndexes(self):
		return ([], [])
	
class ClassData(object):

	def __init__(self, session):
		self.classlist = defaultdict(PlaceHolder)
		self.indexlist = dict()
		for cls in session.query(Class):
			self.classlist[cls.code] = cls
		for idx in session.query(Index):
			self.indexlist[idx.code] = idx


	def getCountedRuns(self, classcode):
		try:
			return self.classlist[classcode].getCountedRuns()
		except:
			return sys.maxint
		

	def getIndexStr(self, car): #classcode, indexcode, tireindexed):
		indexstr = car.indexcode or ""
		try:
			cls = self.classlist[car.classcode]
			if cls.classindex != "":
				indexstr = cls.classindex

			if cls.classmultiplier < 1.000 and (not cls.usecarflag or car.tireindexed):
				indexstr = indexstr + '*'
		except:
			pass
		return indexstr


	def getEffectiveIndex(self, car): #classcode, indexcode, tireindexed):
		indexval = 1.0
		try:
			cls = self.classlist[car.classcode]

			if cls.classindex != "":
				indexval *= self.indexlist[cls.classindex].value

			if cls.carindexed and car.indexcode:
				indexval *= self.indexlist[car.indexcode].value

			if cls.classmultiplier < 1.000 and (not cls.usecarflag or car.tireindexed):
				indexval *= cls.classmultiplier

		except Exception, e:
			log.warning("getEffectiveIndex(%s,%s,%s) failed: %s" % (car.classcode, car.indexcode, car.tireindexed, e))

		return indexval


