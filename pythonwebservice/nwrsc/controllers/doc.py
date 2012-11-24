import logging
import os

from nwrsc.controllers.lib.base import BaseController, BeforePage
from pylons import config

log = logging.getLogger(__name__)

class DocController(BaseController):
	"""
		Special controller to provide a list of the documents in the static docs
		folder.  Kind of like an apache alias without need for one.
	"""

	def index(self):
		yield "<H3>Scorekeeper Documents</H3>\n<ul>\n";
		for f in os.listdir(os.path.join(config['pylons.paths']['static_files'], 'doc')):
			if f[0] != '.':
				yield "<li><a href='/doc/%s'>%s</a></li>\n" % (f,f)
		yield "</ul>\n";

