
import sys
import re
import logging
import datetime

from nwrsc.model import *
from sqlalchemy.databases.sqlite import SLDateTime, SLDate

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
	session.execute("DROP TABLE oldeventresults")
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


def convert20122(session):

	metadata.bind = session.bind

	# Add new announcer table
	metadata.tables['announcer'].create()
	
	# Remove updated from old eventresults
	log.info("update eventresults")
	makeTableOld(session, metadata, 'eventresults');
	for row in session.execute("select * from oldeventresults"):
		session.add(EventResult(**row2dict(row)))

	log.info("Drop old tables")
	session.execute("DROP TABLE oldeventresults")

	# Add new cars flag
	session.execute("ALTER TABLE cars ADD COLUMN tireindexed BOOLEAN DEFAULT 0")

	log.info("update settings")
	settings = Settings()
	settings.load(session)
	settings.schema = '20123'
	settings.save(session)

	session.commit()


def convert20123(session):

	metadata.bind = session.bind

	# Add new cars flag
	session.execute("ALTER TABLE classlist ADD COLUMN usecarflag BOOLEAN DEFAULT 0")

	log.info("update settings")
	settings = Settings()
	settings.load(session)
	settings.schema = '20124'
	settings.save(session)

	session.commit()


def convert20124(session):

	metadata.bind = session.bind

	# Add new doublespecial flag
	session.execute("ALTER TABLE events ADD COLUMN doublespecial BOOLEAN DEFAULT 0")

	log.info("update settings")
	settings = Settings()
	settings.load(session)
	settings.schema = '20131'
	settings.save(session)

	session.commit()

def convert20131(session):

	metadata.bind = session.bind

	# Add option for restricting car indexes
	session.execute("ALTER TABLE classlist ADD COLUMN caridxrestrict VARCHAR(128) DEFAULT ''")

	log.info("update settings")
	settings = Settings()
	settings.load(session)
	settings.schema = '20132'
	settings.save(session)

	session.commit()

def convert20132(session):

	metadata.bind = session.bind

	# Create passwords table and copy over values
	metadata.tables['passwords'].create()
	session.execute("INSERT INTO passwords (tag, value) select 'series', val from settings where name='password'");
	session.execute("INSERT INTO passwords (tag, value) select id, password from events")

	# Remove password from events table
	log.info("update events")
	makeTableOld(session, metadata, 'events');
	for row in session.execute("select * from oldevents"):
		event = Event(**row2dict(row))
		event.date = SLDate().result_processor(None)(event.date)
		event.regopened = SLDateTime().result_processor(None)(event.regopened)
		event.regclosed = SLDateTime().result_processor(None)(event.regclosed)
		session.add(event)
	session.execute("DROP TABLE oldevents")

	# Remove password from settings
	session.execute("DELETE FROM settings where name='password'")

	log.info("update settings")
	settings = Settings()
	settings.load(session)
	settings.schema = '20133'
	settings.save(session)

	session.commit()


def convert20133(session):

	# Put membership back into driver table, not extras
	session.execute("ALTER TABLE drivers ADD COLUMN membership VARCHAR(16) DEFAULT ''")
	for row in session.execute("select driverid, value from driverextra where name='membership'"):
		session.execute("update drivers set membership=:value where id=:driverid", row)
	session.execute("delete from driverextra where name='membership'")
	session.execute("delete from driverfields where name='membership'")

	cardpy = session.query(Data).filter(Data.name=='card.py').first()
	cardpy.data = re.sub("getExtra\('membership'\)", 'membership', cardpy.data)

	log.info("update settings")
	settings = Settings()
	settings.load(session)
	settings.schema = '20134'
	settings.save(session)

	session.commit()


def convert20134(session):

	# Add paid column, drop changes table
	session.execute("ALTER TABLE registered ADD COLUMN paid BOOLEAN DEFAULT 0")
	session.execute("DROP TABLE changes")

	log.info("update settings")
	settings = Settings()
	settings.load(session)
	settings.schema = '20135'
	settings.save(session)
	session.commit()


def convert20135(session):
	# Create notes table
	metadata.bind = session.bind
	metadata.tables['drivernotes'].create()

	log.info("update settings")
	settings = Settings()
	settings.load(session)
	settings.schema = '20141'
	settings.save(session)
	session.commit()

	

converters = {
	'20112': convert2011,
	'20121': convert20121,
	'20122': convert20122,
	'20123': convert20123,
	'20124': convert20124,
	'20131': convert20131,
	'20132': convert20132,
	'20133': convert20133,
	'20134': convert20134,
	'20135': convert20135
}


def convert(session):
	try:
		settings = Settings()
		settings.load(session)

		while settings.schema != SCHEMA_VERSION:
			log.info("CONVERSION: upgrading %s (need %s)", settings.schema, SCHEMA_VERSION)
			converters[settings.schema](session)
			settings.load(session)

	except:
		raise ConversionError("Error converting from %s, %s" % (settings.schema, sys.exc_info()[1]))


