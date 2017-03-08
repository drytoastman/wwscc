
from flask import g
from .base import AttrBase

class Event(AttrBase):

    def feedFilter(self, key, value):
        if key in ('paypal', 'snail', 'cost'):
            return None
        return value

    @classmethod
    def get(cls, eventid):
        with g.db.cursor() as cur:
            cur.execute("select * from events where eventid=%s", (eventid,))
            return cls(**cur.fetchone())

    @classmethod
    def byDate(cls):
        with g.db.cursor() as cur:
            cur.execute("select * from events order by date")
            return [cls(**x) for x in cur.fetchall()]

    """
    def _get_count(self):
        from registration import Registration
        return object_session(self).query(Registration.id).filter(Registration.eventid==self.id).count()
    count = property(_get_count)

    def _get_drivers_count(self):
        return object_session(self).execute("select count(distinct(c.driverid)) from cars as c, registered as r where r.carid=c.id and r.eventid=%d"%self.id).fetchone()[0]
    drivercount = property(_get_drivers_count)
    """

    def getSegments(self):
        val = self.attr.get('segments', '')
        sp  = val.strip().split(',').remove('')
        if sp is None:
            return []
        return [int(x) for x in sp]

    def getSegmentCount(self):
        return len(self.getSegments())

    def getCountedRuns(self):
        if self.countedruns <= 0:
            return 999
        else:
            return self.countedruns

    def __repr__(self):
        return "<Event: {}>".format(self.name)

