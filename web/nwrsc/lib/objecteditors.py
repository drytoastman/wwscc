from pylons import request, tmpl_context as c
from pylons.templating import render_mako
from pylons.controllers.util import abort
from pylons.decorators import jsonify
from nwrsc.lib.titlecase import titlecase
from nwrsc.model import *
from sqlalchemy.sql import func

import logging
log = logging.getLogger(__name__)

class ObjectEditor(object):

	def editdriver(self):
		try:
			fields = self.session.query(DriverField).all()
			fieldnames = [x.name for x in fields]
			driverid = request.POST.get('driverid', None)
			log.info('request to edit driver %s' % driverid)
			driver = self.session.query(Driver).get(driverid)
			for attr in request.POST:
				if hasattr(driver, attr):
					setattr(driver, attr, request.POST[attr])
				elif attr in fieldnames:
					if len(request.POST[attr]) == 0:
						driver.delExtra(attr)
					else:
						driver.setExtra(attr, request.POST[attr])
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
				setattr(car, attr, request.POST.get(attr, ''))
			self.session.commit()
			return ""
		except Exception, e:
			log.info('edit car failed: %s' % e)
			abort(400);


	def carnumbers(self):
		code = request.POST.get('code', None)
		drid = request.POST.get('driverid', None)
		if code is None or drid is None:
			return "missing data in request"

		if self.settings.superuniquenumbers:
			query = self.session.query(Car.number).distinct().filter(Car.driverid!=int(drid))
		else:
			query = self.session.query(Car.number).distinct().filter(Car.classcode==code).filter(Car.driverid!=int(drid))

		c.used = set([x[0] for x in query])
		c.largest = self.settings.largestcarnumber
		return render_mako('/forms/carnumbers.mako')


