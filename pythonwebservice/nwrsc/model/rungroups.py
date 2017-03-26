from collections import defaultdict, OrderedDict
from operator import attrgetter
from flask import g
from .base import Entrant


class ClassList(list):
    def __init__(self):
        list.__init__(self)
        self.numbers = set()

    def add(self, e):
        if (e.number+100)%200 in self.numbers: return False
        self.append(e)
        self.numbers.add(e.number)
        return True

class GroupOrder(OrderedDict):

    def pad(self):
        """ If the class is a odd # of entries and next class is not single, add a space """
        codes = list(self.keys())
        for ii in range(len(codes)-1):
            if len(self[codes[ii]]) % 2 != 0 and len(self[codes[ii+1]]) > 1:
                self[codes[ii]].append(Entrant())

    def number(self):
        """ Create the grid numbers for each entry """
        ii = 0
        for code in self:
            for e in self[code]:
                ii += 1
                e.grid = ii

class RunGroups(defaultdict):

    def put(self, entrant):
        cc = entrant.classcode
        for num, go in self.items():
            if cc in go:
                if not go[cc].add(entrant):
                    self[num+100][cc].add(entrant)
                return
        raise Exception("Failed to find a rungroup for {}".format(cc))

    def sort(self, key):
        for go in self.values():
            for clist in go.values():
                clist.sort(key=attrgetter(key))

    @classmethod
    def getForEvent(cls, eventid):
        ret = RunGroups(GroupOrder)
        with g.db.cursor() as cur:
            cur.execute("select * from classorder where eventid=%s order by rungroup, gorder", (eventid,))
            for x in cur.fetchall():
                ret[x['rungroup']][x['classcode']] = ClassList()
                ret[x['rungroup']+100][x['classcode']] = ClassList()
        return ret


