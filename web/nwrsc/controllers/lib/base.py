"""
The base Controller API

Provides the BaseController class for subclassing.

"""

from pylons.controllers import WSGIController
from pylons.templating import render_mako
from pylons import config, request, response, tmpl_context as c
from webob.exc import HTTPNotFound

from nwrsc.model import Driver, Session, Settings, SCHEMA_VERSION
from nwrsc.model.conversions import convert as dbconversion
from sqlalchemy import create_engine

import os
import glob
import logging

log = logging.getLogger(__name__)


class BeforePage(Exception):

	def __init__(self, data):
		self.data = data


class DatabaseListing(object):

	def __init__(self, **kwargs):
		for n in ('name', 'locked', 'archived', 'parentseries', 'mtime', 'driver'):
			setattr(self, n, kwargs[n])

	def getFeed(self):
		return {'name':self.name, 'locked':self.locked, 'archived':self.archived, 'parentseries':self.parentseries, 'mtime':self.mtime}


class BaseController(WSGIController):
	""" Base controller ala Pylons base, modified for using multiple databases based on URL """


	def databasePath(self, name, mustExist=True):
		"""
			Given a database name (no extension), return the full path where we expect to find it
			Includes checks for case insensitive names.
			Returns None if there nothing to find.
		"""
		path = os.path.join(config['seriesdir'], '%s.db' % name)
		if os.path.exists(path):
			return path

		path = os.path.join(config['archivedir'], '%s.db' % name)
		if os.path.exists(path):
			return path

		name = name.lower()

		for path in glob.glob(os.path.join(config['seriesdir'], '*.db')):
			if os.path.basename(path)[:-3].lower() == name:
				return path

		for path in glob.glob(os.path.join(config['archivedir'], '*.db')):
			if os.path.basename(path)[:-3].lower() == name:
				return path

		if not mustExist:
			return os.path.join(config['seriesdir'], '%s.db' % name)

		return None


	def csv(self, filename, titles, objects):
		# CSV data, just use a template and return
		response.headers['Content-type'] = "application/octet-stream"
		response.headers['Content-Disposition'] = 'attachment;filename=%s.csv' % filename
		response.charset = 'utf8'

		# output title line
		yield ','.join(titles)
		yield '\n'

		for obj in objects:
			line = []
			for t in titles:
				s = getattr(obj, t) 
				if s is None:
					line.append("\"\"")
				elif hasattr(s, 'replace'):
					line.append("\"%s\""%s.replace('\n', ' ').replace('"', '""'))
				else:
					line.append("%s"%s)

			yield(','.join(line))
			yield('\n')



	def lineage(self, startseries, maxcount=5):
		""" Get the lineage of a paricular series based on the starting UUID up to a maximum history """
		ret = list()
		dblist = self._databaseList()
		while len(startseries) > 0 and maxcount > 0: # Max also catches missing parents and stops runaway loop
			maxcount -= 1
			for db in dblist:
				if db.name == startseries:
					ret.append(db.name)
					startseries = db.parentseries
					break

		return ret

		
	def databaseSelector(self, archived=False, timelimit=0.0):
		"""" Common routine for other controllers, return a list of active series to select """
		c.dblist = list()
		for db in self._databaseList(archived=archived):
			if db.mtime < timelimit:
				continue
			c.dblist.append(db)
		
		return render_mako('/databaseselect.mako')


	def _verifyID(self, firstname, lastname, email):
		""" return a driver object if one matches the three tuple """
		query = self.session.query(Driver)
		query = query.filter(Driver.firstname.like(firstname+'%'))
		query = query.filter(Driver.lastname.like(lastname+'%'))
		for d in query.all():
			if d.email.lower().strip() == email.lower().strip():
				return d
		return None


	def _activeSeries(self):
		return [os.path.basename(x)[:-3] for x in glob.glob('%s/*.db' % (config['seriesdir']))]


	def _databaseList(self, archived=True, driver=None):
		"""
			Return a list of the current databases with information on lock and archive settings
		"""
		ret = list()
		savedengine = self.session.bind

		paths = glob.glob('%s/*.db' % (config['seriesdir']))
		if archived:
			paths.extend(glob.glob('%s/*.db' % (config['archivedir'])))

		for path in paths:
			try:
				engine = create_engine('sqlite:///%s' % path)
				self.session.bind = engine
				settings = Settings()
				settings.load(self.session)
				name = os.path.splitext(os.path.basename(path))[0] 
				mtime = os.path.getmtime(path)
				archived = (os.path.dirname(path) == config['archivedir'])

				check = None
				if driver is not None:
					check = self._verifyID(driver.firstname, driver.lastname, driver.email)
					
				ret.append(DatabaseListing(name=name, mtime=mtime, driver=check, archived=archived,
										locked=settings.locked, parentseries=settings.parentseries))
				self.session.close()
			except Exception, e:
				log.error("available error with %s (%s) " % (name,e))
	
		self.session.bind = savedengine
		return ret


	def _loadDriverFrom(self, otherseries=None, firstname=None, lastname=None, email=None):
		"""
			Get driver entry from another series, kept in lib/base so any users of create_engine
			stay in this single file, hidden from others
		"""
		savedengine = self.session.bind
		engine = create_engine('sqlite:///%s' % self.databasePath(otherseries))
		self.session.bind = engine
		driver = self._verifyID(firstname, lastname, email)
		self.session.expunge(driver) # so we can use in another session
		self.session.bind = savedengine
		return driver.copy()  # just copy it, don't know what magic sqla wants to reset ids and mappings


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
			engine = create_engine('sqlite:///:memory:')
		else:
			dbpath = self.databasePath(self.database)
			if dbpath is not None:
				engine = create_engine('sqlite:///%s' % dbpath)
			else:
				return HTTPNotFound() # don't know where you are going, but it stops here for me

		# setup sqlalchemy session and pull in settings
		self.session = Session()
		self.session.bind = engine
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
			Session.remove() # sqlalchemy session


