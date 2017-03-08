import logging
from flask import Blueprint, request, g, escape, make_response
from nwrsc.model import *

log  = logging.getLogger(__name__)
Xml  = Blueprint("Xml", __name__)
Json = Blueprint("Json", __name__) 

@Json.route("/")
@Xml.route("/")
def eventlist():
    return feed_encode("eventlist", Event.byDate())

@Json.route("/classes")
@Xml.route("/classes")
def classlist():
    return feed_encode("seriesclasses", Class.getAll())

@Json.route("/indexes")
@Xml.route("/indexes")
def indexlist():
    return feed_encode("seriesindicies", Index.getAll())

@Json.route("/<int:eventid>")
@Xml.route("/<int:eventid>")
def eventresults():
    # Need to rewrap raw dict/list output into classes for XML output
    class Result(AttrBase):
        pass
    res = EventResult.get(Event.get(g.eventid))
    forencoding = dict()
    for cls in res:
        forencoding[cls] = list()
        for e in res[cls]:
            newruns = []
            for c in e['runs']:
                newcourse = []
                newruns.append(newcourse)
                for r in c:
                    newcourse.append(Run(**r))

            e['runs'] = newruns
            forencoding[cls].append(Result(**e))
            
    return feed_encode("classlist", forencoding)

@Json.route("/<int:eventid>/challenge")
@Xml.route("/<int:eventid>/challenge")
def challengelist():
    return feed_encode("challengelist", Challenge.getForEvent(g.eventid))

@Json.route("/<int:eventid>/challenge/<int:challengeid>")
@Xml.route("/<int:eventid>/challenge/<int:challengeid>")
def challenge(challengeid):
    return feed_encode("roundlist", Challenge.getResults(challengeid))

@Xml.route("/<int:eventid>/scca")
def scca():
    class Entry(AttrBase):
        pass
    results = EventResult.get(Event.get(g.eventid))
    entries = list()
    for cls in results:
        for res in results[cls]:
            entries.append(Entry(FirstName=res['firstname'],
                                 LastName=res['lastname'],
                                 MemberNo=res['membership'],
                                 Class=res['classcode'],
                                 Index=res['indexcode'],
                                 Pos=res['position'],
                                 CarModel="%s %s %s %s" % (res['year'], res['make'], res['model'], res['color']),
                                 CarNo="%s" % (res['number']),
                                 TotalTm=res['sum']))
    return feed_encode("Entries", entries)


def feed_encode(head, data):
    if request.blueprint == 'Xml':
        response = make_response(XMLEncoder().encode(head, data))
        response.headers['Content-type'] = 'text/xml'
    else:
        response = make_response(BaseEncoder(indent=1).encode(data))
        response.headers['Content-type'] = 'text/javascript'

    return response


class XMLEncoder(object):
    """ XML in python doesn't have easy encoding or custom getter options like JSONEncoder so we do it ourselves. """
    def __init__(self, indent=False):
        self.bits = list()
        self.indent = indent

    def encode(self, outertag, data):
        self.bits.append('<%s>'  % outertag)
        self.toxml(data)
        self.bits.append('</%s>' % outertag)
        return str(''.join(self.bits))

    def toxml(self, data):
        if type(data) in (list, tuple):      self._encodelist(data)
        elif hasattr(data, 'getPublicFeed'): self._encodefeed(data)
        elif type(data) in (dict,):          self._encodedict(data)
        else:                                self._encodedata(data)

    def _encodelist(self, data):
        for v in data:
            self.toxml(v)

    def _encodedict(self, data):
        for k,v in data.items():
            self.bits.append('<%s>'  % k)
            self.toxml(v)
            self.bits.append('</%s>' % k)

    def _encodefeed(self, data):
        self.bits.append('<%s>'  % data.__class__.__name__)
        self._encodedict(data.getPublicFeed())
        self.bits.append('</%s>' % data.__class__.__name__)

    def _encodedata(self, data):
        self.bits.append(escape(str(data)))

