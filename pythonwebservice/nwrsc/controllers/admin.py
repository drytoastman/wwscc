import logging
import operator
import re
import io

from datetime import datetime, timedelta

#from nwrsc.controllers.lib.entranteditor import EntrantEditor
#from nwrsc.controllers.lib.objecteditors import ObjectEditor
#from nwrsc.controllers.lib.cardprinting import CardPrinting
#from nwrsc.controllers.lib.purgecopy import PurgeCopy
from flask import Blueprint, g, redirect, request, render_template, session, url_for

from nwrsc.model import *
from nwrsc.lib.postgresql import check_password

log = logging.getLogger(__name__)

Admin = Blueprint("Admin", __name__)

AUTHKEY = 'adminauth'

@Admin.before_request
def setup():
    g.title = 'Scorekeeper Admin'
    g.activeseries = Series.active()
    if AUTHKEY not in session:
        session[AUTHKEY] = {}

def isauth():
    return g.series in session[AUTHKEY]

@Admin.route("/login", methods=['POST', 'GET'])
def login():
    if request.form.get('message'): # super simple bot test (advanced bots will get by this)
        abort(404)

    if request.form.get('password'):
        if check_password(g.series, request.form.get('password').strip()):
            session[AUTHKEY][g.series] = 1
            session.modified = True
            return redirect(url_for(".index"))

    return render_template('/admin/login.html')
    

@Admin.route("/")
def index():
    return render_template('/admin/simple.html', text="<h2>{} Adminstration</h2>".format(g.series))

@Admin.route("/<int:eventid>")
def event():
    if not isauth(): return login()
    return render_template('/admin/event.html', event=Event.get(g.eventid))

## Perhaps move these to an admin like site?

@Admin.route("/<int:eventid>/audit")
def audit():
    course = request.args.get('course', 1)
    group  = request.args.get('group', 1)
    order  = request.args.get('order', 'runorder')
    event  = Event.get(g.eventid)
    audit  = Audit.audit(event, course, group)

    if order in ['firstname', 'lastname']:
        audit.sort(key=lambda obj: str.lower(str(getattr(obj, order))))
    else:
        order = 'runorder'
        audit.sort(key=lambda obj: obj.row)

    return render_template('/admin/audit.html', audit=audit, event=event, course=course, group=group, order=order)


@Admin.route("/<int:eventid>/grid")
def grid():
    order = request.args.get('order', 'number')
    groups = RunGroups.getForEvent(g.eventid)

    # Create a list of entrants in order of rungroup, classorder and [net/number]
    if order == 'position': 
        for l in Result.getEventResults(g.eventid).values():
            for d in l:
                groups.put(Entrant(**d))
    else: # number
        for e in Registration.getForEvent(g.eventid):
            groups.put(e)

    groups.sort(order)
    for go in groups.values():
        go.pad()
        go.number()

    return render_template('/admin/grid.html', groups=groups, order=order, starts=[k for k in groups if k < 100])


@Admin.route("/<int:eventid>/editevent",   endpoint='editevent')
@Admin.route("/<int:eventid>/list",        endpoint='list')
@Admin.route("/<int:eventid>/rungroups",   endpoint='rungroups')
@Admin.route("/<int:eventid>/deleteevent", endpoint='deleteevent')
@Admin.route("/<int:eventid>/printhelp",   endpoint='printhelp')
@Admin.route("/<int:eventid>/printcards",  endpoint='printcards')
@Admin.route("/<int:eventid>/numbers",     endpoint='numbers')
@Admin.route("/<int:eventid>/paid",        endpoint='paid')
@Admin.route("/<int:eventid>/paypal",      endpoint='paypal')
@Admin.route("/<int:eventid>/contactlist", endpoint='contactlist')
@Admin.route("/<int:eventid>/newentrants", endpoint='newentrants')
def notyetdone():
    return "TBD"



#####################################################################################################


class AdminController(): #BaseController, EntrantEditor, ObjectEditor, CardPrinting, PurgeCopy):

    def __before__(self):
        c.stylesheets = ['/css/admin.css']
        c.javascript = ['/js/admin.js']

        if self.database is not None:
            c.events = self.session.query(Event).all()
            c.seriesname = self.database
        self.eventid = self.routingargs.get('eventid', None)
        self.action = self.routingargs.get('action', '')

        c.isLocked = self.settings.locked
        c.settings = self.settings
        c.event = None
        if self.eventid and self.eventid.isdigit():
            c.event = self.session.query(Event).get(self.eventid)

        if c.event is None and self.eventid not in (None, 's'):
            c.text = "<h3>No such event for %s</h3>" % self.eventid
            raise BeforePage(render_template('/admin/simple.html'))

        if self.database is None or (self.action == 'index' and c.event is None):
            return

        self._checkauth(c.event)

        if self.settings.locked:
            if self.action not in ('index', 'printcards', 'paid', 'numbers', 'paypal', 'newentrants', 'printhelp', 'forceunlock'):
                c.seriesname = self.settings.seriesname
                c.next = self.action
                raise BeforePage(render_template('/admin/locked.html'))


    def _checkauth(self, event):
        if self.srcip == '127.0.0.1':
            c.isAdmin = True
            return

        try:
            digestinfo = session.setdefault(('digest', self.srcip), {})
            pwdict = Password.load(self.session)
            passwords = { "admin" : pwdict["series"] }
            if event is not None and str(event.id) in pwdict:
                passwords["event"] = pwdict[str(event.id)]

            authname = authCheck(digestinfo, self.database, passwords, request)
            if authname == "admin":
                c.isAdmin = True
        finally:
            session.save()
        

        def _validateNumber(self, num):
            try:
                    nummatch = re.compile('(\d{6}_\d)|(\d{6})')
                    obj = nummatch.search(num)
            except:
                    raise Exception("_validateNumber failed on the value '%s'" % num)
            if obj is not None:
                    return obj.group(1) or obj.group(2)
            raise IndexError("nothing found")
        
    def passwords(self):
        c.action = 'setpasswords'
        return render_template("/admin/passwords.html")

    def setpasswords(self):
        passwords = Password.load(self.session)
        for k, v in request.POST.iteritems():
            if v.strip() != '':
                passwords[k] = v
        Password.save(self.session, passwords)
        self.session.commit()
        redirect(url_for(action='index'))


    def forceunlock(self):
        self.settings.locked = False
        self.settings.save(self.session)
        self.session.commit()
        redirect(url_for(action=request.GET.get('next', '')))

    def contactlist(self):
        c.classlist = self.session.query(Class).order_by(Class.code).all()
        c.indexlist = [""] + [x[0] for x in self.session.query(Index.code).order_by(Index.code)]
        c.preselect = request.GET.get('preselect', "").split(',')

        c.drivers = dict()
        for (dr, car, reg) in self.session.query(Driver, Car, Registration).join('cars', 'registration'):
            if self.eventid.isdigit() and reg.eventid != int(self.eventid): 
                continue

            if dr.id not in c.drivers:
                dr.events = set([reg.eventid])
                dr.classes = set([car.classcode])
                c.drivers[dr.id] = dr
            else:
                dr = c.drivers[dr.id]
                dr.events.add(reg.eventid)
                dr.classes.add(car.classcode)

        if self.eventid.isdigit():
            c.title = c.event.name
            c.showevents = False
        else:
            c.title = "Series"
            c.showevents = True

        return render_template('/admin/contactlist.html')


    def downloadcontacts(self):
        """ Process settings form submission """
        idlist = request.POST['ids'].split(',')
        drivers = self.session.query(Driver).filter(Driver.id.in_(idlist)).all()
        cols = ['id', 'firstname', 'lastname', 'email', 'address', 'city', 'state', 'zip', 'phone', 'membership', 'brag', 'sponsor']
        return self.csv("ContactList", cols, drivers)



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
            report = WeekendReport()
            c.weeks[wed] = report

            report.events = bins[wed]
            report.drivers = self.session.query(Driver).join('cars', 'runs').filter(Run.eventid.in_(eventids)).distinct().all()
            report.membership = list()
            report.invalid = list()
            for d in report.drivers:
                if d.membership is None:
                    d.membership = ""
                try:
                    report.membership.append(self._validateNumber(d.membership) or "")
                except IndexError:
                    report.invalid.append(d.membership or " ")

            report.membership.sort()  # TODO: should we take into account first letters and go totally numeric?
            report.invalid.sort()

        return render_template('/admin/weekend.html')


    ### Settings table editor ###
    def seriessettings(self):
        c.settings = self.settings
        c.parentlist = self.lineage(self.settings.parentseries)
        c.action = 'updatesettings'
        c.button = 'Update'
        return render_template('/admin/seriessettings.html')


    #@validate(schema=SettingsSchema(), form='seriessettings', prefix_error=False)
    def updatesettings(self):
        """ Process settings form submission """
        self.settings.set(self.form_result)
        self.settings.save(self.session)

        for key, fileobj in self.form_result.iteritems():
            if hasattr(fileobj, 'filename') and fileobj.type.startswith("image/"):
                Data.set(self.session, key, fileobj.value, fileobj.type)

            if key.startswith('blank'):
                try:
                    from PIL import Image
                except:
                    import Image
                output = io.BytesIO()
                Image.new('RGB', (4,4), (255,255,255)).save(output, "PNG")
                Data.set(self.session, key[5:], output.getvalue(), 'image/png')
                output.close()

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
        return render_template('/admin/editor.html')

    def savecode(self):
        name = str(request.POST.get('name', None))
        data = str(request.POST.get('data', ''))
        if name is not None:
            Data.set(self.session, name, data)
            self.session.commit()
            redirect(url_for(action='editor', name=name))


    def cleanup(self):
        Registration.updateFromRuns(self.session)
        c.text = "<h3>Cleaned</h3>"
        return render_template('/admin/simple.html')


    def recalc(self):
        return render_template('/admin/recalc.html')
        
    def dorecalc(self):
        from nwrsc.lib.resultscalc import RecalculateResults
        response.content_type = 'text/plain'
        return RecalculateResults(self.session, self.settings)


    def printhelp(self):
        return render_template('/admin/printhelp.html')

    def restricthelp(self):
        return render_template('/admin/restricthelp.html')


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

        return render_template('/admin/numberlist.html')

    def paid(self):
        """ Return the list of fees paid before this event """
        c.header = '<h2>Fees Collected Before %s</h2>' % c.event.name
        c.beforelist = FeeList.get(self.session, self.eventid)[-1].before
        return render_template('/admin/feelist.html')

    def newentrants(self):
        """ Return the list of new entrants/fees collected by event or for the series """
        if self.eventid == 's':
            c.feelists = FeeList.getAll(self.session)
            return render_template('/admin/newentrants.html')
        else:
            c.feelists = FeeList.get(self.session, self.eventid)
            return render_template('/admin/newentrants.html')

    def paypal(self):
        """ Return a list of paypal transactions for the current event """
        c.payments = self.session.query(Payment).filter(Payment.eventid==self.eventid).all()
        c.payments.sort(key=lambda obj: obj.driver.lastname)
        return render_template('/admin/paypal.html')    

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
        return render_template('/admin/editrungroups.html')


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
        except Exception as e:
            log.error("setRunGroups failed: %s" % e)
            self.session.rollback()
        redirect(url_for(action='rungroups'))
        

    ### other ###
    def editevent(self):
        """ Present form to edit event details """
        c.action = 'updateevent'
        c.button = 'Update'
        return render_template('/admin/eventedit.html')

    #@validate(schema=EventSchema(), form='editevent', prefix_error=False)
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
        c.event.doublespecial = False
        c.event.date = datetime.today()
        c.event.regopened = datetime.today()
        c.event.regclosed = datetime.today()
        return render_template('/admin/eventedit.html')

    #@validate(schema=EventSchema(), form='createevent', prefix_error=False)
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
            raise BeforePage(render_template('/admin/simple.html'))

        # no runs, kill it
        self.session.query(Registration).filter(Registration.eventid==self.eventid).delete()
        self.session.query(Event).filter(Event.id==self.eventid).delete()
        self.session.commit()
        redirect(url_for(eventid='s', action=''))


    def list(self):
        query = self.session.query(Driver,Car,Registration).join('cars', 'registration').filter(Registration.eventid==self.eventid)
        c.classdata = ClassData(self.session)
        c.registered = {}
        for (driver, car, reg) in query.all():
            if car.classcode not in c.registered:
                c.registered[car.classcode] = []
            c.registered[car.classcode].append(RegObj(driver, car, reg))
        return render_template('/admin/entrylist.html')

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
        return render_template('/admin/fieldlist.html')

    #@validate(schema=DriverFieldListSchema(), form='fieldlist')
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
        return render_template('/admin/classlist.html')


    #@validate(schema=ClassListSchema(), form='classlist')
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
        return render_template('/admin/indexlist.html')

    #@validate(schema=IndexListSchema(), form='indexlist')
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


    def invalidcars(self):
        c.classdata = ClassData(self.session)
        c.invalidnumber = []
        c.invalidclass = []
        c.invalidindex = []
        c.unindexedclass = []
        c.restrictedindex = []

        for car in self.session.query(Car):
            if car.number is None:
                c.invalidnumber.append(car)

            if not car.classcode or car.classcode not in c.classdata.classlist:
                c.invalidclass.append(car)

            if c.classdata.classlist[car.classcode].carindexed:
                if not car.indexcode or car.indexcode not in c.classdata.indexlist:
                    c.invalidindex.append(car)

            if not c.classdata.classlist[car.classcode].carindexed:
                if car.indexcode:
                    c.unindexedclass.append(car)

            if car.classcode:
                restrict = c.classdata.classlist[car.classcode].restrictedIndexes()
                if car.indexcode in restrict[0]:
                    c.restrictedindex.append(car)
                if car.indexcode in restrict[1] and car.tireindexed:
                    c.restrictedindex.append(car)

        return render_template('/admin/invalidcars.html')

