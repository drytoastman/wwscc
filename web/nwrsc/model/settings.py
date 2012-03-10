
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
	BOOLS = ["locked", "superuniquenumbers", "useppoints"]
	STRS = ["ppoints", "seriesname", "sponsorlink", "password", "schema"]

	def __init__(self):
		self.locked = False
		self.superuniquenumbers = False
		self.useppoints = False

		self.ppoints = "20,16,13,11,9,7,6,5,4,3,2,1"
		self.seriesname = ""
		self.sponsorlink = ""
		self.password = ""
		self.schema = "missing"

		self.largestcarnumber = 1999
		self.useevents = 6
		self.minevents = 0


	def set(self, items):
		for k, v in items.iteritems():
			if k in Settings.INTS:
				setattr(self, k, int(v))
			elif k in Settings.BOOLS:
				print "set %s with %s" % (k, v)
				setattr(self, k, v in ("True", "1", True, "checked"))
			elif k in Settings.STRS:
				setattr(self, k, v)
			
		
	def load(self, session):
		for s in session.query(Setting):
			if s.name in Settings.INTS:
				setattr(self, s.name, int(s.val))
			elif s.name in Settings.BOOLS:
				setattr(self, s.name, s.val in ("True", "1"))
			elif s.name in Settings.STRS:
				setattr(self, s.name, s.val)

	def save(self, session):
		for name in Settings.INTS + Settings.STRS + Settings.BOOLS:
			s = session.query(Setting).get(name)
			if s is None:
				s = Setting()
				s.name = name
				session.add(s)

			if name in Settings.BOOLS:
				s.val = getattr(self, name) and "1" or "0"
			else:
				s.val = str(getattr(self, name))

