"""Routes configuration

The more specific and detailed routes should be defined first so they
may take precedent over the more generic routes. For more information
refer to the routes manual at http://routes.groovie.org/docs/
"""
from pylons import config
from routes import Mapper

def make_map():
	"""Create, configure and return the routes Mapper"""
	map = Mapper(directory=config['pylons.paths']['controllers'], always_scan=config['debug'])
	map.minimization = False
	
	# default to results
	map.connect('/', controller='results')

	# db is its own thing
	map.connect('/db/{database}/nocache/{name}', controller='db', action='nocache')
	map.connect('/db/{database}/{name}', controller='db')

	# controllers that don't follow the main pattern
	map.connect('/dbserve/available', controller='dbserve', action='available')
	map.connect('/dbserve/{database}/{action}', controller='dbserve')
	map.connect('/register/{database}/{action}', controller='register')
	map.connect('/register/{database}/{action}/{other}', controller='register')
	map.connect('/history', controller='history')
	map.connect('/history/{action}', controller='history')

	map.connect('/ical', controller='ical')
	map.connect('/ical/{first}/{last}/{email}/registered', controller='ical', action='registered')

	# for easier use of url_for
	map.connect('registerlink', '/ical/{first}/{last}/{email}/registered')

	# Basic matching patterns
	map.connect('/{controller}')
	map.connect('/{controller}/')
	map.connect('/{controller}/{database}')
	map.connect('/{controller}/{database}/')
	map.connect('/{controller}/{database}/{eventid}')
	map.connect('/{controller}/{database}/{eventid}/')
	map.connect('/{controller}/{database}/{eventid}/{action}')
	map.connect('/{controller}/{database}/{eventid}/{action}/{other}')

	return map
