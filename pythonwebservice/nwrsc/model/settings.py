
from sqlalchemy import Table, Column
from sqlalchemy.orm import mapper
from sqlalchemy.types import String

from meta import metadata


## Settings table
t_settings = Table('settings', metadata,
	Column('name', String(32), primary_key=True),
	Column('val', String(128)),
	)

class Setting(object):
	pass
			
mapper(Setting, t_settings)


class Settings(object):

	INTS = ["largestcarnumber", "useevents", "minevents"]
	BOOLS = ["locked", "superuniquenumbers", "indexafterpenalties", "usepospoints"]
	STRS = ["pospointlist", "champsorting", "seriesname", "sponsorlink", "schema", "parentseries", "classinglink"]
	FLOATS = []

	def __init__(self):
		self.locked = False
		self.superuniquenumbers = False
		self.indexafterpenalties = False
		self.usepospoints = False

		self.pospointlist = "20,16,13,11,9,7,6,5,4,3,2,1"
		self.champsorting = ""
		self.seriesname = ""
		self.sponsorlink = ""
		self.schema = "missing"
		self.parentseries = ""
		self.classinglink = ""

		self.largestcarnumber = 1999
		self.useevents = 6
		self.minevents = 0


	def set(self, items):
		for k, v in items.iteritems():
			if k in Settings.INTS:
				setattr(self, k, int(v))
			elif k in Settings.FLOATS:
				setattr(self, k, float(v))
			elif k in Settings.BOOLS:
				setattr(self, k, v in ("True", "1", True, "checked"))
			else:
				setattr(self, k, v)
			
		
	def load(self, session):
		for s in session.query(Setting):
			if s.name in Settings.INTS:
				setattr(self, s.name, int(s.val))
			elif s.name in Settings.FLOATS:
				setattr(self, s.name, float(s.val))
			elif s.name in Settings.BOOLS:
				setattr(self, s.name, s.val in ("True", "1"))
			else:
				setattr(self, s.name, s.val)

	def save(self, session):
		for name in Settings.INTS + Settings.FLOATS + Settings.STRS + Settings.BOOLS:
			s = session.query(Setting).get(name)
			if s is None:
				s = Setting()
				s.name = name
				session.add(s)

			if name in Settings.BOOLS:
				s.val = getattr(self, name) and "1" or "0"
			else:
				s.val = str(getattr(self, name))

