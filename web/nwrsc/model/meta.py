from sqlalchemy import ThreadLocalMetaData
from sqlalchemy.orm import scoped_session, sessionmaker, _Session
from sqlmap import sqlmap


class MySession(_Session):

	def __init__(self, **kwargs):
		_Session.__init__(self, **kwargs)

	def sqlmap(self, key, args):
		""" Execute on the connection directly so we can use '?' markers in SQL """
		dargs = dict()
		for ii, a in enumerate(args):
			dargs['h%d'%(ii+1)] = a
		return self.execute(sqlmap[key], dargs)


metadata = ThreadLocalMetaData()
Session = scoped_session(sessionmaker(class_=MySession, autocommit=False))

