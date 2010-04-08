
from mako import exceptions
from mako.template import Template
from mako.lookup import TemplateLookup
from nwrsc.model import Session, Data 

from pylons import tmpl_context as c, request
import logging

log = logging.getLogger(__name__)

class DatabaseLookup(TemplateLookup):

	def get_template(self, uri):
		""" Look for URI with db prefix indicating we need to check/load from the database """
		if uri.startswith('db:'):
			database = request.environ.get('wsgiorg.routing_args', (None,{}))[1].get('database', None)
			name = uri[3:]
			uri = "db:%s:%s" % (database, name)  # Create a more unique URI for caching
			# Try and load the template from the database
			format = Session.query(Data).get(name)
			if format is not None:
				text = str(format.data)
				dbhash = hash(text)
	
				# Load the hash value from the cached version (if it exists)
				try:
					cachehash = hash(TemplateLookup.get_template(self, uri).source)
				except exceptions.TemplateLookupException, e:
					cachehash = -1
	
				# If the hash values are different, recompile and place the new version in the cache
				if cachehash != dbhash:
					log.debug("compile %s" % (uri))
					self.put_string(uri, text)
			else:
				c.missingtemplate = uri
				log.error("Cant locate template for DB uri '%s'" % uri)
				uri = '/missing.mako' # switch to missing template
			Session.close()
					
		# Default falls back to regular lookup but in the DB case, something should now be in the cache
		return TemplateLookup.get_template(self, uri)


