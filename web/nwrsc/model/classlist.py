from sqlalchemy import Table, Column, ForeignKey, UniqueConstraint
from sqlalchemy.orm import mapper, relation
from sqlalchemy.types import Integer, String, Boolean, Float

from meta import metadata
import logging
import sys

log = logging.getLogger(__name__)

## Classes table
t_classlist = Table('classlist', metadata,
	Column('code', String(16), primary_key=True),
	Column('descrip', String(128)),
	Column('carindexed', Boolean, default=False),
	Column('classindexed', Boolean, default=False),
	Column('classmultiplier', Float, default=1.0),
	Column('eventtrophy', Boolean, default=True),
	Column('champtrophy', Boolean, default=True),
	Column('numorder', Integer),
	Column('countedruns', Integer),
	UniqueConstraint('code', name='classidx_1')
	)

class Class(object):

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
			if isinstance(v, bool):
				d[k] = v and "1" or "0"
			else:
				d[k] = v
		return d


	@classmethod
	def activeClasses(cls, session, eventid):
		sql = "select distinct x.* from classlist as x, cars as c, runs as r " + \
				"where r.eventid=:id and r.carid=c.id and c.classcode=x.code"
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


class ClassData(object):

	def __init__(self, session):
		self.classlist = dict()
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
		
	def getIndexStr(self, classcode, indexcode):
		indexstr = indexcode
		try:
			cls = self.classlist[classcode]
			if cls.classindexed:
				indexstr = classcode

			if cls.classmultiplier < 1.000:
				indexstr = indexstr + '*'
		except:
			pass
		return indexstr


	def getEffectiveIndex(self, classcode, indexcode):
		indexval = 1.0
		try:
			cls = self.classlist[classcode]

			if cls.classindexed:
				indexval *= self.indexlist[classcode].value

			if cls.carindexed and indexcode:
				indexval *= self.indexlist[indexcode].value

			indexval *= cls.classmultiplier
		except Exception, e:
			log.warning("getEffectiveIndex(%s,%s) failed: %s" % (classcode, indexcode, e))

		return indexval


