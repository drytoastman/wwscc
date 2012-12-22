#!/usr/bin/env python

import sys
import logging
from nwrsc.model import *
from nwrsc.model.conversions import convert
from sqlalchemy import create_engine

if len(sys.argv) < 2:
	print "\nUsage: %s [files ... ]\n" % sys.argv[0]
	print "\tRuns the conversion process on all provided databases\n\n"
	sys.exit(0)

logging.basicConfig(level=logging.DEBUG, format="%(message)s")

for dbfile in sys.argv[1:]:
	if not dbfile.endswith('.db'):
		continue

	logging.info("converting %s", dbfile)
	session = Session()
	session.bind = create_engine('sqlite:///%s' % dbfile)
	convert(session)


