#!/usr/bin/env python

import sys
import os
import datetime
from nwrsc.model import *
from sqlalchemy import create_engine

def extract(sourcefile, destdir):
	try:
		os.makedirs(destdir)
	except OSError:
		pass


	print "Extract %s to %s" % (sourcefile, destdir)
	metadata.bind = create_engine('sqlite:///%s' % sourcefile)
	session = Session()

	for d in session.query(Data):
		if d.mime.startswith('text/'):
			fp = open(os.path.join(destdir, d.name), 'w')
			fp.write(d.data)
			fp.close()

	session.close()


if len(sys.argv) < 3:
	print "\nUsage: %s <sourcedirectory> <outputdirectory>\n" % sys.argv[0]
	print "\tExtracts the data files of all databases in <sourcedirectory> into <outputdirectory>/<dbname>/\n\n"
	sys.exit(0)

sourcedir = sys.argv[1]
destdir = sys.argv[2]

for dbfile in os.listdir(sourcedir):
	if not dbfile.endswith('.db'):
		continue
	outdir = os.path.join(destdir, os.path.basename(dbfile)[:-3])
	extract(os.path.join(sourcedir,dbfile), outdir)


