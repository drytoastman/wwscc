import logging
import cStringIO
import os
import glob
import time
import operator

from sqlalchemy import create_engine
from sqlalchemy.sql import func
from sqlalchemy.pool import NullPool
from datetime import datetime

from pylons import request, response, session, config, tmpl_context as c
from pylons.templating import render_mako
from pylons.controllers.util import redirect, url_for, abort
from pylons.decorators import jsonify, validate
from nwrsc.lib.base import BaseController, BeforePage
from nwrsc.lib.entranteditor import EntrantEditor
from nwrsc.lib.schema import *
from nwrsc.model import *

log = logging.getLogger(__name__)

def insertfile(cur, name, type, path):
	try:
		cur.execute("insert into new.data values (?,?,?,?)", (name, type, datetime.today(), open(path).read()))
	except Exception, e:
		log.warning("Couldn't insert %s, %s" % (name, e))

class AdminController(BaseController, EntrantEditor):

	def __before__(self):
		c.stylesheets = ['/css/admin.css', '/css/redmond/jquery-ui-1.8.2.custom.css', '/css/anytimec.css']
		c.javascript = ['/js/admin.js', '/js/sortabletable.js', '/js/jquery-1.4.2.min.js', '/js/jquery-ui-1.8.2.custom.min.js', '/js/superfish.js', '/js/jquery.validate.min.js', '/js/anytimec.js']
		if self.database is not None:
			c.events = self.session.query(Event).all()
		self.eventid = self.routingargs.get('eventid', None)

		c.isLocked = self.settings.locked
		c.event = None
		if self.eventid and self.eventid.isdigit():
			c.event = self.session.query(Event).get(self.eventid)

		if self.eventid and self.routingargs.get('action', '') != 'login':
			self._checkauth(self.eventid, c.event)

		if self.settings.locked:
			action = self.routingargs.get('action', '')
			if action not in ['login', 'index', 'printcards', 'paid', 'numbers', 'paypal', 'fees', 'allfees', 'printhelp', 'forceunlock']:
				c.seriesname = self.settings.seriesname
				c.next = action
				raise BeforePage(render_mako('/admin/locked.mako'))


	def _checkauth(self, eventid, event):
		if self.srcip == '127.0.0.1':
			c.isAdmin = True
			return
	
		if event is None and eventid != 's':
			c.text = "<h3>No such event for %s</h3>" % eventid
			raise BeforePage(render_mako('/admin/simple.mako'))
	
		ipsession = session.setdefault(self.srcip, {})
		tokens = ipsession.setdefault('authtokens', set())
		c.isAdmin = 'series' in tokens
		if event is not None:
			if int(eventid) in tokens or 'series' in tokens:
				return
			c.request = "Need authentication token for %s" % event.name
			raise BeforePage(render_mako('/admin/login.mako'))
		else:
			if c.isAdmin:
				return
			c.request = "Need authentication token for the series"
			raise BeforePage(render_mako('/admin/login.mako'))
	

	def forceunlock(self):
		self.settings.locked = False
		self.settings.save(self.session)
		self.session.commit()
		redirect(url_for(action=request.GET.get('next', '')))


	def login(self):
		password = request.POST.get('password')
		ipsession = session.setdefault(self.srcip, {})
		tokens = ipsession.setdefault('authtokens', set())

		if password == self.settings.password:
			tokens.add('series')

		for event in c.events:
			if password == event.password:
				tokens.add(event.id)

		session.save()
		redirect(url_for(action=''))
			

	def index(self):
		if self.eventid and self.eventid.isdigit():
			return render_mako('/admin/event.mako')
		elif self.database is not None:
			c.text = "<h2>%s Adminstration</h2>" % self.routingargs['database']
			return render_mako('/admin/simple.mako')
		else:
			c.files = map(os.path.basename, glob.glob('%s/*.db' % (config['seriesdir'])))
			return render_mako('/databaseselect.mako')


	### Settings table editor ###
	def seriessettings(self):
		c.settings = self.settings
		c.action = 'updatesettings'
		c.button = 'Update'
		return render_mako('/forms/seriessettings.mako')

	@validate(schema=SettingsSchema(), form='seriessettings', prefix_error=False)
	def updatesettings(self):
		""" Process settings form submission """
		self.settings.set(self.form_result)
		self.settings.save(self.session)

		for key, file in self.form_result.iteritems():
			if hasattr(file, 'filename'):
				# TODO: Check file size before doing this
				Data.set(self.session, key, file.value, file.type)

		self.session.commit()
		redirect(url_for(action='seriessettings'))


	### Data editor ###
	def editor(self):
		if 'name' not in request.GET:
			return "Missing name"
		c.name = request.GET['name']
		c.data = ""
		data = self.session.query(Data).get(c.name)
		if data is not None:
			c.data = data.data
		return render_mako('/admin/editor.mako')

	def savecode(self):
		name = str(request.POST.get('name', None))
		data = str(request.POST.get('data', ''))
		if name is not None:
			Data.set(self.session, name, data)
			self.session.commit()
			redirect(url_for(action='editor', name=name))


	def cleanup(self):
		updateFromRuns(self.session)
		c.text = "<h3>Cleaned</h3>"
		return render_mako('/admin/simple.mako')


	def recalc(self):
		return render_mako('/admin/recalc.mako')
		
	def dorecalc(self):
		from nwrsc.controllers.dblib import RecalculateResults
		response.headers['Content-type'] = 'text/plain'
		return RecalculateResults(self.session)


	def printhelp(self):
		return render_mako('/admin/printhelp.mako')


	def printcards(self):

		drawCard = self.loadPythonFunc('drawCard', self.session.query(Data).get('card.py').data)

		page = request.GET.get('page', 'card')
		type = request.GET.get('type', 'blank')

		query = self.session.query(Driver,Car,Registration).join('cars', 'registration').filter(Registration.eventid==self.eventid)
		if type == 'blank':
			registered = [(None,None,None)]
		elif type == 'lastname':
			registered = query.order_by(func.lower(Driver.lastname), func.lower(Driver.firstname)).all()
		elif type == 'classnumber':
			registered = query.order_by(Car.classcode, Car.number).all()
		
		if page == 'csv':
			# CSV data, just use a template and return
			c.registered = registered
			response.headers['Content-type'] = "application/octet-stream"
			response.headers['Content-Disposition'] = 'attachment;filename=cards.csv'
			response.charset = 'utf8'
			return render_mako('/admin/csv.mako')

		# Otherwise we are are PDF
		try:
			from reportlab.pdfgen import canvas
			from reportlab.lib.units import inch
		except:
			c.text = "<h4>PDFGen not installed, can't create timing card PDF files from this system</h4>"
			return render_mako("/admin/simple.mako")

		if page == 'letter': # Letter has an additional 72 points Y to space out
			size = (8*inch, 11*inch)
		else:
			size = (8*inch, 5*inch)

		if page == 'letter' and len(registered)%2 != 0:
			registered.append((None,None,None)) # Pages are always two cards per so make it divisible by 2

		buffer = cStringIO.StringIO()
		canvas = canvas.Canvas(buffer, pagesize=size, pageCompression=1)
		while len(registered) > 0:
			if page == 'letter':
				canvas.translate(0, 18)  # 72/4, bottom margin for letter page
				(driver, car, reg) = registered.pop(0)
				drawCard(canvas, c.event, driver, car)
				canvas.translate(0, 396)  # 360+72/2 card size plus 2 middle margins
				(driver, car, reg) = registered.pop(0)
				drawCard(canvas, c.event, driver, car)
			else:
				(driver, car, reg) = registered.pop(0)
				drawCard(canvas, c.event, driver, car)
			canvas.showPage()
		canvas.save()

		response.headers['Content-type'] = "application/octet-stream"
		response.headers['Content-Disposition'] = 'attachment;filename=cards.pdf'
		return buffer.getvalue()


	def numbers(self):

		# As with other places, one big query followed by mangling in python is faster (and clearer)
		c.numbers = {}
		for res in self.session.query(Driver.firstname, Driver.lastname, Car.classcode, Car.number).join('cars'):
			if self.settings.superuniquenumbers:
				code = "All"
			else:
				code = res[2]
			num = res[3]
			name = res[0]+" "+res[1]
			if code not in c.numbers:
				c.numbers[code] = {}
			c.numbers[code].setdefault(num, set()).add(name)

		return render_mako('/admin/numberlist.mako')

	def paid(self):
		""" Return the list of fees paid before this event """
		f = FeeList.get(self.session, self.eventid)
		c.header = '<h2>Fees Collected Before %s</h2>' % c.event.name
		c.feelist = f.before
		return render_mako('/admin/feelist.mako')

	def fees(self):
		""" Return the list of fees collected at a single event """
		f = FeeList.get(self.session, self.eventid)
		c.header = '<h2>Fees Collected During %s (%d)</h2>' % (c.event.name, len(f.during))
		c.feelist = f.during
		return render_mako('/admin/feelist.mako')

	def allfees(self):
		""" Return the complete list of fees collected at all events """
		c.feelists = FeeList.getAll(self.session)
		return render_mako('/admin/allfees.mako')

	def paypal(self):
		""" Return a list of paypal transactions for the current event """
		c.payments = self.session.query(Payment).filter(Payment.eventid==self.eventid).all()
		c.payments.sort(key=lambda obj: obj.driver.lastname)
		return render_mako('/admin/paypal.mako')	

	### RunGroup Editor ###
	def rungroups(self):
		c.action = 'setRunGroups'
		c.groups = {0:[]}
		allcodes = set([res[0] for res in self.session.query(Class.code)])
		for group in self.session.query(RunGroup).order_by(RunGroup.rungroup, RunGroup.gorder).filter(RunGroup.eventid==self.eventid).all():
			c.groups.setdefault(group.rungroup, list()).append(group.classcode)
			allcodes.discard(group.classcode)
		for code in sorted(allcodes):
			c.groups[0].append(code)
		return render_mako('/admin/editrungroups.mako')


	def setRunGroups(self):
		try:
			for group in self.session.query(RunGroup).filter(RunGroup.eventid==self.eventid):
				self.session.delete(group)
			self.session.flush()
			for k in request.POST:
				if k[:5] == 'group':
					if int(k[5]) == 0:  # not recorded means group 0
						continue
					for ii, code in enumerate(request.POST[k].split(',')):
						g = RunGroup()
						g.eventid = self.eventid
						g.rungroup = int(k[5])
						g.classcode = str(code)
						g.gorder = ii
						self.session.add(g)
			self.session.commit()
		except Exception, e:
			logging.error("setRunGroups failed: %s" % e)
			self.session.rollback()
		redirect(url_for(action='rungroups'))
		

	### other ###
	def editevent(self):
		""" Present form to edit event details """
		c.action = 'updateevent'
		c.button = 'Update'
		return render_mako('/forms/eventedit.mako')

	@validate(schema=EventSchema(), form='editevent', prefix_error=False)
	def updateevent(self):
		""" Process edit event form submission """
		self.copyvalues(self.form_result, c.event)
		self.session.commit()
		redirect(url_for(action='editevent'))

	def createevent(self):
		""" Present form to create a new event """
		c.action = 'newevent'
		c.button = 'New'
		c.event = Event()
		c.event.conepen = 2.0
		c.event.gatepen = 10.0
		c.event.courses = 1
		c.event.runs = 4
		c.event.perlimit = 2
		c.event.totlimit = 0
		c.event.date = datetime.today()
		c.event.regopened = datetime.today()
		c.event.regclosed = datetime.today()
		return render_mako('/forms/eventedit.mako')

	@validate(schema=EventSchema(), form='createevent', prefix_error=False)
	def newevent(self):
		""" Process new event form submission """
		ev = Event()
		print self.form_result
		self.copyvalues(self.form_result, ev)
		self.session.add(ev)
		self.session.commit()
		redirect(url_for(eventid=ev.id, action=''))


	class RegObj(object):
		def __init__(self, d, c, r):
			self.driver = d
			self.car = c
			self.reg = r

	def list(self):
		query = self.session.query(Driver,Car,Registration).join('cars', 'registration').filter(Registration.eventid==self.eventid)
		c.classdata = ClassData(self.session)
		c.registered = {}
		for (driver, car, reg) in query.all():
			if car.classcode not in c.registered:
				c.registered[car.classcode] = []
			c.registered[car.classcode].append(self.RegObj(driver, car, reg))
		return render_mako('/admin/entrylist.mako')

	def delreg(self):
		regid = request.POST.get('regid', None)
		if regid:
			reg = self.session.query(Registration).filter(Registration.id==regid).first()
			if reg:
				self.session.delete(reg)
				self.session.commit()
		redirect(url_for(action='list'))
	

	#### Class and Index Editors ####

	def fieldlist(self):
		c.action = 'processFieldList'
		c.fieldlist = self.session.query(DriverField).all()
		return render_mako('/forms/fieldlist.mako')

	@validate(schema=DriverFieldListSchema(), form='fieldlist')
	def processFieldList(self):
		data = self.form_result['fieldlist']
		if len(data) > 0:
			# delete fields, then add new submitted ones
			for fld in self.session.query(DriverField):
				self.session.delete(fld)
			for obj in data:
				self.session.add(DriverField(**obj))
		self.session.commit()
		redirect(url_for(action='fieldlist'))


	def classlist(self):
		c.action = 'processClassList'
		c.classlist = self.session.query(Class).order_by(Class.code).all()
		c.indexlist = [""] + [x[0] for x in self.session.query(Index.code).order_by(Index.code)]
		return render_mako('/forms/classlist.mako')

	@validate(schema=ClassListSchema(), form='classlist')
	def processClassList(self):
		data = self.form_result['clslist']
		if len(data) > 0:
			# delete classes, then add new submitted ones
			for cls in self.session.query(Class):
				self.session.delete(cls)
			for obj in data:
				self.session.add(Class(**obj))
		self.session.commit()
		redirect(url_for(action='classlist'))
		

	def indexlist(self):
		c.action = 'processIndexList'
		c.indexlist = self.session.query(Index).order_by(Index.code).all()
		return render_mako('/forms/indexlist.mako')

	@validate(schema=IndexListSchema(), form='indexlist')
	def processIndexList(self):
		data = self.form_result['idxlist']
		if len(data) > 0:
			# delete indexes, then add new submitted ones
			for idx in self.session.query(Index):
				self.session.delete(idx)
			for obj in data:
				self.session.add(Index(**obj))
		self.session.commit()
		redirect(url_for(action='indexlist'))

	### Clean Series ###

	def purge(self):
		c.files = map(os.path.basename, glob.glob('%s/*.db' % (config['seriesdir'])))
		c.files.remove(self.database+".db")
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

		# All cars that have runs in any previous database
		carids = set()
		for s in searchseries:
			conn = sqlite3.connect(os.path.join(config['seriesdir'], s))
			conn.row_factory = sqlite3.Row
			cur = conn.cursor()
			cur.execute("select distinct carid from runs")
			carids.update([x[0] for x in cur.fetchall()])
			conn.close()

		# All drivers associated with those runs
		driverids = set()
		for s in searchseries:
			conn = sqlite3.connect(os.path.join(config['seriesdir'], s))
			conn.row_factory = sqlite3.Row
			cur = conn.cursor()
			cur.execute("select distinct driverid from cars where id in (%s)" % (','.join(map(str, carids))))
			driverids.update([x[0] for x in cur.fetchall()])
			conn.close()

		# Drivers in this database that have no unique/email
		#blankdr = [x[0] for x in self.session.execute("select id from drivers where email=''")]
		delcar = deldr = 0

		if len(searchseries) > 0:  # don't delete if they didn't select any series, that will delete all
			delcar = self.session.execute("delete from cars where id not in (%s)" % (','.join(map(str, carids)))).rowcount
			#delcar += self.session.execute("delete from cars where driverid in (%s)" % ','.join(map(str,blankdr))).rowcount
			deldr = self.session.execute("delete from drivers where id not in (%s)" % (','.join(map(str, driverids)))).rowcount
			#deldr += self.session.execute("delete from drivers where id in (%s)" % ','.join(map(str,blankdr))).rowcount

		if len(purgeclasses) > 0:
			sqllist = "', '".join(purgeclasses)
			delcar += self.session.execute("delete from cars where classcode in ('"+sqllist+"')").rowcount
		
		self.session.commit()
		c.text = "<h4>Deleted %s cars and %s drivers</h4>" % (delcar, deldr)
		return render_mako('/admin/simple.mako')



	### Series Copying ###
	def copyseries(self):
		c.action = 'processCopySeries'
		return render_mako('/forms/copyseries.mako')


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

		if not os.path.exists(self.databasePath(name)):
			metadata.bind = create_engine('sqlite:///%s' % self.databasePath(name), poolclass=NullPool)
			metadata.create_all()

			conn = sqlite3.connect(':memory:')
			conn.row_factory = sqlite3.Row
			cur = conn.cursor()
			cur.execute("attach '%s' as old" % self.databasePath(self.database))
			cur.execute("attach '%s' as new" % self.databasePath(name))

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
				insertfile(cur, 'results.css', 'text/css', os.path.join(root, 'examples/wwresults.css'))
				insertfile(cur, 'event.mako', 'text/plain', os.path.join(root, 'examples/wwevent.mako'))
				insertfile(cur, 'champ.mako', 'text/plain', os.path.join(root, 'examples/wwchamp.mako'))
				insertfile(cur, 'toptimes.mako', 'text/plain', os.path.join(root, 'examples/toptimes.mako'))
				insertfile(cur, 'classresult.mako', 'text/plain', os.path.join(root, 'examples/classresults.mako'))
				insertfile(cur, 'card.py', 'text/plain', os.path.join(root, 'examples/basiccard.py'))

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

		redirect(url_for(action='copyseries'))

