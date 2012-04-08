import cgi
import os.path

from paste.urlparser import PkgResourcesParser
from pylons import request
from pylons.controllers.util import forward
from pylons.middleware import error_document_template
from webhelpers.html.builder import literal

from nwrsc.controllers.lib.base import BaseController

import logging
log = logging.getLogger(__name__)

class ErrorController(BaseController):
	"""Generates error documents as and when they are required.

	The ErrorDocuments middleware forwards to ErrorController when error
	related status codes are returned from the application.

	This behaviour can be altered by changing the parameters to the
	ErrorDocuments middleware in your config/middleware.py file.
	
	"""

	error_template = """<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
<title>Server Error %(code)s</title>
<style type="text/css">
        .red {
            color:#FF0000;
        }
        .bold {
            font-weight: bold;
        }
</style>
</head>
<body>
    <div id="container">
        %(message)s
    </div>
</body>
</html>

	"""

	def document(self):
		"""Render the error document"""
		req = request.environ.get('pylons.original_request')
		resp = request.environ.get('pylons.original_response')
		log.info("Processing error for %s - %s" % (req.url, resp.status))
		content = literal(resp.body) or cgi.escape(request.GET.get('message', ''))
		page = self.error_template % \
			dict(prefix=request.environ.get('SCRIPT_NAME', ''),
				 code=cgi.escape(request.GET.get('code', str(resp.status_int))),
				 message=content)
		return page

	def img(self, id):
		"""Serve Pylons' stock images"""
		return self._serve_file('/'.join(['media/img', id]))

	def style(self, id):
		"""Serve Pylons' stock stylesheets"""
		return self._serve_file('/'.join(['media/style', id]))

	def _serve_file(self, path):
		"""Call Paste's FileApp (a WSGI application) to serve the file
		at the specified path
		"""
		static = PkgResourcesParser('pylons', 'pylons')
		request.environ['PATH_INFO'] = '/%s' % path
		return static(request.environ, self.start_response)
