
from collections import OrderedDict
import json

from flask import g


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


SUMPART = "sum(('x' || substring(t.rowhash, {}, 8))::bit(32)::bigint)"
SUMS = "{}, {}, {}, {}".format(SUMPART.format(1), SUMPART.format(9), SUMPART.format(17), SUMPART.format(25))


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

def get(request):
    with g.db.cursor() as cur:
        cur.execute
