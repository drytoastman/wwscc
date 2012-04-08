"""The base Controller API

Provides the BaseController class for subclassing.
"""
from pylons.controllers import WSGIController
from pylons.templating import render_mako
from pylons import config, request, tmpl_context as c
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

class DatabaseListing(object):
	def __init__(self, **kwargs):
		for n in ('name', 'locked', 'archived', 'mtime'):
			setattr(self, n, kwargs[n])

	def getFeed(self):
		return self.__dict__


class BaseController(WSGIController):
	""" Base controller ala Pylons base, modified for using multiple databases based on URL """


	def databasePath(self, database):
		""" Given a database name (no extension), return the full path where we expect to find it """
		return os.path.join(config['seriesdir'], '%s.db' % (database))


	def databaseSelector(self, showArchived=False, timelimit=0.0):
		"""" Common routine for other controllers, return a list of active series to select """
		c.dblist = list()
		for db in self._databaseList():
			if not showArchived and db.archived:
				continue
			if db.mtime < timelimit:
				continue
			c.dblist.append(db)
		
		return render_mako('/databaseselect.mako')


	def _databaseList(self):
		"""
			Return a list of the current databases with information on lock and archive settings
			Note, this affects the current active metadata
		"""
		ret = list()
		for path in glob.glob('%s/*.db' % (config['seriesdir'])):
			try:
				engine = create_engine('sqlite:///%s' % path, poolclass=NullPool)
				metadata.bind = engine
				self.session.bind = engine
				self.settings = Settings()
				self.settings.load(self.session)
				name = os.path.splitext(os.path.basename(path))[0] 
				mtime = os.path.getmtime(path)
				ret.append(DatabaseListing(name=name, mtime=mtime, locked=self.settings.locked, archived=self.settings.archived))
				self.session.close()
			except Exception, e:
				log.error("available error with %s (%s) " % (name,e))
	
		return ret


	def _findDatabase(self):
		""" See if we can find the database as specified, if not, check without case """
		dbpath = self.databasePath(self.database)
		if os.path.exists(dbpath):
			return dbpath
		
		dblower = dbpath.lower()
		for name in glob.glob(self.databasePath('*')):
			if name.lower() == dblower:
				self.database = os.path.basename(name)[:-3]
				return name 

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
				return HTTPNotFound() # don't know where you are going, but it stops here for me

		self.session = Session()
		self.session.bind = engine
		metadata.bind = engine

		self.settings = Settings()
		if self.database is not None:
			self.settings.load(self.session)
			# Check if we are the correct schema, update if possible
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

