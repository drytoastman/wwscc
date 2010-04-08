from data import PrevEntry
from event import Event

class FeeList(object):

	basic = """select distinct lower(d.firstname) as firstname, lower(d.lastname) as lastname 
			from runs as r, cars as c, drivers as d 
			where r.carid=c.id and c.driverid=d.id"""

	beforeQuery = basic + " and r.eventid in (select id from events where date < (select date from events where id=:eventid))"
	afterQuery = basic + " and r.eventid=:eventid"

	def __init__(self, setbefore, setafter):
		self.before = list(setbefore)
		self.before.sort(key=lambda x:(x[1], x[0]))
		self.during = list(setafter.difference(setbefore))
		self.during.sort(key=lambda x:(x[1], x[0]))


	@classmethod
	def get(cls, session, eventid):
		setbefore = set()
	
		for x in session.query(PrevEntry):
			setbefore.add((x.firstname.strip().lower(), x.lastname.strip().lower()))
	
		for x in session.execute(FeeList.beforeQuery, params={'eventid':eventid}).fetchall():
			setbefore.add((x.firstname.strip().lower(), x.lastname.strip().lower()))
		
		setafter = setbefore.copy()
		for x in session.execute(FeeList.afterQuery, params={'eventid':eventid}).fetchall():
			setafter.add((x.firstname.strip().lower(), x.lastname.strip().lower()))

		return FeeList(setbefore, setafter)


	@classmethod
	def getAll(cls, session):
		
		ret = []
		setbefore = set()
		for x in session.query(PrevEntry):
			setbefore.add((x.firstname.strip().lower(), x.lastname.strip().lower()))
	
		for event in session.query(Event): #.order_by(Event.date):
			setafter = setbefore.copy()
			for x in session.execute(FeeList.afterQuery, params={'eventid':event.id}).fetchall():
				setafter.add((x.firstname.strip().lower(), x.lastname.strip().lower()))

			e = {}
			e['name'] = event.name
			e['feelist'] = FeeList(setbefore, setafter)
			ret.append(e)
			
			setbefore.update(setafter)

		return ret

