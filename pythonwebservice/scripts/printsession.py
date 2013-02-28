#!/usr/bin/env python

import pickle
import pprint
import sys

if len(sys.argv) < 2:
	print "\nUsage: %s <filename>\n" % sys.argv[0]
	print "\tPretty print the session info in <newfilename>\n\n"
	sys.exit(0)

fp = open(sys.argv[1], 'r')
s = fp.read()
fp.close()

obj = pickle.loads(s)
pprint.pprint(obj)
