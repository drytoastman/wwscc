from collections import defaultdict
import logging
import sys
import re

from flask import g

from .base import AttrBase
from nwrsc.lib.misc import csvlist

log = logging.getLogger(__name__)

class Class(AttrBase):

    def getCountedRuns(self):
        if self.countedruns <= 0:
            return 999
        else:
            return self.countedruns

    @classmethod
    def get(cls, code):
        with g.db.cursor() as cur:
            cur.execute("select * from classlist where classcode=%s", (code,))
            return Class(**cur.fetchone())

    @classmethod
    def getAll(cls):
        with g.db.cursor() as cur:
            cur.execute("select * from classlist order by classcode")
            return [Class(**x) for x in cur.fetchall()]

    @classmethod
    def activeClasses(cls, eventid):
        with g.db.cursor() as cur:
            cur.execute("select distinct x.* from classlist as x, cars as c, runs as r where r.eventid=%s and r.carid=c.carid and c.classcode=x.classcode", (eventid,))
            active = [Class(**x) for x in cur.fetchall()]
            cur.execute("select c.classcode from cars as c, runs as r where r.eventid=%s and r.carid=c.carid and c.classcode='UKNWN'", (eventid,))
            if cur.rowcount > 0:
                active.append(PlaceHolder())
            return sorted(active, key=lambda x:x.classcode)


class Index(AttrBase):
    @classmethod
    def getAll(cls):
        with g.db.cursor() as cur:
            cur.execute("select * from indexlist order by indexcode")
            return [Index(**x) for x in cur.fetchall()]


class PlaceHolder(object):
    def __init__(self):
        self.indexcode = ""
        self.classmultiplier = 1.0
        self.countedruns = 0
        self.usecarflag = False
        self.carindexed = False

class ClassData(object):

    def __init__(self, classdicts=[], indexdicts=[]):
        self.classlist = defaultdict(PlaceHolder)
        self.indexlist = dict()
        for c in classdicts:
            self.classlist[c['classcode']] = Class(**c)
        for i in indexdicts:
            self.indexlist[i['indexcode']] = Index(**i)

    @classmethod
    def get(cls):
        """ Get all the class and index data for the active series """
        ret = ClassData()
        with g.db.cursor() as cur:
            cur.execute("select * from classlist")
            for x in cur.fetchall():
                c = Class(**x)
                ret.classlist[c.classcode] = c
            cur.execute("select * from indexlist")
            for x in cur.fetchall():
                i = Index(**x)
                ret.indexlist[i.indexcode] = i
        return ret


    def indexes(self):
        ret = list(self.indexlist.keys())
        ret.remove("")
        return sorted(ret)


    def restrictedIndexes(self, classcode):

        RINDEX = re.compile(r'([+-])\((.*?)\)')
        RFLAG = re.compile(r'([+-])\[(.*?)\]')

        def globItem(item, full):
            tomatch = '^' + item.strip().replace('*', '.*') + '$'
            ret = set()
            for x in full:
                if re.search(tomatch, x):
                    ret.add(x)
            return ret
 
        def processList(results, fullset):
            ret = set(fullset)
            for ii, pair in enumerate(results):
                if pair[0] not in ('+','-'):
                    log.warning("Index limit script: excepted + or -, found {}".format(pair[0]))
                    continue
                ADD = (pair[0] == '+')
                if ii == 0 and ADD:
                    ret = set()
                for item in csvlist(pair[1]):
                    if ADD:
                        ret |= globItem(item, fullset)
                    else:
                        ret -= globItem(item, fullset)
            return ret

        if classcode not in self.classlist or not self.classlist[classcode].caridxrestrict:
            return ([], [])
        full = self.classlist[classcode].caridxrestrict.replace(" ", "")
        idxlist = list(self.indexlist.keys())
        idxlist.remove("")
        indexrestrict = processList(RINDEX.findall(full), idxlist)
        flagrestrict = processList(RFLAG.findall(full), idxlist)
        if len(indexrestrict) == len(idxlist):
            indexrestrict = []
        if len(flagrestrict) == len(idxlist):
            flagrestrict = []
        return (indexrestrict, flagrestrict)


    def getCountedRuns(self, classcode):
        try:
            return self.classlist[classcode].getCountedRuns()
        except:
            return 999
        
    def getIndexStr(self, car):
        indexstr = car.indexcode or ""
        try:
            cls = self.classlist[car.classcode]
            if cls.indexcode != "":
                indexstr = cls.indexcode

            if cls.classmultiplier < 1.000 and (not cls.usecarflag or car.useclsmult):
                indexstr = indexstr + '*'
        except:
            pass
        return indexstr


    def getEffectiveIndex(self, car): 
        indexval = 1.0
        try:
            cls = self.classlist[car.classcode]

            if cls.indexcode != "":
                indexval *= self.indexlist[cls.indexcode].value

            if cls.carindexed and car.indexcode:
                indexval *= self.indexlist[car.indexcode].value

            if cls.classmultiplier < 1.000 and (not cls.usecarflag or car.useclsmult):
                indexval *= cls.classmultiplier

        except Exception as e:
            log.warning("getEffectiveIndex(%s,%s,%s) failed: %s" % (car.classcode, car.indexcode, car.useclsmult, e))

        return indexval


