import logging
import operator
import re

from datetime import datetime, timedelta
from collections import defaultdict

from pylons import request, response, session, config, tmpl_context as c
from pylons.templating import render_mako
from pylons.controllers.util import redirect, url_for
from pylons.decorators import jsonify, validate

from nwrsc.controllers.lib.base import BaseController, BeforePage
from nwrsc.controllers.lib.entranteditor import EntrantEditor
from nwrsc.controllers.lib.objecteditors import ObjectEditor
from nwrsc.controllers.lib.cardprinting import CardPrinting
from nwrsc.controllers.lib.purgecopy import PurgeCopy

from nwrsc.lib.schema import *
from nwrsc.model import *

log = logging.getLogger(__name__)
nummatch = re.compile('(\d{6}_\d)|(\d{6})')

def _validateNumber(num):
	obj = nummatch.search(num)
	if obj is not None:
		return obj.group(1) or obj.group(2)
	raise IndexError("nothing found")
			

class AdminSession(object):

	def __init__(self, data, database):
		self.data = data
		self.database = database
		self.tokens = data.setdefault('authtokens', set())

	def isSeriesAdmin(self):
		return "%s:series"%self.database in self.tokens

	def isEventAdmin(self, eventid):
		return "%s:%s" % (self.database,eventid) in self.tokens

	def addSeriesAdmin(self):
		self.tokens.add("%s:series" % self.database)
		session.save()

	def addEventAdmin(self, eventid):
		self.tokens.add("%s:%s" % (self.database, eventid))
		session.save()

	def saveAction(self, act):
		self.data['action'] = act
		session.save()

	def getClearAction(self):
		return self.data.pop('action', '')
		session.save()


class AdminController(BaseController, EntrantEditor, ObjectEditor, CardPrinting, PurgeCopy):

	def __before__(self):
		c.stylesheets = ['/css/admin.css', '/css/forms.css', '/css/custom-theme/jquery-ui-1.8.18.custom.css', '/css/anytimec.css']
		c.javascript = ['/js/admin.js', '/js/sortabletable.js', '/js/jquery-1.7.1.min.js', '/js/jquery-ui-1.8.18.custom.min.js', '/js/superfish.js', '/js/jquery.validate.min.js', '/js/anytimec.js']
		try:
			c.javascript.append(url_for(action='scripts'))  # If we are far enough along to link to it
		except:
			pass

		if self.database is not None:
			c.events = self.session.query(Event).all()
			c.seriesname = self.database
		self.eventid = self.routingargs.get('eventid', None)
		self.action = self.routingargs.get('action', '')

		c.isLocked = self.settings.locked
		c.event = None
		if self.eventid and self.eventid.isdigit():
			c.event = self.session.query(Event).get(self.eventid)

		if self.eventid and self.action not in ('login', 'scripts'):
			self._checkauth(self.eventid, c.event)

		if self.settings.locked:
			if self.action not in ('login', 'scripts', 'index', 'printcards', 'paid', 'numbers', 'paypal', 'newentrants', 'printhelp', 'forceunlock'):
				c.seriesname = self.settings.seriesname
				c.next = self.action
				raise BeforePage(render_mako('/admin/locked.mako'))


	def _checkauth(self, eventid, event):
		if self.srcip == '127.0.0.1':
			c.isAdmin = True
			return
	
		if event is None and eventid != 's':
			c.text = "<h3>No such event for %s</h3>" % eventid
			raise BeforePage(render_mako('/admin/simple.mako'))
	
		mysession = AdminSession(session.setdefault(('admin', self.srcip), {}), self.database)

		if mysession.isSeriesAdmin():
			c.isAdmin = True
			return

		if event is not None and mysession.isEventAdmin(eventid):
			return
			
		if event is not None:
			c.request = "Need authentication token for %s" % event.name
		else:
			c.request = "Need authentication token for the series"

		mysession.saveAction(self.routingargs['action'])
		raise BeforePage(render_mako('/admin/login.mako'))
	

	def forceunlock(self):
		self.settings.locked = False
		self.settings.save(self.session)
		self.session.commit()
		redirect(url_for(action=request.GET.get('next', '')))


	def login(self):
		password = request.POST.get('password')
		mysession = AdminSession(session.setdefault(('admin', self.srcip), {}), self.database)

		if password == self.settings.password:
			mysession.addSeriesAdmin()

		for event in c.events:
			if password == event.password:
				mysession.addEventAdmin(event.id)

		action = mysession.getClearAction()
		redirect(url_for(action=action))
			

	def scripts(self):
		response.headers['Cache-Control'] = 'max-age=360' 
		response.headers.pop('Pragma', None)
		return render_mako('/forms/careditor.mako') + render_mako('/forms/drivereditor.mako')


	def index(self):
		if self.eventid and self.eventid.isdigit():
			return render_mako('/admin/event.mako')
		elif self.database is not None:
			c.text = "<h2>%s Adminstration</h2>" % self.routingargs['database']
			return render_mako('/admin/simple.mako')
		else:
			return self.databaseSelector(archived=True)



	def contactlist(self):
		c.classlist = self.session.query(Class).order_by(Class.code).all()
		c.indexlist = [""] + [x[0] for x in self.session.query(Index.code).order_by(Index.code)]
		return render_mako('/admin/contactlist.mako')


	def sendcontactlist(self):
		""" Process settings form submission """
		classes = list()
		events = list()
		for k in request.POST:
			if k.startswith('class-'):  classes.append(k[6:])
			elif k.startswith('event-'):  events.append(k[6:])

		title = "Contact List"
		query = self.session.query(Driver).join('cars', 'registration')
		if len(events) > 0: query = query.filter(Registration.eventid.in_(events))
		if len(classes) > 0: query = query.filter(Car.classcode.in_(classes))
		return self.csv("ContactList", ['firstname', 'lastname', 'email'], query)



	class WeekendReport(object):
		pass


	def weekend(self):
		""" Create a weekend report reporting unique entrants and their information """
		bins = defaultdict(list)
		events = self.session.query(Event).order_by(Event.date).all()
		for e in events:
			wed = e.date + timedelta(-(e.date.weekday()+5)%7)   # convert M-F(0-7) to Wed-Tues(0-7), formerly (2-6,0-1)
			bins[wed].append(e)

		c.weeks = dict()
		for wed in sorted(bins):
			eventids = [e.id for e in bins[wed]]
			report = self.WeekendReport()
			c.weeks[wed] = report

			report.events = bins[wed]
			report.drivers = self.session.query(Driver).join('cars', 'runs').filter(Run.eventid.in_(eventids)).distinct().all()
			report.membership = list()
			report.invalid = list()
			for d in report.drivers:
				member = d.getExtra('membership')
				try:
					report.membership.append(_validateNumber(member) or "")
				except IndexError:
					report.invalid.append(member or " ")

			report.membership.sort()  # TODO: should we take into account first letters and go totally numeric?
			report.invalid.sort()

		return render_mako('/admin/weekend.mako')


	### Settings table editor ###
	def seriessettings(self):
		c.settings = self.settings
		c.action = 'updatesettings'
		c.button = 'Update'
		return render_mako('/admin/seriessettings.mako')

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
		from nwrsc.controllers.lib.resultscalc import RecalculateResults
		response.content_type = 'text/plain'
		return RecalculateResults(self.session, self.settings)


	def printhelp(self):
		return render_mako('/admin/printhelp.mako')


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
		c.header = '<h2>Fees Collected Before %s</h2>' % c.event.name
		c.beforelist = FeeList.get(self.session, self.eventid)[-1].before
		return render_mako('/admin/feelist.mako')

	def newentrants(self):
		""" Return the list of new entrants/fees collected by event or for the series """
		if self.eventid == 's':
			c.feelists = FeeList.getAll(self.session)
			return render_mako('/admin/newentrants.mako')
		else:
			c.feelists = FeeList.get(self.session, self.eventid)
			return render_mako('/admin/newentrants.mako')

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
			log.error("setRunGroups failed: %s" % e)
			self.session.rollback()
		redirect(url_for(action='rungroups'))
		

	### other ###
	def editevent(self):
		""" Present form to edit event details """
		c.action = 'updateevent'
		c.button = 'Update'
		return render_mako('/admin/eventedit.mako')

	@validate(schema=EventSchema(), form='editevent', prefix_error=False)
	def updateevent(self):
		""" Process edit event form submission """
		c.event.merge(**self.form_result)
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
		return render_mako('/admin/eventedit.mako')

	@validate(schema=EventSchema(), form='createevent', prefix_error=False)
	def newevent(self):
		""" Process new event form submission """
		ev = Event(**self.form_result)
		self.session.add(ev)
		self.session.commit()
		redirect(url_for(eventid=ev.id, action=''))


	def deleteevent(self):
		""" Request to delete an event, verify if we can first, then do it """
		if self.session.query(Run).filter(Run.eventid==self.eventid).count() > 0:
			c.text = "<h3>%s has runs assigned to it, you cannot delete it</h3>" % (c.event.name)
			raise BeforePage(render_mako('/admin/simple.mako'))

		# no runs, kill it
		self.session.query(Registration).filter(Registration.eventid==self.eventid).delete()
		self.session.query(Event).filter(Event.id==self.eventid).delete()
		self.session.commit()
		redirect(url_for(eventid='s', action=''))


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
		return render_mako('/admin/fieldlist.mako')

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
		return render_mako('/admin/classlist.mako')


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
		return render_mako('/admin/indexlist.mako')

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

