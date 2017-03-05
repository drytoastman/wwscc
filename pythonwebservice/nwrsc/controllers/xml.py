
from flask import Blueprint, request, g
from nwrsc.model import *

Xml = Blueprint("Xml", __name__)

@Xml.route("/scca")
def scca():
    return "scca"

class XmlController(object):
	""" Provides the feed using XML encoding """

	def _encode(self, head, o):
		response.headers['Content-type'] = 'text/xml'
		self.buffer = list()
		self.buffer.append('<%s>'%head)
		self._format(o)
		self.buffer.append('</%s>'%head)
		return str(''.join(self.buffer))

	def _format(self, o):
		if type(o) in (list, tuple):
			for i in o:
				self._format(i)
		elif type(o) in (dict,):
			for k,v in o.iteritems():
				self.buffer.append('<%s>'%k)
				self._format(v)
				self.buffer.append('</%s>'%k)
		elif hasattr(o, 'getPublicFeed'):
			self.buffer.append('<%s>'%o.__class__.__name__)
			for k,v in o.getFeed().iteritems():
				self.buffer.append('<%s>'%k)
				self._format(v)
				self.buffer.append('</%s>'%k)
			self.buffer.append('</%s>'%o.__class__.__name__)
		else:
			self.buffer.append(escape(str(o)))

