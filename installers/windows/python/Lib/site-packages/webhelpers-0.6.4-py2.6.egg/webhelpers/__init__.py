"""Functions useful in web applications (and other applications)

WebHelpers provides a wide variety of functions useful in any web application,
including an HTML tag generator with smart escaping, tag functions,
text-to-HTML converters, a paginator, etc.  These can be used in any web
framework.  A few Pylons-specific helpers are in ``webehlpers.pylonslib``;
these can be ported to other frameworks by simply replacing the
framework-specific API calls (request/response/session/URL).

Some helpers are also useful in non-web applications.  There are text
formatters, advanced container types, country & state abbreviations, simple
numeric statistics, etc.

The main criteria for incuding helpers are: 

* Is it useful in a wide variety of applications, especially web applications?

* Does it avoid dependencies outside the Python standard library, especially
  C extensions which are hard to install on Windows and Macintosh?

* Is it too small to be released as its own project, and is there no other
  Python project more appropriate for it?

Helper functions are organized into modules by theme.  All HTML generators are
under the ``webhelpers.html`` package, except for a few third-party modules
which are directly under ``webhelpers``.

WebHelpers depends on Routes 1.x due to the unit tests and deprecated Rails
helpers.

Users of WebHelpers 0.3 will notice significant changes in 0.6.  All helpers
ported from Rails (the ``webhelpers.rails`` package) have been deprecated, and
most have been replaced with new functions in ``webhelpers.html`` or elsewhere.
``from webhelpers import *`` no longer imports all helpers; you must
specifically import the modules or functions you want.  Javascript libraries
(Prototype, Scriptaculous, and ``link_to_remote()``) have also been deprecated
because the Javascript state-of-the-art changes too rapidly.

Pylons applications ported to Pylons 0.9.7 that depend on the deprecated Rails
helpers should add the following to myapp/lib/helpers.py::

    from webhelpers.rails.wrapped import *
    from routes import url_for, redirect_to

A few helpers have external dependencies as noted in their module docstrings.
"""
