
from simplejson import JSONEncoder
from pylons import response
from nwrsc.controllers.feed import FeedController

class JEncoder(JSONEncoder):
	def default(self, o):
		if hasattr(o, 'getFeed'):
			return o.getFeed()
		else:
			return str(o)

class JsonController(FeedController):
	"""
		Serve up data feeds as json
	"""
	def _encode(self, head, o):
		response.headers['Content-type'] = 'text/javascript'
		return JEncoder(indent=1).encode(o)

