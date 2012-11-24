#!/usr/bin/env python

import sys
import os
import datetime
from nwrsc.model import *
from sqlalchemy import create_engine

if len(sys.argv) < 3:
	print "\nUsage: %s <sourcedirectory> <databasefile>\n" % sys.argv[0]
	print "\tInserts the data files from <sourcedirectory> into <databasefile>\n\n"
	sys.exit(0)

sourcedir = sys.argv[1]
metadata.bind = create_engine('sqlite:///%s' % sys.argv[2])
session = Session()

for name in os.listdir(sourcedir):
	if name.endswith('.mako') or name.endswith('.py'):
		mime = 'text/plain'
	elif name.endswith('.css'):
		mime = 'text/css'
	else:
		continue

	fp = open(os.path.join(sourcedir, name), 'r')
	data = fp.read()
	fp.close()
	print "insert %s, %s, %d" % (name, mime, len(data))
	Data.set(session, name, data, mime)

session.commit()
session.close()

