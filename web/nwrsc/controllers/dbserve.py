import logging
import traceback
import sys
import glob
import os
import shutil

from pylons import config, request, response
from pylons.controllers.util import abort
from sqlalchemy import create_engine
from sqlalchemy.pool import NullPool

from nwrsc.lib.base import BaseController
from nwrsc.lib.codec import Codec, DataInput
from nwrsc.controllers.dblib import UpdateClassResults
from nwrsc.model import *

log = logging.getLogger(__name__)

def corename(file):
	base = os.path.basename(file)
	return os.path.splitext(base)[0]+"\n"

class DbserveController(BaseController):
	"""
		DBServe is used as the contact point for the java applications when speaking to
		the web service.
	"""

	def __before__(self):
		# Perform authentication (TBD: real authentication)		
		action = self.routingargs.get('action', '')
		if action in ['available']:
			return

		password = request.environ.get('HTTP_X_SCOREKEEPER', '')
		if password != self.settings.password:
			log.warning("Incorrect password used for %s" % (self.database))
			abort(401)


	def download(self):
		if self.settings.locked:
			log.warning("Download request for %s, but it is locked" % (self.database))
			abort(404)
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
		
		engine = create_engine('sqlite:///%s' % self.databasePath(self.database), poolclass=NullPool)
		self.session.bind = engine
		metadata.bind = engine
		self.settings = Settings()
		self.settings.load(self.session)
		self.settings.locked = False
		self.settings.save(self.session)
		self.session.commit()
		return ""


	def available(self):
		response.headers['Content-type'] = 'text/plain'
		data = ''
		for file in glob.glob('%s/*.db' % (config['seriesdir'])):
			try:
				engine = create_engine('sqlite:///%s' % file, poolclass=NullPool)
				metadata.bind = engine
				self.session.bind = engine
				self.settings = Settings()
				self.settings.load(self.session)
				name = os.path.splitext(os.path.basename(file))[0] 
				data += "%s %s %s\n" % (name, self.settings.locked and "1" or "0", self.settings.archived and "1" or "0")
				self.session.close()
			except Exception, e:
				log.error("available error with %s (%s) " % (file,e))
	
		return data
		

	def sqlmap(self):
		try:
			stream = DataInput(request.environ['wsgi.input'].read(int(request.environ['CONTENT_LENGTH'])))
			ret = ""
			while stream.dataAvailable() > 0:
				(type, key, values) = Codec.decodeRequest(stream)

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

	def UpdateClass(self, eventid, classcode, carid):
		UpdateClassResults(self.session, eventid, classcode, carid)
		return Codec.encodeLastId(-1)

