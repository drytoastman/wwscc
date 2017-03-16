import logging
from flask import g
from .base import AttrBase, Entrant

log = logging.getLogger(__name__)

class Challenge(AttrBase):
    @classmethod
    def getAll(cls):
        with g.db.cursor() as cur:
            cur.execute("select * from challenges order by challengeid")
            return [cls(**x) for x in cur.fetchall()]

    """
    @classmethod
    def get(cls, challengeid):
        with g.db.cursor() as cur:
            cur.execute("select * from challenges where challengeid=%s", (challengeid,))
            return cls(**cur.fetchone())

    @classmethod
    def getForEvent(cls, eventid):
        with g.db.cursor() as cur:
            cur.execute("select * from challenges where eventid=%s", (eventid,))
            return [cls(**x) for x in cur.fetchall()]

    """


