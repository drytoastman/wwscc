#!/usr/bin/env python

import sys
import datetime
from nwrsc.model import *
from sqlalchemy import create_engine

today = datetime.date.today()

metadata.bind = create_engine('sqlite:///%s' % sys.argv[1])
metadata.create_all()

session = Session()
session.add(Event(name="testevent", date=today, regopened=today, regclosed=today, courses=1, runs=4, countedruns=0))
session.add(Class(code="PRO1", carindexed=True))
session.add(Index(code="I1", value="0.970"))
session.add(Index(code="I2", value="0.980"))
settings = Settings()
settings.schema = SCHEMA_VERSION
settings.save(session)
session.commit()

