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
from pylons.decorators import jsonify, validate as xvalidate
from tw.mods.pylonshf import validate
from nwrsc.lib.base import BaseController, BeforePage
from nwrsc.model import *
from nwrsc.forms import *
import nwrsc

log = logging.getLogger(__name__)

def insertfile(cur, name, type, path):
	try:
		cur.execute("insert into new.data values (?,?,?,?)", (name, type, datetime.today(), open(path).read()))
	except Exception, e:
		log.warning("Couldn't insert %s, %s" % (name, e))

class AdminController(BaseController):

	def __before__(self):
		c.stylesheets = ['/css/admin.css', '/css/redmond/jquery-ui-1.8.2.custom.css']
		c.javascript = ['/js/admin.js', '/js/sortabletable.js', '/js/jquery-1.4.2.min.js', '/js/jquery-ui-1.8.2.custom.min.js', '/js/superfish.js', '/js/jquery.validate.min.js']
		c.isLocked = (int(self.settings.get('locked', 1)) == 1)
		if self.database is not None:
			c.events = self.session.query(Event).all()
		self.eventid = self.routingargs.get('eventid', None)

		c.event = None
		if self.eventid and self.eventid.isdigit():
			c.event = self.session.query(Event).get(self.eventid)

		if self.eventid and self.routingargs.get('action', '') != 'login':
			self._checkauth(self.eventid, c.event)

		if int(self.settings.get('locked', 0)):
			action = self.routingargs.get('action', '')
			if action not in ['index', 'printcards', 'paid', 'numbers', 'paypal', 'fees', 'allfees', 'printhelp', 'forceunlock']:
				c.seriesname = self.settings.get('seriesname', 'Missing Name')
				c.next = action
				raise BeforePage(render_mako('/admin/locked.mako'))


	def _checkauth(self, eventid, event):
		if self.srcip == '127.0.0.1':
			c.isAdmin = True
			return
	
		if event is None and eventid != 's':
			c.adminheader = "No such event for %s" % eventid
			raise BeforePage(render_mako('/admin/title.mako'))
	
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
		locked = self.session.query(Setting).get('locked')
		locked.val = '0'
		self.session.commit()
		redirect(url_for(action=request.GET.get('next', '')))


	def login(self):
		password = request.POST.get('password')
		ipsession = session.setdefault(self.srcip, {})
		tokens = ipsession.setdefault('authtokens', set())

		if password == self.settings['password']:
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
			c.adminheader = "%s Adminstration" % self.routingargs['database']
			return render_mako('/admin/title.mako')
		else:
			c.files = map(os.path.basename, glob.glob('%s/*.db' % (config['seriesdir'])))
			return render_mako('/databaseselect.mako')


	### Settings table editor ###
	def seriessettings(self):
		c.settings = self.settings
		c.settings['locked'] = bool(int(c.settings['locked']))
		c.action = 'updatesettings'
		c.button = 'Update'
		return render_mako('/admin/settings.mako')

	@validate(form=settingsForm, error_handler='seriessettings')
	def updatesettings(self):
		""" Process settings form submission """
		self.form_result['locked'] = int(self.form_result['locked'])
		Setting.saveDict(self.session, self.form_result)
		self.session.commit()
		redirect(url_for(action='seriessettings'))


	def uploadimage(self):
		for key, file in request.POST.iteritems():
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
		c.adminheader = "Cleaned"
		return render_mako('/admin/title.mako')


	def recalc(self):
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
			c.adminheader = "PDFGen not installed, can't create timing card PDF files from this system"
			return render_mako("/admin/title.mako")

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
			code = res[2]
			num = res[3]
			name = res[0]+" "+res[1]
			if code not in c.numbers:
				c.numbers[code] = {}
			c.numbers[code][num] = name

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

	def edit(self):
		""" Present form to edit event details """
		c.editpassword = True
		c.action = 'updateevent'
		c.button = 'Update'
		return render_mako('/admin/eventedit.mako')

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
		


	### Entrant editor ####

	class DriverInfo(object):
		def __init__(self, d, c):
			self.driver = d
			self.cars = c

	def drivers(self):
		c.classdata = ClassData(self.session)
		return render_mako('/admin/drivers.mako')

		
	def mergedriver(self):
		try:
			driverid = int(request.POST.get('driverid', None))
			allids = map(int, request.POST.get('allids', '').split(','))
			allids.remove(driverid)
			for tomerge in allids:
				log.info("merge %s into %s" % (tomerge, driverid))
				# update car id maps
				for car in self.session.query(Car).filter(Car.driverid==tomerge):
					car.driverid = driverid 
				# delete old driver
				dr = self.session.query(Driver).filter(Driver.id==tomerge).first()
				self.session.delete(dr)
				
			self.session.commit()
			return "";
		except Exception, e:
			log.info('merge driver failed: %s' % e)
			abort(400);


	def deletedriver(self):
		try:
			driverid = request.POST.get('driverid', None)
			log.info('request to delete driver %s' % driverid)
			for car in self.session.query(Car).filter(Car.driverid==driverid):
				if len(self.session.query(Run.eventid).distinct().filter(Run.carid==car.id).all()) > 0:
					raise Exception("driver car has runs")
				self.session.delete(car)
			dr = self.session.query(Driver).filter(Driver.id==driverid).first()
			self.session.delete(dr)
			self.session.commit()
			return "";
		except Exception, e:
			log.info('delete driver failed: %s' % e)
			abort(400);


	def editdriver(self):
		try:
			driverid = request.POST.get('driverid', None)
			log.info('request to edit driver %s' % driverid)
			driver = self.session.query(Driver).get(driverid)
			for attr in request.POST:
				if hasattr(driver, attr):
					setattr(driver, attr, request.POST[attr])
			self.session.commit()
		except Exception, e:
			log.info('edit driver failed: %s' % e)
			abort(400);


	def deletecar(self):
		try:
			carid = request.POST.get('carid', None)
			log.info('request to delete car %s' % carid)
			car = self.session.query(Car).get(carid)
			if len(self.session.query(Run.eventid).distinct().filter(Run.carid==car.id).all()) > 0:
				raise Exception("car has runs")
			self.session.delete(car)
			self.session.commit()
			return "";
		except Exception, e:
			log.info('delete car failed: %s' % e)
			abort(400);


	def editcar(self):
		try:
			carid = request.POST.get('carid', None)
			log.info('request to edit car %s' % carid)
			car = self.session.query(Car).get(carid)
			for attr in ('year', 'make', 'model', 'color', 'number', 'classcode', 'indexcode'):
				if attr in request.POST:
					setattr(car, attr, request.POST[attr])
			self.session.commit()
			return ""
		except Exception, e:
			log.info('edit car failed: %s' % e)
			abort(400);


	@jsonify
	def getdrivers(self):
		return {'data': self.session.query(Driver.id,Driver.firstname,Driver.lastname).order_by(Driver.firstname, Driver.lastname).all()}

	
	@jsonify
	def getitems(self):
		c.items = list()
		for id in map(int, request.GET.get('driverids', "").split(',')):
			dr = self.session.query(Driver).filter(Driver.id==id).first();
			cars = self.session.query(Car).filter(Car.driverid==id).all();
			for car in cars:
				car.runs = len(self.session.query(Run.eventid).distinct().filter(Run.carid==car.id).filter(Run.eventid<100).all())
			c.items.append(self.DriverInfo(dr, cars))

		return {'data': str(render_mako('/admin/driverinfo.mako'))}



	### other ###

	@validate(form=eventForm, error_handler='edit')
	def updateevent(self):
		""" Process edit event form submission """
		self.copyvalues(self.form_result, c.event)
		self.session.commit()
		redirect(url_for(action='edit'))

	def create(self):
		""" Present form to create a new event """
		c.editpassword = True
		c.action = 'newevent'
		c.button = 'New'
		return render_mako('/admin/eventcreate.mako')

	@validate(form=eventForm, error_handler='create')
	def newevent(self):
		""" Process new event form submission """
		ev = Event()
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

	def classlist(self):
		c.action = 'processClassList'
		c.classlist = self.session.query(Class).order_by(Class.code).all()
		return render_mako('/admin/editclasses.mako')

	@validate(form=classEditForm, error_handler='classlist')
	def processClassList(self):
		data = self.form_result['grow']
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
		return render_mako('/admin/editindexes.mako')

	@validate(form=indexEditForm, error_handler='indexlist')
	def processIndexList(self):
		data = self.form_result['grow']
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
		return render_mako('/admin/purge.mako')

	def processPurge(self):
		try:
			import sqlite3
		except:
			from pysqlite2 import dbapi2 as sqlite3

		# All cars that have runs in any previous database
		carids = set()
		for s in request.POST.keys():
			conn = sqlite3.connect(os.path.join(config['seriesdir'], s))
			conn.row_factory = sqlite3.Row
			c = conn.cursor()
			c.execute("select distinct carid from runs")
			carids.update([x[0] for x in c.fetchall()])
			conn.close()

		# All drivers associated with those runs
		driverids = set()
		for s in request.POST.keys():
			conn = sqlite3.connect(os.path.join(config['seriesdir'], s))
			conn.row_factory = sqlite3.Row
			c = conn.cursor()
			c.execute("select distinct driverid from cars where id in (%s)" % (','.join(map(str, carids))))
			driverids.update([x[0] for x in c.fetchall()])
			conn.close()

		# Drivers in this database that have no unique/email
		blankdr = [x[0] for x in self.session.execute("select id from drivers where email=''")]

		delcar = self.session.execute("delete from cars where id not in (%s)" % (','.join(map(str, carids)))).rowcount
		delcar += self.session.execute("delete from cars where driverid in (%s)" % ','.join(map(str,blankdr))).rowcount
		delcar += self.session.execute("delete from cars where classcode in ('TOAM', 'TOPM', 'NOVA', 'NOVP')").rowcount
		deldr = self.session.execute("delete from drivers where id not in (%s)" % (','.join(map(str, driverids)))).rowcount
		deldr += self.session.execute("delete from drivers where id in (%s)" % ','.join(map(str,blankdr))).rowcount
		self.session.commit()
		return "Deleted %s cars and %s drivers" % (delcar, deldr)




	### Series Copying ###

	def copyseries(self):
		c.action = 'processCopySeries'
		return render_mako('/admin/copyseries.mako')


	@validate(form=seriesCopyForm, error_handler='copyseries')
	def processCopySeries(self):
		try:
			import sqlite3
		except:
			from pysqlite2 import dbapi2 as sqlite3

		""" Process settings form submission """
		log.debug("copyseriesform: %s", self.form_result)
		name = self.form_result['name']
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

