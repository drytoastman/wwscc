"""
  This is the code for all the JSON and XML feeds.  Like the results interface, everything
  should be taken from the results table so that it continues to work after old series are
  expunged.  In general, JSON goes directly to the encoder, XML has to have some meta
  information posted in it to create meaningful tags (no anonymous lists or objects)
"""

import logging
from flask import Blueprint, request, g, escape, make_response
from nwrsc.model import Result, BaseEncoder

log  = logging.getLogger(__name__)
Xml  = Blueprint("Xml", __name__)
Json = Blueprint("Json", __name__) 


@Json.route("/")
@Xml.route("/")
def eventinfo():
    info = Result.getSeriesInfo()
    if request.blueprint == 'Json':
        return json_encode(info)

    info['_type'] = 'info'
    for x in info['challenges']: x['_type'] = 'Challenge'
    for x in info['events']:     x['_type'] = 'Event'
    for x in info['classes']:    x['_type'] = 'Class'
    for x in info['indexes']:    x['_type'] = 'Index'
    return xml_encode(dict(info)) # force back to base dict type

@Json.route("/<int:eventid>")
@Xml.route("/<int:eventid>")
def eventresults():
    res = Result.getEventResults(g.eventid)
    if request.blueprint == 'Json':
        return json_encode(res)

    for entries in res.values():
        for e in entries:
            e['_type'] = 'Entrant'
            for c in e['runs']:
                for r in c:
                    r['_type'] = 'Run'
    return xml_encode(res, wrapper='classlist')

@Json.route("/champ")
@Xml.route("/champ")
def champresults():
    res = Result.getChampResults()
    if request.blueprint == 'Json':
        return json_encode(res)

    res['_type'] = 'ChampClasses'
    for y in res.values():
        if type(y) is not list: continue
        for x in y: x['_type'] = 'ChampEntrant'

    return xml_encode(res)


@Json.route("/challenge/<int:challengeid>")
@Xml.route("/challenge/<int:challengeid>")
def challenge(challengeid):
    rounds = list(Result.getChallengeResults(challengeid).values())
    if request.blueprint == 'Json':
        return json_encode(rounds)

    for rnd in rounds:
        rnd['_type'] = 'Round'
    return xml_encode(rounds, wrapper='Rounds')

@Xml.route("/<int:eventid>/scca")
def scca():
    results = Result.getEventResults(g.eventid)
    entries = list()
    for cls in results:
        for res in results[cls]:
            entries.append(dict(FirstName=res['firstname'],
                                 LastName=res['lastname'],
                                 MemberNo=res['membership'],
                                 Class=res['classcode'],
                                 Index=res['indexcode'],
                                 Pos=res['position'],
                                 CarModel="%s %s %s %s" % (res['year'], res['make'], res['model'], res['color']),
                                 CarNo="%s" % (res['number']),
                                 TotalTm="%0.3lf" % res['net'],
                                 _type='Entry'
                            ))
    return xml_encode(entries, wrapper="Entries")


def xml_encode(data, wrapper=None):
    response = make_response(XMLEncoder().encode(data, wrapper))
    response.headers['Content-type'] = 'text/xml'
    return response

def json_encode(data):
    response = make_response(BaseEncoder(indent=1, sort_keys=True).encode(data))
    response.headers['Content-type'] = 'application/json'
    return response


class XMLEncoder(object):
    """ XML in python doesn't have easy encoding or custom getter options like JSONEncoder so we do it ourselves. """
    def __init__(self, indent=False):
        self.bits = list()
        self.indent = indent

    def encode(self, data, wrapper=None):
        if wrapper:
            self.bits.append('<%s>' % wrapper)
        self.toxml(data)
        if wrapper:
            self.bits.append('</%s>' % wrapper)
        return str(''.join(self.bits))

    def toxml(self, data):
        if hasattr(data, 'getPublicFeed'): self._encodefeed(data)
        elif type(data) in (list, tuple):  self._encodelist(data)
        elif type(data) in (dict,):        self._encodedict(data)
        else:                              self._encodedata(data)

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
        print("name is %s"%data.__class__.__name__)
        self.bits.append('<%s>'  % data.__class__.__name__)
        self._encodedict(data.getPublicFeed())
        self.bits.append('</%s>' % data.__class__.__name__)

    def _encodedata(self, data):
        self.bits.append(escape(str(data)))

