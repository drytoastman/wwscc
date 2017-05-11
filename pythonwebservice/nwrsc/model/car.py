import uuid
import json

from flask import g
from .base import AttrBase

class Car(AttrBase):
    toplevel = ['carid', 'driverid', 'classcode', 'indexcode', 'number', 'useclsmult']

    def new(self, driverid):
        with g.db.cursor() as cur:
            newid = uuid.uuid1()
            self.cleanAttr()
            cur.execute("INSERT INTO cars (carid,driverid,classcode,indexcode,number,useclsmult,attr) VALUES (%s,%s,%s,%s,%s,%s,%s)",
                        (newid, driverid, self.classcode, self.indexcode, self.number, self.useclsmult, json.dumps(self.attr)))
            g.db.commit()

    def update(self, verifyid):
        with g.db.cursor() as cur:
            self.cleanAttr()
            cur.execute("UPDATE cars SET classcode=%s,indexcode=%s,number=%s,useclsmult=%s,attr=%s,modified=now() where carid=%s and driverid=%s",
                       (self.classcode, self.indexcode, self.number, self.useclsmult, json.dumps(self.attr), 
                        self.carid, verifyid))
            g.db.commit()

    @classmethod
    def delete(cls, carid, verifyid):
        with g.db.cursor() as cur:
            cur.execute("DELETE FROM cars WHERE carid=%s and driverid=%s", (carid, verifyid))
            g.db.commit()

    @classmethod
    def getForDriver(cls, driverid):
        return cls.getall("select * from cars where driverid=%s order by classcode,number", (driverid,))

    @classmethod
    def usedNumbers(cls, driverid, classcode, superunique=False):
        with g.db.cursor() as cur:
            if superunique:
                cur.execute("select distinct number from cars where number not in (select number from cars where driverid = %s)", driverid)
            else:
                cur.execute("select distinct number from cars where classcode=%s and number not in (select number from cars where classcode=%s and driverid=%s)", (classcode, classcode, driverid))
            return [x[0] for x in cur.fetchall()]


