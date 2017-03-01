
from flask import g
from .base import AttrBase

class Event(AttrBase):

    """
    def _get_count(self):
        from registration import Registration
        return object_session(self).query(Registration.id).filter(Registration.eventid==self.id).count()
    count = property(_get_count)

    def _get_drivers_count(self):
        return object_session(self).execute("select count(distinct(c.driverid)) from cars as c, registered as r where r.carid=c.id and r.eventid=%d"%self.id).fetchone()[0]
    drivercount = property(_get_drivers_count)
    """

    @classmethod
    def get(cls, eventid):
        with g.db.cursor() as cur:
            cur.execute("select * from events where eventid=%s", (eventid,))
            return cls(**cur.fetchone())

    def getPublicFeed(self):
        d = dict()
        for k,v in self.__dict__.iteritems():
            if v is None or k in ('paypal', 'snail', 'cost'):
                continue
            d[k] = v
        return d

    def getSegments(self):
        return map(int, str(self.attr.get('segments', '')).strip().split(','))

    def getSegmentCount(self):
        return len(self.getSegments())

    def getCountedRuns(self):
        if self.countedruns <= 0:
            return 999
        else:
            return self.countedruns

