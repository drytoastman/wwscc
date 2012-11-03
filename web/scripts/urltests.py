#!/usr/bin/env python

import subprocess

IP="127.0.0.1"
DB="save"
failed = 0

def testURL(url):
	global failed
	url = url%DB
	ret = subprocess.call(["wget", "-q", "http://%s/%s" % (IP,url)])
	if ret != 0:
		print "%s FAILED" % (url)
		failed += 1
	else:
		print "%s OK" % (url)


testURL("results/%s/1/")
testURL("results/%s/1/all")
testURL("results/%s/1/champ")

print "================="
print "total failures %d" % failed
