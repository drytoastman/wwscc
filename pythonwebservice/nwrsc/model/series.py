
import collections
import operator
import re
from flask import g

class Series(object):

    @classmethod
    def exists(cls, series):
        with g.db.cursor() as cur:
            cur.execute("select schema_name from information_schema.schemata where schema_name=%s", (series,))
            return cur.rowcount > 0

    @classmethod
    def list(cls):
        with g.db.cursor() as cur:
            cur.execute("select schema_name from information_schema.schemata order by schema_name")
            serieslist = [x[0] for x in cur.fetchall() if x[0] not in ('pg_catalog', 'information_schema', 'public')]

            lists = { 'Other': [] }
            for series in serieslist:
                try:
                    year = re.search('\d{4}', series).group(0)
                    if year not in lists: lists[year] = list()
                    lists[year].append(series)
                except:
                    lists['Other'].append(series)

            ret = collections.OrderedDict()
            for key in sorted(lists.keys()):
                ret[key] = sorted(lists[key])
            return ret

