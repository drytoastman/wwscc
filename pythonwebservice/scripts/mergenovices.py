#!/usr/bin/env python

import sys
import os
import csv
import datetime
from nwrsc.model import *
from sqlalchemy import create_engine

if len(sys.argv) < 3:
	print "\nUsage: %s <sourcefile> <databasefile> <eventid>\n" % sys.argv[0]
	print "\tMerges the data from <sourcefile> into <databasefile> and registers for event <eventid>\n\n"
	sys.exit(0)

sourcecsv = sys.argv[1]
metadata.bind = create_engine('sqlite:///%s' % sys.argv[2])
eventid = int(sys.argv[3])
session = Session()
number = 500

with open(sys.argv[1], 'rb') as csvfp:
	csvreader = csv.DictReader(csvfp)
	
	for row in csvreader:
		query = session.query(Driver)
		query = query.filter(Driver.firstname.like(row['First Name'])) # no case compare
		query = query.filter(Driver.lastname.like(row['Last Name'])) # no case compare
		query = query.filter(Driver.email.like(row['E-mail'])) # no case compare
		driver = query.first()
		if driver is not None:
			print "found match for " + row['Last Name']
		else:
			print "enter data for " + row['Last Name']
			driver = Driver()
			driver.firstname = row['First Name']
			driver.lastname = row['Last Name']
			driver.email = row['E-mail']
			driver.membership = row['Member #']
			driver.address = row['Address 1']
			driver.city = row['City']
			driver.state = row['State']
			driver.phone = row['Home Phone']
			driver.zip = row['Zip Code']
			session.add(driver)
			session.commit()

		query = session.query(Car)
		query = query.filter(Car.driverid==driver.id)
		query = query.filter(Car.year.like(row['Year']))
		query = query.filter(Car.make.like(row['Make']))
		query = query.filter(Car.model.like(row['Model']))
		car = query.first()

		if car is not None:
			print "found match for " + row['Model']
		else:
			print "enter data for " + row['Model']
			car = Car()
			car.driverid = driver.id
			car.year = row['Year']
			car.make = row['Make']
			car.model = row['Model']
			car.color = ""
			car.number = number
			car.classcode = "NOVAM"
			car.indexcode = ""
			number += 1
			session.add(car)
			session.commit()

		reg = Registration(eventid, car.id)
		try:
			session.add(reg)
			session.commit()
			print "registered %d" % car.id
		except:
			session.rollback()
			print "already registered"

session.close()


