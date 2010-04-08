#!/usr/bin/env python

# Used to convert older nwrsc database schemas into the new version.
# This script can be retired once everything is stable on the new version

from nwrsc.model import *
from sqlalchemy import create_engine
from datetime import datetime
import sys, os
import pyodbc

try:
	import sqlite3
except:
	from pysqlite2 import dbapi2 as sqlite3

def formatdate(d):
	try:
		dt = datetime.fromtimestamp(int(d)/1000.0)
		return "%4.4d-%2.2d-%2.2d" % (dt.year, dt.month, dt.day)
	except:
		return "2000-01-01"

def formatdatetime(d):
	try:
		dt = datetime.fromtimestamp(int(d)/1000.0)
		return "%4.4d-%2.2d-%2.2d %2.2d:%2.2d:%2.2d.0" % (dt.year, dt.month, dt.day, dt.hour, dt.minute, dt.second)
	except:
		return "2000-01-01 12:01:01.0"

def formattime(d):
	try:
		dt = datetime.fromtimestamp(int(d)/1000.0)
		return "%2.2d:%2.2d:%2.2d.0" % (dt.hour, dt.minute, dt.second)
	except:
		return "12:01:01.0"


old = sys.argv[1]
new = sys.argv[2]

try:
	os.remove(new)
except OSError, x:
	if x.errno != 2:
		raise

metadata.bind = create_engine('sqlite:///' + new)
metadata.create_all()
session = Session()

olddb = pyodbc.connect('DRIVER={Microsoft Access Driver (*.mdb)};DBQ='+old, autocommit=True)
cursor = olddb.cursor()

# Create classes and indexes manually 

# Drivers
getcur.execute("select * from drivers")
for old in getcur:
	nd = Driver()
	nd.id = old['DrID']
	nd.firstname = old['FirstName']
	nd.lastname = old['LastName']
	nd.email = old['Email']
	nd.address = old['Address']
	nd.city = old['City']
	nd.state = old['State']
	nd.zip = old['Zip']
	nd.homephone = old['HomePhone']
	nd.workphone = old['WorkPhone']
	nd.brag = old['Brag']
	nd.sponsor = old['Sponsor']
	nd.membership = old['MemberNumber']
	session.add(nd)

# Cars
getcur.execute("select * from cars")
for old in getcur:
	nc = Car()
	nc.id = old['CarID']
	nc.year = old['CarYear']
	nc.make = old['Make']
	nc.model = old['Model']
	nc.color = old['Color']
	nc.number = old['CarNumber']
	nc.driverid = old['DrID']
	nc.classcode = old['ClassName']
	nc.indexcode = old['IndexClass']
	session.add(nc)

"""
# Events
getcur.execute("select * from events")
for old in getcur:
	ne = Event()
	ne.id = 
	ne.password = ""
	ne.name = 
	ne.date = 
	ne.location = 
	ne.sponsor = 
	ne.host = 
	ne.chair = 
	ne.designer = 
	ne.ispro = False
	ne.courses =
	ne.runs = 
	ne.regopened = 
	ne.regclosed = 
	ne.perlimit = 0
	ne.totlimit = 0
	ne.paypal = ""
	ne.snail = ""
	ne.cost = 25
	ne.notes = ""
	session.add(ne)

# Runs
getcur.execute("select * from runs")
for old in getcur:
	nr = Run()
	nr.eventid =
	nr.carid =
	nr.course =
	nr.run =
	nr.cones = 
	nr.gates = 
	nr.status = 
	nr.reaction = 0.0
	nr.sixty = 0.0
	nr.seg1 = 0.0
	nr.seg2 = 0.0
	nr.seg3 = 0.0
	nr.seg4 = 0.0
	nr.seg5 = 0.0
	nr.raw = 
	nr.net = 0.0 # Recalc later
	nr.rorder = 1 # Recalc later
	nr.norder = 1 # Recalc later

# RunOrder
putcur.execute("insert into new.runorder (eventid, course, rungroup, carid, row) select eventid,course,rungroup,carid,row from old.runorder")
"""

# Save (commit) the changes
getcur.close()
olddb.close()
session.commit()


