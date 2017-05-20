
from collections import OrderedDict
import datetime
import json
import logging

TABLES =  OrderedDict({
    'settings':        ['name'],
    'indexlist':       ['indexcode'],
    'drivers':         ['driverid'],
    'events':          ['eventid'],
    'classlist':       ['classcode'],
    'classorder':      ['eventid', 'classcode', 'rungroup'],
    'cars':            ['carid'],
    'registered':      ['eventid', 'carid'],
    'runorder':        ['eventid', 'course', 'rungroup', 'row'],
    'runs':            ['eventid', 'carid', 'course', 'run'],
    'challenges':      ['challengeid'],
    'challengerounds': ['challengeid', 'round'],
    'challengeruns':   ['challengeid', 'round', 'carid', 'course'],
})

log  = logging.getLogger(__name__)
SUMPART = "sum(('x' || substring(t.rowhash, {}, 8))::bit(32)::bigint)"
SUMS = "{}, {}, {}, {}".format(SUMPART.format(1), SUMPART.format(9), SUMPART.format(17), SUMPART.format(25))
LOCALTIME = datetime.datetime(9999, 1, 1)


def loadHashes():
    ret = {}
    with g.db.cursor() as cur:
        for table, pk in TABLES.items():
            md5cols = '||'.join("md5({}::text)".format(k) for k in pk+['modified'])
            cur.execute("SELECT {} FROM (SELECT MD5({}) as rowhash from {}) as t".format(SUMS, md5cols, table))
            if cur.rowcount != 1:
                raise Exception('Invalid return value for hash request')
            row = cur.fetchone()
            if row[0] is not None:
                ret[table] = int(sum(row))
    ret['all'] = sum(ret.values())
    return ret

def loadPk(table):
    if table not in TABLES:
        return {"error":"No such table " + table}
    with g.db.cursor() as cur:
        cur.execute("SELECT {} from {}".format(','.join(TABLES[table]+['modified']), table))
        return cur.fetchall()


def clearactive(db):
    with db.cursor() as cur:
        cur.execute("UPDATE mergeservers SET active=FALSE")

def newserver(db, obj):
    with db.cursor() as cur:
        cur.execute("INSERT INTO mergeservers (serverid, active, lastmerge, attr) VALUES (%s,%s,%s,%s)",
            (obj['serverid'], obj['active'], obj['lastmerge'], json.dumps(obj['attr'])))

def updateserver(db, obj):
    with db.cursor() as cur:
        cur.execute("UPDATE mergeservers SET active=%s, lastmerge=%s, attr=%s where serverid=%s", 
            (obj['active'], obj['lastmerge'], json.dumps(obj['attr']), obj['serverid']))

def getserver(db, serverid):
    with db.cursor() as cur:
        cur.execute("SELECT * FROM mergeservers WHERE serverid=%s", (serverid,))
        if cur.rowcount == 0:
            return None
        return cur.fetchone()

def activeservers(db):
    with db.cursor() as cur:
        cur.execute("SELECT * FROM mergeservers WHERE active=TRUE")
        return cur.fetchall()

def getlocalhost(db):
    with db.cursor() as cur:
        cur.execute("SELECT * FROM mergeservers WHERE lastmerge>=%s", (LOCALTIME,))
        if cur.rowcount == 0:
            return None
        if cur.rowcount > 1:
            log.error("More than one local host in the merge table")
        return cur.fetchone()

