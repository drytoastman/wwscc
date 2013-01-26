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
	
	# The ErrorController route (handles 404/500 error pages); it should
	# likely stay at the top, ensuring it can always be resolved
	map.connect('/error/{action}', controller='error')
	map.connect('/error/{action}/{id}', controller='error')

	# default to results
	map.connect('/', controller='results')

	# listing for doc directroy
	map.connect('/doc', controller='doc')

	# db is its own thing
	map.connect('/db/{database}/nocache/{name}', controller='db', action='nocache')
	map.connect('/db/{database}/{name}', controller='db')

	# dbserve and register don't use eventid
	map.connect('/dbserve/available', controller='dbserve', action='available')
	map.connect('/dbserve/{database}/{action}', controller='dbserve')
	map.connect('/registernew/{database}/{action}', controller='registernew')
	map.connect('/registerold/{database}/{action}', controller='registerold')
	map.connect('/register/{database}/{action}', controller='register')
	map.connect('/register/{database}/{action}/{other}', controller='register')

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
