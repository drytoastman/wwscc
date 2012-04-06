"""The base Controller API

Provides the BaseController class for subclassing.
"""
from pylons.controllers import WSGIController
from pylons import config, request
from webob.exc import HTTPNotFound

from nwrsc.model import Session, metadata, Settings, SCHEMA_VERSION
from nwrsc.model.conversions import convert as dbconversion
from sqlalchemy import create_engine
from sqlalchemy.pool import NullPool

import os
import glob
import logging
log = logging.getLogger(__name__)


class BeforePage(Exception):
	def __init__(self, data):
		self.data = data

class BaseController(WSGIController):

	def databasePath(self, database):
		return os.path.join(config['seriesdir'], '%s.db' % (database))

	def databaseSelector(self):
		c.files = map(os.path.basename, glob.glob('%s/*.db' % (config['seriesdir'])))
		return render_mako('/databaseselect.mako')

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
		log.debug("process (%s)" % (environ['PATH_INFO']))

		self.srcip = request.environ.get("X_FORWARDED_FOR", request.environ["REMOTE_ADDR"]) 
		self.routingargs = environ['wsgiorg.routing_args'][1]
		self.database = self.routingargs.get('database', None)

		if self.database is None:  # no database specified yet, allow selection later by controller
			engine = create_engine('sqlite:///:memory:', poolclass=NullPool)
		else:
			dbpath = self._findDatabase()
			if dbpath is not None:
				engine = create_engine('sqlite:///%s' % dbpath, poolclass=NullPool)
			else:
				return HTTPNotFound()

		self.session = Session()
		self.session.bind = engine
		metadata.bind = engine

		self.settings = Settings()
		if self.database is not None:
			self.settings.load(self.session)
			if self.settings.schema != SCHEMA_VERSION:
				dbconversion(self.session)

		try:
			try:
				return WSGIController.__call__(self, environ, start_response)
			except BeforePage, p:
				start_response('200 OK', [('content-type', 'text/html')], None)
				return p.data
		finally:
			Session.remove()

