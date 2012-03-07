"""The base Controller API

Provides the BaseController class for subclassing.
"""
from pylons.controllers import WSGIController
from pylons import config, request
from paste.deploy.converters import asbool

from nwrsc.model import Session, metadata, Settings, Data, SCHEMA_VERSION
from sqlalchemy import create_engine
from sqlalchemy.pool import NullPool

import time
import os
import sys
import glob

import logging
log = logging.getLogger(__name__)


class BeforePage(Exception):
	def __init__(self, data):
		self.data = data


class BaseController(WSGIController):

	def __init__(self):
		self.a = time.time()

	def copyvalues(self, src, dst):
		for k, v in src.iteritems():
			if hasattr(dst, k):
				setattr(dst, k, v)

	def loadPythonFunc(self, func, text):
		# Create place to load stuff defined in loaded code, provide limited builtins
		loadenv = dict()
		sand = dict()
		for k in ['str', 'range']:
			sand[k] = __builtins__[k]
		loadenv['__builtins__'] = sand

		# Some flailing attempt at stopping bad behaviour
		if 'import' in text:
			raise Exception("python code to load contains import, not loading")
		
		text = str(text)
		text = text.replace('\r', '')
		exec text in loadenv
		return loadenv[func]
		
	def databasePath(self, database):
		return os.path.join(config['seriesdir'], '%s.db' % (database))

	def _findDatabase(self):
		dbpath = self.databasePath(self.database)
		if os.path.exists(dbpath):
			return dbpath
		
		dblower = dbpath.lower()
		for file in glob.glob(self.databasePath('*')):
			if file.lower() == dblower:
				self.database = os.path.basename(file)[:-3]
				return file

		self.database = None
		return None

	def __call__(self, environ, start_response):
		"""Invoke the Controller"""
		# WSGIController.__call__ dispatches to the Controller method
		# the request is routed to. This routing information is
		# available in environ['pylons.routes_dict']
		log.debug("start(%s)" % (environ['PATH_INFO']))

		self.srcip = request.environ.get("X_FORWARDED_FOR", request.environ["REMOTE_ADDR"]) 
		self.routingargs = environ['wsgiorg.routing_args'][1]
		self.database = self.routingargs.get('database', None)
		dbpath = self._findDatabase()
		if dbpath is not None:
			engine = create_engine('sqlite:///%s' % dbpath, poolclass=NullPool)
		else:
			engine = create_engine('sqlite:///:memory:', poolclass=NullPool)

		self.session = Session()
		self.session.bind = engine
		metadata.bind = engine

		self.settings = Settings()
		if self.database is not None:
			self.settings.load(self.session)
			if self.settings.schema != SCHEMA_VERSION:
				start_response('200 OK', [('content-type', 'text/html')], None)
				return "Software schema verison is %s but series database is %s, database schema or software needs to be updated to match" % \
						(SCHEMA_VERSION, self.settings.schema)

		try:
			try:
				return WSGIController.__call__(self, environ, start_response)
			except BeforePage, p:
				start_response('200 OK', [('content-type', 'text/html')], None)
				return p.data
		finally:
			Session.remove()
			log.debug("finish(%s): %f" % (environ['PATH_INFO'], time.time() - self.a))

