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
getcur = olddb.cursor()

# Create classes and indexes manually 

# Drivers
getcur.execute("select * from Drivers")
for old in getcur:
	nd = Driver()
	nd.id = old.DrID
	nd.firstname = old.FirstName
	nd.lastname = old.LastName
	nd.email = old.Email
	nd.address = old.Address
	nd.city = old.City
	nd.state = old.State
	nd.zip = old.Zip
	nd.homephone = old.HomePhone
	nd.workphone = old.WorkPhone
	nd.brag = old.Brag
	nd.sponsor = old.Sponsor
	nd.membership = old.MemberNumber
	session.add(nd)

# Cars
getcur.execute("select * from Cars")
for old in getcur:
	nc = Car()
	nc.id = old.CarID
	nc.year = old.CarYear
	nc.make = old.Make
	nc.model = old.Model
	nc.color = old.Color
	nc.number = old.CarNumber
	nc.driverid = old.DrID
	nc.classcode = old.ClassName
	nc.indexcode = old.IndexClass
	session.add(nc)

# Events
getcur.execute("select * from Event_Info")
for old in getcur:
	ne = Event()
	ne.id = old.Event_ID
	ne.password = "scca"
	ne.name = old.Event_Name
	ne.date = old.Event_Date
	ne.location = old.Event_Host
	ne.sponsor = old.Event_Sponsors
	ne.host = ""
	ne.chair = old.Event_Chair
	ne.designer = old.Event_CourseDesigner
	ne.ispro = False
	ne.courses = old.Event_NumCourses
	ne.runs = old.Event_NumRuns
	ne.regopened = old.Event_Date
	ne.regclosed = old.Event_Date 
	ne.perlimit = 2
	ne.totlimit = 0
	ne.paypal = ""
	ne.snail = ""
	ne.cost = 25
	ne.notes = ""
	session.add(ne)

# Runs
getcur.execute("select * from Event_Times")
for old in getcur:
	nr = Run()
	nr.eventid = old.Tm_Event_ID
	nr.carid = old.Tm_Car_ID
	nr.course = old.Tm_Course
	nr.run = old.Tm_Run_Nbr
	nr.cones = old.Tm_Cones
	nr.gates = old.Tm_Gates
	nr.status = old.Tm_Run_Status
	nr.reaction = 0.0
	nr.sixty = 0.0
	nr.seg1 = 0.0
	nr.seg2 = 0.0
	nr.seg3 = 0.0
	nr.seg4 = 0.0
	nr.seg5 = 0.0
	nr.raw = old.Tm_Raw_Time
	nr.net = 0.0 # Recalc later
	nr.rorder = 1 # Recalc later
	nr.norder = 1 # Recalc later
	session.add(nr)

# RunOrder
getcur.execute("select * from RunOrder")
for old in getcur:
	no = RunOrder()
	no.eventid = old.EventID
	no.course = 1
	no.rungroup = old.RunGroup
	no.carid = old.CarID
	no.row = old.RunOrder
	session.add(no)

# Classes/Indexes
getcur.execute("select * from Classes")
for old in getcur:
	if True: #old.IndexValue == 1:
		nc = Class()
		nc.code = old.Designation
		nc.descrip = old.Name
		nc.carindexed = 1
		nc.classindexed = 0
		nc.classmultiplier = 1.0
		nc.eventtrophy = 1
		nc.champtrophy = 1
		nc.numorder = old.SortOrder
		session.add(nc)
		
# Save (commit) the changes
getcur.close()
olddb.close()
session.commit()


