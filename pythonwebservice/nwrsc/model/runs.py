from flask import g
from .base import AttrBase

class Run(AttrBase):

    def feedFilter(self, key, value):
        if key in ('carid', 'eventid', 'modified') or (isinstance(value, int) and value < 0):
            return None
        return value
       
    """
    def getSegment(self, idx):
        return getattr(self, "seg%d" % (idx))

    def validSegments(self, segmentlist):
        segments = list()
        for ii, segmin in enumerate(segmentlist):
            segx = getattr(self, "seg%d" % (ii+1))
            if segx < segmin:
                return [None] * len(segmentlist)
            segments.append(segx)
        return segments
    """

