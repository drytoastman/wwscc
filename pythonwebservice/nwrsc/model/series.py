
import collections
import operator
import re
from flask import g

class Series(object):
    ACTIVE   = 1
    ARCHIVED = 0
    INVALID  = -1
    UNKNOWN  = -2

    @classmethod
    def type(cls, series):
        with g.db.cursor() as cur:
            cur.execute("select schema_name from information_schema.schemata where schema_name=%s", (series,))
            if cur.rowcount > 0: return Series.ACTIVE
            cur.execute("select count(1) from results where series=%s", (series,))
            if cur.fetchone()[0] >= 2: return Series.ARCHIVED
        return Series.INVALID

    @classmethod
    def active(cls):
        with g.db.cursor() as cur:
            cur.execute("select schema_name from information_schema.schemata")
            return [x[0] for x in cur.fetchall() if x[0] not in ('pg_catalog', 'information_schema', 'public')]

    @classmethod
    def list(cls):
        with g.db.cursor() as cur:
            cur.execute("select schema_name from information_schema.schemata union select series from results")
            serieslist = [x[0] for x in cur.fetchall() if x[0] not in ('pg_catalog', 'information_schema', 'public')]

            lists = {}
            other = []
            for series in serieslist:
                try:
                    year = re.search('\d{4}', series).group(0)
                    if year not in lists: lists[year] = list()
                    lists[year].append(series)
                except:
                    other.append(series)

            ret = collections.OrderedDict()
            for key in sorted(lists.keys(), reverse=True):
                ret[key] = sorted(lists[key])
            if len(other) > 0:
                ret['Other'] = other
            return ret

