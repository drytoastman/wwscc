
import sys
import re
import logging
import datetime

from nwrsc.model import *
from sqlalchemy.databases.sqlite import SLDateTime

log = logging.getLogger(__name__)

class ConversionError(StandardError):
	pass


def row2dict(row):
	args = dict() # why do they hate me so
	for k,v in row.items():
		args[str(k)] = v
	return args

def makeTableOld(session, metadata, name):
	session.execute("ALTER TABLE %s RENAME TO old%s" % (name, name))
	for index in metadata.tables[name].indexes:
		session.execute("DROP INDEX %s" % index.name)
	metadata.tables[name].create()

def convert2011(session):
	metadata.bind = session.bind

	log.info("update classes")
	makeTableOld(session, metadata, 'classlist');
	# change classindexed/BOOLEAN to classindex/STRING
	for row in session.execute("select * from oldclasslist"):
		newclass = Class(**row2dict(row))
		newclass.classindex = row.classindexed and newclass.code or ''
		session.add(newclass)
	
	log.info("update drivers")
	makeTableOld(session, metadata, 'drivers');
	metadata.tables['driverextra'].create()
	metadata.tables['driverfields'].create()

	# change homephone->phone, delete workphone, move membership/clubs to extra
	log.info("pull in old drivers table")
	for row in session.execute("select * from olddrivers"):
		newdriver = Driver(**row2dict(row))
		newdriver.phone = row.homephone
		session.add(newdriver)

		if row.membership is not None and len(row.membership) > 0:
			session.add(DriverExtra(driverid=row.id, name='membership', value=row.membership))
		if row.clubs is not None and len(row.clubs) > 0:
			session.add(DriverExtra(driverid=row.id, name='clubs', value=row.clubs))

	log.info("drop old drivers table")
	session.execute("DROP TABLE oldclasslist")
	session.execute("DROP TABLE olddrivers")

	log.info("filter data")
	classresults = session.query(Data).filter(Data.name=='classresult.mako').first()
	classresults.data = re.sub('classindexed', 'classindex', classresults.data)

	cardpy = session.query(Data).filter(Data.name=='card.py').first()
	cardpy.data = re.sub(r'def drawCard.*', 'def drawCard(c, event, driver, car, image, **kwargs):\n', cardpy.data)
	cardpy.data = re.sub('homephone', 'phone', cardpy.data)
	cardpy.data = re.sub('membership', "getExtra('membership')", cardpy.data)

	log.info("update settings")
	settings = Settings()
	settings.load(session)  # also loads new default values
	settings.schema = '20121'
	settings.save(session)

	session.commit()


def convert20121(session):

	metadata.bind = session.bind

	#rename eventresults.ppoint and ppoints
	"""
	"""
	log.info("update eventresults")
	makeTableOld(session, metadata, 'eventresults');
	processor = SLDateTime().result_processor(None)
	for row in session.execute("select * from oldeventresults"):
		newresult = EventResult(**row2dict(row))
		newresult.diffpoints = row.points
		newresult.pospoints = row.ppoints
		newresult.updated = processor(newresult.updated)  # doesn't get converted in regular select
		session.add(newresult)

	# load challenges and then resave to delete "bonus" attribute
	log.info("update challenges")
	makeTableOld(session, metadata, 'challenges');
	for row in session.execute("select * from oldchallenges"):
		session.add(Challenge(**row2dict(row)))

	log.info("Drop old tables")
	#session.execute("DROP TABLE oldeventresults")
	session.execute("DROP TABLE oldchallenges")
	
	# add usepospoints, champsorting, change ppoints to pospointlist
	log.info("update settings")
	settings = Settings()
	settings.load(session)  # also loads new default values
	settings.schema = '20122'
	settings.usepospoints = False
	settings.champsorting = ""
	settings.pospointlist = settings.ppoints
	settings.save(session)

	session.execute("delete from settings where name='ppoints'");
	session.commit()

	

converters = {
	'20112': convert2011,
	'20121': convert20121,
}

def convert(session):
	try:
		settings = Settings()
		settings.load(session)

		while settings.schema != SCHEMA_VERSION:
			log.info("CONVERSION: upgrading %s", settings.schema)
			converters[settings.schema](session)
			settings.load(session)
			break

	except:
		raise ConversionError("Error converting from %s, %s" % (settings.schema, sys.exc_info()[1]))


