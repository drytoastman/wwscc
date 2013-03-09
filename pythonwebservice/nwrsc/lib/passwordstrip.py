
import tempfile
import sqlite3
import os
import logging

log = logging.getLogger(__name__)

def stripPasswords(sourcefile):
	""" Return a binary string output of the sourcefile with the passwords entries removed """

	# copy the source file into a tempfile
	(tempfp, tempname) = tempfile.mkstemp()
	outfp = os.fdopen(tempfp)
	infp = open(sourcefile, 'rb')
	outfp.write(infp.read())
	oufp.close()
	infp.close()

	# open the copied file and remove all the passwords entries
	conn = sqlite3.connect(tempname)
	conn.execute("delete from passwords")
	conn.close()
	infp = open(tempname, 'rb')
	data = infp.read()
	infp.close()

	try:
		os.remove(tempname)
	except:
		log.warning("unable to delete tempfile %s" % tempfile)

	# return the data strings from the temp file
	return data


def restorePasswords(postfp, destfile):
	""" Update destfile with data but keep the passwords that are in destfile """

	# Write our new data to file
	(tempfp, tempname) = tempfile.mkstemp()
	outfp = os.fdopen(tempfp)
	outfp.write(postfp.read())
	oufp.close()

	# Open if and copy the passwords from what is on the sevrer
	conn = sqlite3.connect(tempname)
	conn.execute("attach '%s' as incoming" % tempname)
	conn.execute("attach '%s' as current" % destfile)
	conn.execute("delete from incoming.passwords")
	conn.execute("insert into incoming.passwords select * from current.passwords")
	conn.close()

	# Overwrite the server version with what we now have
	dpfp = open(destfile, 'wb')
	infp = open(tempname, 'rb')
	dbfp.write(infp.read())
	infp.close()
	dbfp.close()

	try:
		os.remove(tempname)
	except:
		log.warning("unable to delete tempfile %s" % tempfile)


