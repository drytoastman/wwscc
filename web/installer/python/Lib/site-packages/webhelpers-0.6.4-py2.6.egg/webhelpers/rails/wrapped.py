"""This module imports all the same helpers as __init__, except they're
wrapped in literal for use in projects with auto-escaping"""
import warnings

from webhelpers.html import literal
from webhelpers.rails.asset_tag import *
from webhelpers.rails.urls import *
from webhelpers.rails.javascript import *
from webhelpers.rails.tags import *
from webhelpers.rails.prototype import *
from webhelpers.rails.scriptaculous import *
from webhelpers.rails.form_tag import *
from webhelpers.rails.secure_form_tag import *
from webhelpers.rails.text import *
from webhelpers.rails.form_options import *
from webhelpers.rails.date import *
from webhelpers.rails.number import *

# Disable doc tests; they fail when the helper is literalized.
__test__ = False

# Freaky as this may be, it wraps all the HTML tags in literal so they
# continue to work right with systems that recognize literal
def wrap_helpers(localdict):
    def helper_wrapper(func):
        def wrapped_helper(*args, **kw):
            return literal(func(*args, **kw))
        try:
            wrapped_helper.__name__ = func.__name__
        except TypeError:
            # < Python 2.4 
            pass
        wrapped_helper.__doc__ = func.__doc__
        return wrapped_helper
    for name, func in localdict.iteritems():
        if not callable(func) or name in ['literal', 'current_url']:
            continue
        localdict[name] = helper_wrapper(func)
wrap_helpers(locals())

from webhelpers.rails import url_for, redirect_to

#from routes import url_for, redirect_to
#
#def deprecated(func, message):
#    def deprecated_method(*args, **kargs):
#        warnings.warn(message, DeprecationWarning, 2)
#        return func(*args, **kargs)
#    try:
#        deprecated_method.__name__ = func.__name__
#    except TypeError:
#        # Python < 2.4
#        pass
#    deprecated_method.__doc__ = "%s\n\n%s" % (message, func.__doc__)
#    return deprecated_method
#
#redirect_to = deprecated(redirect_to, 'webhelpers.rails.redirect_to is '
#                         'deprecated, import redirect_to from routes instead')
