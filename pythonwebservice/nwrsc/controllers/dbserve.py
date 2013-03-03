import traceback
import sys
import shutil
import logging

log = logging.getLogger(__name__)

from pylons import request, response, session
from pylons.controllers.util import abort, url_for
from sqlalchemy import create_engine

from nwrsc.controllers.lib.base import BaseController, BeforePage
from nwrsc.controllers.lib.auth import SRPAuthentication
from nwrsc.lib.codec import Codec, DataInput
from nwrsc.controllers.lib.resultscalc import UpdateClassResults
from nwrsc.model import *


class DbserveController(BaseController, SRPAuthentication):
	"""
		DBServe is used as the contact point for the java applications when speaking to
		the web service.
	"""

	def available(self): 
		""" special URL, doesn't have database assigned, not verfication required """
		response.headers['Content-type'] = 'text/plain'
		data = ""
		for db in self._databaseList():
			data += "%s %s %s\n" % (db.name, db.locked and "1" or "0", db.archived and "1" or "0")
		return data
		

	def __before__(self):
		action = self.routingargs.get('action', '')
		session['mark'] = 1
		session.save() # force cookie out
		if action in ['available', 'srp', 'authenticate']:
			return
		if not self.verify(request.body):
			abort(401, headers={'WWW-Authenticate':'srp users="%s:series", loc="%s"' % (self.database, url_for(action='srp'))})


	def download(self):
		if self.settings.locked:
			log.warning("Download request for %s, but it is locked" % (self.database))
			abort(404, "Database locked, unavailable for download")
		self.settings.locked = True
		self.settings.save(self.session)
		self.session.commit()
		return self.copy()


	def copy(self):
		response.headers['Content-type'] = 'application/octet-stream'
		fp = open(self.databasePath(self.database), 'rb')
		data = fp.read()
		log.info("Read in database file of %d bytes", len(data))
		fp.close()
		return data


	def upload(self):
		dbpost = request.POST['db']
		out = open(self.databasePath(self.database), 'wb')
		shutil.copyfileobj(dbpost.file, out)
		dbpost.file.close()
		out.close()
		
		engine = create_engine('sqlite:///%s' % self.databasePath(self.database))
		self.session.bind = engine
		Registration.updateFromRuns(self.session)
		self.session.sqlmap("TRACKCLEAR", [])
		self.settings = Settings()
		self.settings.load(self.session)
		self.settings.locked = False
		self.settings.save(self.session)
		self.session.commit()
		return "Complete"


	def available(self):
		response.headers['Content-type'] = 'text/plain'
		data = ""
		for db in self._databaseList(archived=False):
			data += "%s %s %s\n" % (db.name, db.locked and "1" or "0", db.archived and "1" or "0")
		return data
		

	def sqlmap(self):
		try:
			stream = DataInput(request.environ['wsgi.input'].read(int(request.environ['CONTENT_LENGTH'])))
			ret = ""
			while stream.dataAvailable() > 0:
				(type, key, values) = Codec.decodeRequest(stream)
				if key in ['TRACK']:
					raise Exception("You are trying to track database changes over a web connection, switch to direct file\n\n")

				log.debug("sqlmap request %s" % (key))
				if type == Codec.SELECT:
					ret = Codec.encodeResults(self.session.sqlmap(key, values))
				elif type == Codec.UPDATE:
					ret = Codec.encodeLastId(self.session.sqlmap(key, values).lastrowid)
				elif type == Codec.FUNCTION:
					if hasattr(self, key):
						ret = getattr(self, key)(*values)
					else:
						raise Exception("Unknown FUNCTION call %s" % (key))
				else:
					raise Exception("Unknown call type %s" % (type))

			self.session.commit()
			return ret

		except Exception, e:
			self.session.rollback()
			msg = Exception.__str__(e)
			filename, lineno, name, line = traceback.extract_tb(sys.exc_info()[2])[-1]
			return Codec.encodeError(filename, lineno, msg)


	# FUNCTION calls
	def GetCarAttributes(self, attr):
		rs = self.session.connection().execute(
			"select distinct %s from cars where LOWER(%s)!=%s and UPPER(%s)!=%s order by %s collate nocase" \
				% tuple([attr]*6))
		return Codec.encodeResults(rs)

	def UpdateClass(self, eventid, course, classcode, carid):
		UpdateClassResults(self.session, eventid, course, classcode, carid)
		return Codec.encodeLastId(-1)

