
import sys
import re
import logging

from nwrsc.model import *

log = logging.getLogger(__name__)

class ConversionError(StandardError):
	pass


def row2dict(row):
	args = dict() # why do they hate me so
	for k,v in row.items():
		args[str(k)] = v
	return args

def convert2011(session):
	metadata.bind = session.bind

	session.execute("ALTER TABLE classlist RENAME to oldclasslist")
	metadata.tables['classlist'].create()

	log.info("update classes")
	# change classindexed/BOOLEAN to classindex/STRING
	for row in session.execute("select * from oldclasslist"):
		newclass = Class(**row2dict(row))
		newclass.classindex = row.classindexed and newclass.code or ''
		session.add(newclass)
	
	log.info("update drivers")
	session.execute("ALTER TABLE drivers RENAME TO olddrivers")
	for index in metadata.tables['drivers'].indexes:
		session.execute("DROP INDEX %s" % index.name)
	metadata.tables['drivers'].create()
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


converters = {
	'20112': convert2011,
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


