from pylons import request, tmpl_context as c
from pylons.templating import render_mako
from pylons.controllers.util import abort
from pylons.decorators import jsonify
from nwrsc.lib.titlecase import titlecase
from nwrsc.model import *
from sqlalchemy.sql import func

import logging
log = logging.getLogger(__name__)

class EntrantEditor(object):

	class DriverInfo(object):
		def __init__(self, d, c):
			self.driver = d
			self.cars = c
			# Make no values into blank strings
			for k, v in self.driver.__dict__.iteritems(): 
				if v is None:
					setattr(self.driver, k, "")
			for car in self.cars:
				for k, v in car.__dict__.iteritems(): 
					if v is None:
						setattr(car, k, "")

	def drivers(self):
		c.classdata = ClassData(self.session)
		c.fields = self.session.query(DriverField).all()
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


	def titlecasedriver(self):
		try:
			driverid = request.POST.get('driverid', None)
			log.info('request to titlecase driver %s' % driverid)
			dr = self.session.query(Driver).get(driverid)
			for attr in ('firstname', 'lastname', 'address', 'city', 'state'):
				setattr(dr, attr, titlecase(getattr(dr, attr), attr))
			self.session.commit()
			return "";
		except Exception, e:
			log.info('title case driver failed: %s' % e)
			abort(400);

	def titlecasecar(self):
		try:
			carid = request.POST.get('carid', None)
			log.info('request to titlecase car %s' % carid)
			car = self.session.query(Car).get(carid)
			for attr in ('make', 'model', 'color'):
				setattr(car, attr, titlecase(getattr(car, attr), attr))
			self.session.commit()
			return "";
		except Exception, e:
			log.info('title case car failed: %s' % e)
			abort(400);


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


	@jsonify
	def getdrivers(self):
		return {'data': self.session.query(Driver.id,Driver.firstname,Driver.lastname).order_by(func.lower(Driver.firstname), func.lower(Driver.lastname)).all()}

	
	@jsonify
	def getitems(self):
		c.items = list()
		c.fields = self.session.query(DriverField).all()
		for id in map(int, request.GET.get('driverids', "").split(',')):
			dr = self.session.query(Driver).filter(Driver.id==id).first();
			cars = self.session.query(Car).filter(Car.driverid==id).all();

			# This just gets the number of runs for the car for all events
			for car in cars:
				car.runs = len(self.session.query(Run.eventid).distinct().filter(Run.carid==car.id).filter(Run.eventid<100).all())

			# Preload the extra fields
			for field in c.fields:
				setattr(dr, field.name, dr.getExtra(field.name))

			c.items.append(self.DriverInfo(dr, cars))

		return {'data': str(render_mako('/admin/driverinfo.mako'))}


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


