
import sys
import re
import logging

from nwrsc.model import *

log = logging.getLogger(__name__)

class ConversionError(StandardError):
	pass

def convert2011(session):
	session.execute("ALTER TABLE classlist RENAME to oldclasslist")
	metadata.tables['classlist'].create()

	# change classindexed/BOOLEAN to classindex/STRING
	for row in session.execute("select * from oldclasslist"):
		newclass = Class(**dict(row.items()))
		newclass.classindex = row.classindexed and newclass.code or ''
		session.add(newclass)
	
	session.execute("ALTER TABLE drivers RENAME TO olddrivers")
	for index in metadata.tables['drivers'].indexes:
		session.execute("DROP INDEX %s" % index.name)
	metadata.tables['drivers'].create()
	metadata.tables['driverextra'].create()
	metadata.tables['driverfields'].create()

	# change homephone->phone, delete workphone, move membership/clubs to extra
	for row in session.execute("select * from olddrivers"):
		newdriver = Driver(**dict(row.items()))
		newdriver.phone = row.homephone
		session.add(newdriver)

		if row.membership is not None and len(row.membership) > 0:
			session.add(DriverExtra(driverid=row.id, name='membership', value=row.membership))
		if row.clubs is not None and len(row.clubs) > 0:
			session.add(DriverExtra(driverid=row.id, name='clubs', value=row.clubs))

	session.execute("DROP TABLE oldclasslist")
	session.execute("DROP TABLE olddrivers")

	classresults = session.query(Data).filter(Data.name=='classresult.mako').first()
	classresults.data = re.sub('classindexed', 'classindex', classresults.data)

	cardpy = session.query(Data).filter(Data.name=='card.py').first()
	cardpy.data = re.sub(r'def drawCard.*', 'def drawCard(c, event, driver, car, image, **kwargs):\n', cardpy.data)
	cardpy.data = re.sub('homephone', 'phone', cardpy.data)
	cardpy.data = re.sub('membership', "getExtra('membership')", cardpy.data)

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


