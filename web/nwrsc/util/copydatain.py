#!/usr/bin/env python

# Used to convert older nwrsc database schemas into the new version.
# This script can be retired once everything is stable on the new version

import nwrsc
from nwrsc.model import *
from sqlalchemy import create_engine
import sys, os

metadata.bind = create_engine('sqlite:///' + sys.argv[1])
session = Session()

def insertfile(name, type, path):
	try:
		Data.set(session, name, open(path).read(), type)
	except Exception, e:
		print("Couldn't insert %s, %s" % (name, e))

root = nwrsc.__path__[0]
insertfile('results.css', 'text/css', os.path.join(root, 'examples/wwresults.css'))
insertfile('event.mako', 'text/plain', os.path.join(root, 'examples/wwevent.mako'))
insertfile('champ.mako', 'text/plain', os.path.join(root, 'examples/wwchamp.mako'))
insertfile('toptimes.mako', 'text/plain', os.path.join(root, 'examples/toptimes.mako'))
insertfile('classresult.mako', 'text/plain', os.path.join(root, 'examples/classresults.mako'))
insertfile('card.py', 'text/plain', os.path.join(root, 'examples/basiccard.py'))
Setting.saveDict(session, {'useevents':5, 'ppoints':'20,16,13,11,9,7,6,5,4,3,2,1'})
session.commit()

