
from flask import g

class Settings(object):

    BOOLS  = ["locked", "superuniquenumbers", "indexafterpenalties", "usepospoints"]
    INTS   = ["largestcarnumber", "dropevents", "minevents"]
    FLOATS = []
    STRS   = ["pospointlist", "champsorting", "seriesname", "sponsorlink", "schema", "parentseries", "classinglink"]
 

    def __init__(self, **initial):
        self.__dict__.update({
            'locked': False,
            'superuniquenumbers': False,
            'indexafterpenalties': False,
            'usepospoints': False,
    
            'largestcarnumber': 1999,
            'dropevents': 2,
            'minevents': 0,

            'pospointlist': "20,16,13,11,9,7,6,5,4,3,2,1",
            'champsorting': "",
            'seriesname': "",
            'sponsorlink': "",
            'schema': "missing",
            'parentseries': "",
            'classinglink': ""
        })
        self.__dict__.update(initial)


    def __setattr__(self, key, val):
        if val is None:
            log.warning("Trying to set %s to None, ignoring" % key)
            return

        if key in Settings.INTS + Settings.FLOATS:
            conv = str(val)
        elif key in Settings.BOOLS:
            conv = val and "1" or "0"
        else:
            conv = val

        self.__dict__[key] = conv
        with g.db.cursor() as cur:
            cur.execute("update settings set val=%s,modified=now() where name=%s", (conv, key))


    @classmethod
    def get(cls):
        ret = Settings()
        with g.db.cursor() as cur:
            cur.execute("select * from settings")
            for row in cur.fetchall():
                n = row['name']
                v = row['val']
                if n in Settings.INTS:
                    ret.__dict__[n] = int(v)
                elif n in Settings.FLOATS:
                    ret.__dict__[n] = float(v)
                elif n in Settings.BOOLS:
                    ret.__dict__[n] = (v == "1")
                else:
                    ret.__dict__[n] = v
        return ret

    def getPublicFeed(self):
        """ Return a single level dict of the attributes and values to create a feed for this object """
        d = dict()
        for k,v in self.__dict__.items():
            if k[0] == '_' or v is None: continue
            d[k] = v
        return d


