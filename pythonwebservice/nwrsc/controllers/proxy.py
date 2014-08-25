
from pylons.controllers import WSGIController
from pylons.controllers.util import abort
from pylons import config
import urllib
import logging
import time


log = logging.getLogger(__name__)


class ProxyController(WSGIController):
	""" Skip any database stuff as this guy just forwards requests on """

	def timer(self, value):
		try :
			if config['nwrsc.onsite']:
				f = urllib.urlopen("http://127.0.0.1:8080/timer/%s" % value);
				return f.read()
			else:
				time.sleep(3)
				abort(403, "not an onsite server")

		except EnvironmentError, e:
			time.sleep(1) # slow down any out of control loops
			abort(404, str(e))
			

