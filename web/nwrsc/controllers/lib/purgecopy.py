import logging
import os

from sqlalchemy import create_engine

from pylons import request, config, tmpl_context as c
from pylons.templating import render_mako
from pylons.controllers.util import redirect, url_for
from pylons.decorators import validate

from nwrsc.lib.schema import *
from nwrsc.model import *

log = logging.getLogger(__name__)


class PurgeCopy(object):

	def purge(self):
		c.files = [x for x in self._databaseList() if x.name != self.database]
		c.classlist = self.session.query(Class).order_by(Class.code).all()
		return render_mako('/admin/purge.mako')


	def processPurge(self):
		try:
			import sqlite3
		except:
			from pysqlite2 import dbapi2 as sqlite3

		searchseries = list()
		purgeclasses = list()
		for k in request.POST.keys():
			if k[0:2] == "c-":
				purgeclasses.append(k[2:])
			elif k[0:2] == "s-":
				searchseries.append(k[2:])

		# All cars that have runs in this series
		currentcar = set()
		currentdr = set()
		for x in self.session.query(Run.carid):
			currentcar.add(x.carid)
		for x in self.session.query(Registration.carid):
			currentcar.add(x.carid)

		currentcar.discard(None)

		for y in self.session.execute("select distinct driverid from cars where id in (%s)" % (','.join(map(str, currentcar)))):
			currentdr.add(y[0])

		# All cars that have runs in any previous database
		oldcarids = set()
		for s in searchseries:
			conn = sqlite3.connect(self.databasePath(s))
			conn.row_factory = sqlite3.Row
			cur = conn.cursor()
			cur.execute("select distinct carid from runs")
			oldcarids.update([x[0] for x in cur.fetchall()])
			conn.close()

		# All drivers associated with those runs
		olddriverids = set()
		for s in searchseries:
			conn = sqlite3.connect(self.databasePath(s))
			conn.row_factory = sqlite3.Row
			cur = conn.cursor()
			cur.execute("select distinct driverid from cars where id in (%s)" % (','.join(map(str, oldcarids))))
			olddriverids.update([x[0] for x in cur.fetchall()])
			conn.close()

		# Drivers in this database that have no unique/email
		#blankdr = [x[0] for x in self.session.execute("select id from drivers where email=''")]
		delcar = deldr = 0

		savecars = ','.join(map(str, oldcarids.union(currentcar)))
		savedrivers = ','.join(map(str, olddriverids.union(currentdr)))

		if len(searchseries) > 0:  # don't delete if they didn't select any series, that will delete all
			delcar = self.session.execute("delete from cars where id not in (%s)" % savecars).rowcount
			deldr = self.session.execute("delete from drivers where id not in (%s)" % savedrivers).rowcount

		if len(purgeclasses) > 0:
			sqllist = "', '".join(purgeclasses)
			currentcars = ','.join(map(str, currentcar))
			delcar += self.session.execute("delete from cars where classcode in ('%s') and id not in (%s)" % (sqllist, currentcars)).rowcount
		
		self.session.commit()
		c.text = "<h4>Deleted %s cars and %s drivers</h4>" % (delcar, deldr)
		return render_mako('/admin/simple.mako')



	### Series Copying ###
	def copyseries(self):
		c.action = 'processCopySeries'
		return render_mako('/admin/copyseries.mako')


	def insertfile(self, cur, name, type, path):
		try:
			cur.execute("insert into new.data values (?,?,?,?)", (name, type, datetime.today(), open(path).read()))
		except Exception, e:
			log.warning("Couldn't insert %s, %s" % (name, e))


	@validate(schema=CopySeriesSchema(), form='copyseries')
	def processCopySeries(self):
		try:
			import sqlite3
		except:
			from pysqlite2 import dbapi2 as sqlite3

		""" Process settings form submission """
		log.debug("copyseriesform: %s", self.form_result)
		name = self.form_result['name']
		import nwrsc
		root = nwrsc.__path__[0]

		newpath = self.databasePath(name, mustExist=False)

		if not os.path.exists(newpath):
			metadata.bind = create_engine('sqlite:///%s' % newpath)
			metadata.create_all()

			conn = sqlite3.connect(':memory:')
			conn.row_factory = sqlite3.Row
			cur = conn.cursor()
			cur.execute("attach '%s' as old" % self.databasePath(self.database))
			cur.execute("attach '%s' as new" % newpath)

			# Settings
			if self.form_result['settings']:
				cur.execute("insert into new.settings select * from old.settings")
			else:
				for k,v in {'useevents':5, 'ppoints':'20,16,13,11,9,7,6,5,4,3,2,1'}.iteritems():
					cur.execute("insert into new.settings values (?,?)", (k,v))
			cur.execute("insert or replace into new.settings values (?,?)", ("password", self.form_result['password']))

			# Template data
			if self.form_result['data']:
				cur.execute("insert into new.data select * from old.data")
			else:
				self.insertfile(cur, 'results.css', 'text/css', os.path.join(root, 'examples/wwresults.css'))
				self.insertfile(cur, 'event.mako', 'text/plain', os.path.join(root, 'examples/wwevent.mako'))
				self.insertfile(cur, 'champ.mako', 'text/plain', os.path.join(root, 'examples/wwchamp.mako'))
				self.insertfile(cur, 'toptimes.mako', 'text/plain', os.path.join(root, 'examples/toptimes.mako'))
				self.insertfile(cur, 'classresult.mako', 'text/plain', os.path.join(root, 'examples/classresults.mako'))
				self.insertfile(cur, 'card.py', 'text/plain', os.path.join(root, 'examples/basiccard.py'))

			if self.form_result['classes']:
				cur.execute("insert into new.classlist select * from old.classlist")
				cur.execute("insert into new.indexlist select * from old.indexlist")

			if self.form_result['drivers']:
				cur.execute("insert into new.drivers select * from old.drivers")

			if self.form_result['cars']:
				cur.execute("insert into new.cars select * from old.cars")

			if self.form_result['prevlist']:
				cur.execute("""insert into new.prevlist (firstname, lastname) 
							select distinct lower(d.firstname) as firstname, lower(d.lastname) as lastname
							from old.runs as r, old.cars as c, old.drivers as d
							where r.carid=c.id and c.driverid=d.id """)
				c.feelists = FeeList.getAll(self.session)

			cur.close()
			conn.commit()

		else:
			log.error("database exists")

		redirect(url_for(database=name, action=''))

