#!/usr/bin/env python

# Create attendance file
import sys
import os
from nwrsc.lib.attendance import *

if os.path.exists('data.attendance'):
	sys.exit('data.attendance already exists')

data = list()
for dbfile in sys.argv[1:]:
	data.extend(loadAttendanceFromDB(dbfile))

writeAttendance('data.attendance', data)

