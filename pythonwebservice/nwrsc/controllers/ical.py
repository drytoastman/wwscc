from pylons import request, response
from sqlalchemy import create_engine
from nwrsc.model import *
from nwrsc.controllers.lib.base import BeforePage, BaseController

from collections import defaultdict
import icalendar

class IcalController(BaseController):

	def __before__(self):
		pass

	def _encode(self, head, o):
		return JEncoder(indent=1).encode(o)

	def index(self):
		return "No type provided."

	def registered(self):
		first = self.routingargs.get('first', None)
		last = self.routingargs.get('last', None)
		email = self.routingargs.get('email', None)
		events = defaultdict(list)

		for db in self._databaseList(archived=False, driver=Driver(firstname=first, lastname=last, email=email)):
			if db.driver is None:
				continue

			path = self.databasePath(db.name)
			engine = create_engine('sqlite:///%s' % path)
			self.session.bind = engine

			for entry in self.session.query(Registration).join('car').join('event').filter(Car.driverid == db.driver.id).order_by(Event.date):
				events[entry.event.name, entry.event.date].append(entry.car.classcode)
		
		toencode = list()
		for (name, date), codes in events.iteritems():
			event = icalendar.Event()
			event.add('summary', "%s: %s" % (name, ','.join(codes)))
			event.add('dtstart', date)
			event['uid'] = 'SCOREKEEPER-CALENDAR-%s-%s' % (name.replace(' ',''), date)
			toencode.append(event)
		
		return self._publishEvents(toencode)


	def _publishEvents(self, events):
		cal = icalendar.Calendar()
		cal.add('prodid', '-//Scorekeeper Calendar')
		cal.add('version', '2.0')
		cal.add('x-wr-calname;value=text', 'scorekeeper')
		cal.add('method', 'publish');
		for event in events:
			cal.add_component(event)

		response.headers['Content-type'] = 'text/calendar'
		return cal.to_ical()

