
from formencode import Schema, ForEach, FancyValidator
from formencode.validators import Number, String, Bool, Int
from formencode.variabledecode import NestedVariables

from datetime import datetime

class SDate(FancyValidator):
	def _to_python(self, value, state):
		try:
			return datetime.strptime(value, "%m/%d/%Y")
		except ValueError, v:
			raise Invalid(str(v), value, state)

class SDateTime(FancyValidator):
	def _to_python(self, value, state):
		try:
			return datetime.strptime(value, "%m/%d/%Y %H:%M")
		except ValueError, v:
			raise Invalid(str(v), value, state)



class ClassSchema(Schema):
	allow_extra_fields = True
	filter_extra_fields = True
	code = String(not_empty=True)
	descrip = String(not_empty=True)
	eventtrophy = Bool()
	champtrophy = Bool()
	carindexed = Bool()
	classindexed = Bool()
	classmultiplier = Number(not_empty=True)
	numorder = Int()
	countedruns = Int(if_empty=0)
	
class ClassListSchema(Schema):
	pre_validators = [NestedVariables()] 
	allow_extra_fields = True
	filter_extra_fields = True
	clslist = ForEach(ClassSchema())


class IndexSchema(Schema):
	allow_extra_fields = True
	filter_extra_fields = True
	code = String(not_empty=True)
	descrip = String(not_empty=True)
	value = Number(not_empty=True)

class IndexListSchema(Schema):
	pre_validators = [NestedVariables()] 
	allow_extra_fields = True
	filter_extra_fields = True
	idxlist = ForEach(IndexSchema())


class SettingsSchema(Schema):
	allow_extra_fields = True
	filter_extra_fields = True
	seriesname = String(not_empty=True)
	password = String(not_empty=True)
	largestcarnumber = Int(min=99)
	minevents = Int(if_empty=0)
	useevents = Int(not_empty=True)
	sponsorlink = String()
	ppoints = String()
	superuniquenumbers = Bool()
	locked = Bool()


class EventSchema(Schema):
	allow_extra_fields = True
	filter_extra_fields = True
	name = String(not_empty=True)
	date = SDate()
	password = String(not_empty=True)
	location = String()
	sponsor = String()
	host = String()
	designer = String()
	chair = String()
	ispro = Bool()
	courses = Int(min=1, not_empty=True)
	runs = Int(min=1, not_empty=True)
	countedruns = Int(if_empty=0)
	conepen = Number(if_empty=2.0)
	gatepen = Number(if_empty=10.0)
	segments = String()
	regopened = SDateTime()
	regclosed = SDateTime()
	perlimit = Int(min=1)
	totlimit = Int(min=0)
	paypal = String()
	cost = Int()
	snail = String()
	notes = String()
	

class CopySeriesSchema(Schema):
	allow_extra_fields = True
	filter_extra_fields = True
	name = String(not_empty=True)
	password = String(not_empty=True)
	settings = Bool()
	data = Bool()
	classes = Bool()
	drivers = Bool()
	cars = Bool()
	prevlist = Bool()


class LoginSchema(Schema):
	allow_extra_fields = True
	filter_extra_fields = True
	firstname = String(not_empty=True)
	lastname = String(not_empty=True)
	email = String(not_empty=True)

class DriverSchema(Schema):
	allow_extra_fields = True
	filter_extra_fields = True
	firstname = String(not_empty=True)
	lastname = String(not_empty=True)
	email = String(not_empty=True)

