from pylons import request, tmpl_context as c
from pylons.templating import render_mako
from pylons.controllers.util import abort
from pylons.decorators import validate
from nwrsc.lib.schema import *
from nwrsc.model import *

import logging
log = logging.getLogger(__name__)

class ObjectEditor(object):

	def _extractDriver(self, driver):
		fields = self.session.query(DriverField).all()
		fieldnames = [x.name for x in fields]
		for attr in self.form_result:
			if hasattr(driver, attr):
				setattr(driver, attr, self.form_result[attr])
			elif attr in fieldnames:
				if len(self.form_result[attr]) == 0:
					driver.delExtra(attr)
				else:
					driver.setExtra(attr, self.form_result[attr])

		return driver


	@validate(schema=DriverSchema())
	def editdriver(self):
		try:
			driverid = self.form_result['driverid']
			log.debug('request to edit driver %s' % driverid)
			self._extractDriver(self.session.query(Driver).get(driverid))
			self.session.commit()
		except Exception, e:
			log.info('edit driver failed: %s' % e)
			abort(400);


	@validate(schema=DriverSchema())
	def newdriver(self):
		try:
			log.debug('request to create driver')
			self.session.add(self._extractDriver(Driver()))
			self.session.commit()
		except Exception, e:
			log.info('new driver failed: %s' % e)
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


	def _extractCar(self, car):
		for attr in ('year', 'make', 'model', 'color', 'number', 'classcode', 'indexcode', 'tireindexed'):
			setattr(car, attr, request.POST.get(attr, ''))
		return car

	def editcar(self):
		try:
			carid = int(request.POST.get('carid', None))
			if carid <= 0:
				return self.newcar()
			log.info('request to edit car %s' % carid)
			self._extractCar(self.session.query(Car).get(carid))
			self.session.commit()
			return ""
		except Exception, e:
			log.info('edit car failed: %s' % e)
			abort(400);

	def newcar(self):
		try:
			log.info('request to add car')
			driverid = int(request.POST.get('driverid', None))
			self.session.add(self._extractCar(Car(driverid=driverid)))
			self.session.commit()
			return ""
		except Exception, e:
			log.info('new car failed: %s' % e)
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


