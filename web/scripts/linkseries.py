#!/usr/bin/env python

import sys
import uuid
from nwrsc.model import *
from sqlalchemy import create_engine

if len(sys.argv) < 2:
	print "\nUsage: %s <oldest database> <second oldest> .... <latest database>\n" % sys.argv[0]
	print "\tRewrites the parentseries for each database\n\n"
	sys.exit(0)


parentseries = ""

for db in sys.argv[1:]:
	metadata.bind = create_engine('sqlite:///%s' % db)
	print "updating %s ... " % db,
	session = Session()
	settings = Settings()
	settings.load(session)

	settings.parentseries = parentseries
	parentseries = db[:-3]

	settings.save(session)
	session.commit()
	session.close()
	print "done."

