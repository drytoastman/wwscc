"""
  This is the code for all the JSON and XML feeds.  Like the results interface, everything
  should be taken from the results table so that it continues to work after old series are
  expunged.  In general, JSON goes directly to the encoder, XML has to have some meta
  information posted in it to create meaningful tags (no anonymous lists or objects)
"""

import logging
from flask import Blueprint, request, g, escape, make_response
from nwrsc.lib.encoding import json_encode, json_raw, xml_encode
from nwrsc.model import Result, Settings 

log  = logging.getLogger(__name__)
Xml  = Blueprint("Xml", __name__)
Json = Blueprint("Json", __name__) 


@Json.route("/")
def jsoninfo():
    return json_raw(Result.getSeriesInfo(asstring=True))

@Xml.route("/")
def xmlinfo():
    info = Result.getSeriesInfo()
    info['_type'] = 'info'
    for x in info['challenges']: x['_type'] = 'Challenge'
    for x in info['events']:     x['_type'] = 'Event'
    for x in info['classes']:    x['_type'] = 'Class'
    for x in info['indexes']:    x['_type'] = 'Index'
    return xml_encode(dict(info)) # force back to base dict type


@Json.route("/<int:eventid>")
def jsonevent():
    return json_raw(Result.getEventResults(g.eventid, asstring=True))

@Xml.route("/<int:eventid>")
def xmlevent():
    res = Result.getEventResults(g.eventid)
    for entries in res.values():
        for e in entries:
            e['_type'] = 'Entrant'
            for c in e['runs']:
                for r in c:
                    r['_type'] = 'Run'
    return xml_encode(res, wrapper='classlist')


@Json.route("/champ")
def jsonchamp():
    return json_raw(Result.getChampResults(asstring=True))

@Xml.route("/champ")
def xmlchamp():
    res = Result.getChampResults()
    for y in res.values():
        for x in y: x['_type'] = 'ChampEntrant'
    res['_type'] = 'ChampClasses'
    return xml_encode(res)


@Json.route("/challenge/<int:challengeid>")
def jsonchallenge(challengeid):
    return json_encode(list(Result.getChallengeResults(challengeid).values()))

@Xml.route("/challenge/<int:challengeid>")
def xmlchallenge(challengeid):
    rounds = list(Result.getChallengeResults(challengeid).values())
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
                                 CarModel="%s %s %s %s" % (res.get('year',''), res.get('make',''), res.get('model',''), res.get('color','')),
                                 CarNo="%s" % (res['number']),
                                 TotalTm="%0.3lf" % res['net'],
                                 _type='Entry'
                            ))
    return xml_encode(entries, wrapper="Entries")

