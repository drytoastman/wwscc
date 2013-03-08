
from sqlalchemy import Table, Column
from sqlalchemy.orm import mapper
from sqlalchemy.types import String

from meta import metadata


## Settings table
t_passwords = Table('passwords', metadata,
	Column('tag', String(12), primary_key=True),
	Column('value', String(64)),
	)

class Password(object):
	def __init__(self, **kwargs):
		for k, v in kwargs.iteritems():
			if hasattr(self, k):
				setattr(self, k, v)

	@classmethod
	def load(cls, session):
		ret = dict()
		for pwd in session.query(Password):
			ret[pwd.tag] = pwd.value
		return ret

	@classmethod
	def save(self, session, values):
		for pwd in self.session.query(Password):
			self.session.delete(pwd)
		for tag, val in values.iteritems():
			self.session.add(Password(tag=tag, val=val))

mapper(Password, t_passwords)


