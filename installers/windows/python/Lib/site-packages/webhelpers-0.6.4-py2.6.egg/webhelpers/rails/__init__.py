"""Helper functions ported from Rails"""
import warnings

warnings.warn("""\
The webhelpers.rails package is deprecated.
- Please begin migrating to the new helpers in webhelpers.html,
  webhelpers.text, webhelpers.number, etc.  
- Import url_for() directly from routes, and redirect_to() from
  pylons.controllers.util (if using Pylons) or from routes.
- All Javascript support has been deprecated.  You can write link_to_remote()
  yourself or use one of the third-party Javascript libraries.""",
    DeprecationWarning, 2)

from routes import url_for, redirect_to
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

__pudge_all__ = locals().keys()
__pudge_all__.sort()


def deprecated(func, message):
    def deprecated_method(*args, **kargs):
        warnings.warn(message, DeprecationWarning, 2)
        return func(*args, **kargs)
    try:
        deprecated_method.__name__ = func.__name__
    except TypeError:
        # Python < 2.4
        pass
    deprecated_method.__doc__ = "%s\n\n%s" % (message, func.__doc__)
    return deprecated_method

redirect_to = deprecated(redirect_to, """\
webhelpers.rails.redirect_to is deprecated; import redirect_to from
pylons.controllers.util (if using Pylons) or routes instead""")

url_for = deprecated(url_for, """\
webhelpers.rails.url_for is deprecated; import url_for from routes instead""")
