import logging

from pylons import request, response
from pylons.controllers.util import etag_cache
from nwrsc.lib.base import BaseController
from nwrsc.model import Data

log = logging.getLogger(__name__)

class DbController(BaseController):
	"""
		DB Controller is only used to access data files in the data table.  At this time, those are series
		images and template files
	"""

	weekdayname = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
	monthname = [None, 'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

	def index(self, name):
		log.debug("dbload: name is %s" % name)
		ret = self.session.query(Data).get(name)
		if ret is not None:
			# ETag stops unnecessary data, max-age will stop requests completely for 5 minutes
			# As data could change, make it short but still enough to make a single session nice
			# and fast
			response.headers['Content-type'] = str(ret.mime)
			response.headers['Last-Modified'] = self.rfc850(ret.mod)
			etag_cache("%s" % ret.mod)
			response.headers['Cache-Control'] = 'max-age=360' 
			response.headers.pop('Pragma', None)
			log.debug("dbload: Returning %s" % ret.name)
			return str(ret.data)
		return None


	def nocache(self, name):
		log.debug("dbload-nocache: name is %s" % name)
		ret = self.session.query(Data).get(name)
		if ret is not None:
			response.headers['Content-type'] = str(ret.mime)
			log.debug("dbload-nocache: Returning %s" % ret.name)
			return str(ret.data)
		return None


	def rfc850(self, dt):
		""" Return RFC850 in a locale independant way """
		return "%s, %02d %3s %4d %02d:%02d:%02d GMT" % (
			self.weekdayname[dt.weekday()], dt.day, self.monthname[dt.month], dt.year, dt.hour, dt.minute, dt.second)

