
import json
from flask import escape, make_response

def xml_encode(data, wrapper=None):
    response = make_response(XMLEncoder().encode(data, wrapper))
    response.headers['Content-type'] = 'text/xml'
    return response

def json_encode(data):
    response = make_response(JSONEncoder().encode(data))
    response.headers['Content-type'] = 'application/json'
    return response

def json_raw(data):
    response = make_response(data)
    response.headers['Content-type'] = 'application/json'
    return response

def to_json(obj):
    return JSONEncoder().encode(obj)

class JSONEncoder(json.JSONEncoder):
    """ Helper that calls getPublicFeed if available for getting json encoding """
    def default(self, o):
        if hasattr(o, 'getPublicFeed'):
            return o.getPublicFeed()
        else:
            return str(o)


class XMLEncoder(object):
    """ XML in python doesn't have easy encoding or custom getter options like JSONEncoder so we do it ourselves. """
    def __init__(self):
        self.bits = list()

    def encode(self, data, wrapper=None):
        if wrapper:
            self.bits.append('<%s>' % wrapper)
        self.toxml(data)
        if wrapper:
            self.bits.append('</%s>' % wrapper)
        return str(''.join(self.bits))

    def toxml(self, data):
        if hasattr(data, 'getPublicFeed'):    self._encodefeed(data)
        elif isinstance(data, (list, tuple)): self._encodelist(data)
        elif isinstance(data, (dict,)):       self._encodedict(data)
        else:                                 self._encodedata(data)

    def _encodelist(self, data):
        if all(isinstance(x, (int,str)) for x in data):
            self.bits.append(escape(','.join(map(str, data))))
        else:
            for v in data:
                self.toxml(v)

    def _encodedict(self, data):
        tag = data.get('_type', None)
        if tag:
            self.bits.append('<%s>'  % tag)
        for k,v in sorted(data.items()):
            if len(k) > 0 and k[0] == '_': 
                continue
            self.bits.append('<%s>'  % k)
            self.toxml(v)
            self.bits.append('</%s>' % k)
        if tag:
            self.bits.append('</%s>'  % tag)

    def _encodefeed(self, data):
        self.bits.append('<%s>'  % data.__class__.__name__)
        self._encodedict(data.getPublicFeed())
        self.bits.append('</%s>' % data.__class__.__name__)

    def _encodedata(self, data):
        self.bits.append(escape(str(data)))

