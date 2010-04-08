"""Helper functions

Consists of functions to typically be used within templates, but also
available to Controllers. This module is available to both as 'h'.
"""
# Import helpers as desired, or define your own, ie:
# from webhelpers.html.tags import checkbox, password

from webhelpers.html.tags import stylesheet_link, javascript_link
from routes import url_for, redirect_to

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

def t3(val, sub = None):
	if val is None or val == 0:
		return ''
	if sub is not None:
		val -= sub
	return "%0.3f" % (val)
