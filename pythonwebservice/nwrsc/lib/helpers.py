"""Helper functions

Consists of functions to typically be used within templates, but also
available to Controllers. This module is available to both as 'h'.
"""
# Import helpers as desired, or define your own, ie:
# from webhelpers.html.tags import checkbox, password

from webhelpers.html.tags import stylesheet_link, javascript_link
from routes import url_for, redirect_to
from simplejson import dumps
import operator


attrgetter = operator.attrgetter

def esc(str):
	str.replace

def hide(val, hidenum):
	ret = ""
	if val is None:
		return ret
	for ii in range(0, min(len(val), hidenum)):
		ret += val[ii]

	for ii in range(hidenum, len(val)):
		if val[ii] == ' ':
			ret += '&nbsp;&nbsp;'
		else:
			ret += '*'

	return ret

def t3(val, sub = None, sign = False):
	if val is None or val == 0:
		return ''
	if sub is not None:
		val -= sub
	if type(val) is int:
		return str(val)

	if sign:	
		return "%+0.3f" % (val)
	else:
		return "%0.3f" % (val)

def ixstr(car):
	if car.indexcode != "" or car.tireindexed:
		return "(%s%s)" % (car.indexcode, car.tireindexed and "+T" or "")
	return ""

def encodesqlobj(obj):
	d = dict()
	for k in obj.__dict__.copy():
		if k.startswith("_"): continue
		d[k] = getattr(obj, k)
	return dumps(d, default=lambda x: x is None and "" or str(x))

