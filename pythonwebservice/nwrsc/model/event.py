
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

