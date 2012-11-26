"""Pylons middleware initialization"""
from beaker.middleware import CacheMiddleware, SessionMiddleware
from paste.cascade import Cascade
from paste.registry import RegistryManager
from paste.urlparser import StaticURLParser
from paste.deploy.converters import asbool
from pylons import config
from pylons.middleware import ErrorHandler, StatusCodeRedirect
from pylons.wsgiapp import PylonsApp
from routes.middleware import RoutesMiddleware

from nwrsc.config.discovery import DatabaseAnnouncer
from nwrsc.config.environment import load_environment
from nwrsc.lib.gzipmiddleware import GzipMiddleware

from weberror.errormiddleware import ErrorMiddleware
import os.path

class PrintErrors(ErrorMiddleware):
	def exception_handler(self, exc, environ):
		ErrorMiddleware.exception_handler(self, exc, environ)
		line = ''
		name = ''
		file = ''
		tb = exc[2]
		while tb is not None:
			if tb.tb_next is None:
				line = tb.tb_lineno
				co = tb.tb_frame.f_code
				file = co.co_filename
				name = co.co_name
			tb = tb.tb_next
		return '<pre>Exception %s in %s (%s line %s)</pre>' % (exc[1], name, os.path.basename(file), line)


def make_app(global_conf, full_stack=True, **app_conf):
	"""Create a Pylons WSGI application and return it

	``global_conf``
		The inherited configuration for this application. Normally from
		the [DEFAULT] section of the Paste ini file.

	``full_stack``
		Whether or not this application provides a full WSGI stack (by
		default, meaning it handles its own exceptions and errors).
		Disable full_stack when this application is "managed" by
		another WSGI middleware.

	``app_conf``
		The application's local configuration. Normally specified in
		the [app:<name>] section of the Paste ini file (where <name>
		defaults to main).

	"""
	# Configure the Pylons environment
	load_environment(global_conf, app_conf)

	# The Pylons WSGI app
	app = PylonsApp()
	
	# Routing/Session/Cache Middleware
	app = RoutesMiddleware(app, config['routes.map'])
	app = SessionMiddleware(app, config)
	app = CacheMiddleware(app, config)
	
	if asbool(full_stack):
		# Display error documents for 401, 403, 404 status codes (and 500 when debug is disabled)
		if asbool(config['debug']):
			app = ErrorHandler(app, global_conf, **config['pylons.errorware'])
		else:
			app = PrintErrors(app, global_conf, **config['pylons.errorware'])

		app = StatusCodeRedirect(app)

	# Establish the Registry for this application
	app = RegistryManager(app)

	# Static files (If running in production, and Apache or another web 
	# server is handling this static content, remove the following 2 lines)
	static_app = StaticURLParser(config['pylons.paths']['static_files'])
	app = Cascade([static_app, app])
	app = GzipMiddleware(app, compresslevel=5)

	if asbool(config.get("nwrsc.onsite", False)):
		announcer = DatabaseAnnouncer(config.get('seriesdir', 'missing'))
		announcer.start()

	return app
