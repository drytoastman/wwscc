import sys
import doctest

from new import instancemethod
from glob import glob
from unittest import TestCase
from itertools import imap, chain
from tw.core.util import install_framework

import pkg_resources

__all__ = [
    "RequireMixin", 
    "WidgetMixin", 
    "WidgetTestCase", 
    "WidgetRequireTestCase",
    "get_doctest_suite",
    ]

class RequireMixin(object):
    """
    Doesn't run the tests in the TestCases that inherit from this mixin
    class if the package requisites in 'require' are not met.
    """
    require = []
    def __init__(self, *args, **kw):
        try:
            pkg_resources.require(*self.require)
        except pkg_resources.DistributionNotFound:
            name = ':'.join([self.__class__.__module__, self.__class__.__name__])
            reqs = self.require
            self._message_displayed = False
            def dummy_run(self, result=None):
                if not self._message_displayed:
                    print >> sys.stderr, "Skipping all tests in %s due to missing " \
                                         "requirements: %r" % (name, reqs)
                    self._message_displayed = True
            self.run = instancemethod(dummy_run, self, self.__class__)
        TestCase.__init__(self, *args, **kw)
            
class WidgetMixin(object):
    widget_kw = {}
    def setUp(self):
        install_framework(force=True)
        if hasattr(self, 'TestWidget'):
            self.widget = self.TestWidget('test', **self.widget_kw)
    def tearDown(self):
        if hasattr(self, 'TestWidget'):
            del self.widget

    def assertInOutput(self, strings, *args, **kw):
        output = self.widget.render(*args, **kw)
        if isinstance(strings, basestring):
            strings = [strings]
        for s in strings:
            self.failUnless(s in output, "%s\n\n%r not in output" %(output,s))

    def assertInOutputIC(self, strings, *args, **kw):
        output = self.widget.render(*args, **kw).lower()
        if isinstance(strings, basestring):
            strings = [strings]
        for s in strings:
            s = s.lower()
            self.failUnless(s in output, "%s\n\n%r not in output" %(output,s))

    def assertNotInOutput(self, strings, *args, **kw):
        output = self.widget.render(*args, **kw)
        if isinstance(strings, basestring):
            strings = [strings]
        for s in strings:
            self.failUnless(
                s not in output, "%s\n\n%r in output" %(output,s)
                )

    def assertNotInOutputIC(self, strings, *args, **kw):
        output = self.widget.render(*args, **kw).lower()
        if isinstance(strings, basestring):
            strings = [strings]
        for s in strings:
            s = s.lower()
            self.failUnless(
                s not in output, "%s\n\n%r in output" %(output,s)
                )

    def assertInStaticCalls(self, strings):
        """Asserts given strings are included in the widget's static
        js calls."""
        from tw.core.resources import JSFunctionCalls
        calls = JSFunctionCalls(function_calls=self.widget._js_calls)
        output = calls.render()
        if isinstance(strings, basestring):
            strings = [strings]
        for s in strings:
            self.failUnless(
                s in output, "%s\n\n%r not in static calls" %(output,s)
                )

    def assertInDynamicCalls(self, strings, *args, **kw):
        """Asserts given strings are included in the widget's dynamic
        js calls."""
        from tw.core.resources import dynamic_js_calls
        location = kw.pop('location', 'bodybottom')
        self.widget.render(*args, **kw)
        output = dynamic_js_calls.call_widgets[location].render()
        if isinstance(strings, basestring):
            strings = [strings]
        for s in strings:
            self.failUnless(
                s in output, "%s\n\n%r not in dynamic calls" %(output,s)
                )

class WidgetTestCase(WidgetMixin, TestCase):
    pass

class WidgetRequireTestCase(RequireMixin, WidgetTestCase):
    pass

def get_doctest_suite(doctest_files, doctest_modules):
    doctest_files = chain(*(imap(glob, doctest_files)))
    suite = doctest.DocFileSuite(
        *tuple(doctest_files),
        **{'optionflags':doctest.ELLIPSIS|doctest.NORMALIZE_WHITESPACE,
           'module_relative':False,
           }
        )
    for mod in doctest_modules:
        try:
            suite.addTests(doctest.DocTestSuite(
                mod,
                **{'optionflags':doctest.ELLIPSIS|doctest.NORMALIZE_WHITESPACE}
                ))
        except ValueError:
            pass # Mod has probably no doctests... ignore it
    return suite
