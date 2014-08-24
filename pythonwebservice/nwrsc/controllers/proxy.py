
from pylons.controllers import WSGIController
from pylons.controllers.util import abort
import urllib
import logging
import time


log = logging.getLogger(__name__)


class ProxyController(WSGIController):
	""" Skip any database stuff as this guy just forwards requests on """

	def timer(self, value):
		try :
			f = urllib.urlopen("http://127.0.0.1:8080/timer/%s" % value);
			return f.read()
		except Exception, e:
			time.sleep(1) # slow down any out of control loops
			abort(404, str(e))
			

