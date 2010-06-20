"""
.. testoutput::
   :hide:

   >>> # This is so the doctests start from a clean state
   >>> from tw.core.util import install_framework; install_framework(True)

ToscaWidgets can inject resources that have been registered for injection in
the current request.

Usually widgets register them when they're displayed and they have instances of
:class:`tw.api.Resource` declared at their :attr:`tw.api.Widget.javascript` or
:attr:`tw.api.Widget.css` attributes.

Resources can also be registered manually from a controller or template by
calling their :meth:`tw.api.Resource.inject` method.

When a page including widgets is rendered, Resources that are registered for
injection arre collected in request-local
storage area (this means any thing stored here is only visible to one single
thread of execution and that its contents are freed when the request is
finished) where they can be rendered and injected in the resulting html.

ToscaWidgets' middleware can take care of injecting them automatically (default)
but they can also be injected explicitly, example::


   >>> from tw.api import JSLink, inject_resources
   >>> JSLink(link="http://example.com").inject()
   >>> html = "<html><head></head><body></body></html>"
   >>> inject_resources(html)
   '<html><head><script type="text/javascript" src="http://example.com"></script></head><body></body></html>'

Once resources have been injected they are popped from request local and
cannot be injected again (in the same request). This is useful in case
:class:`injector_middleware` is stacked so it doesn't inject them again.

Injecting them explicitly is neccesary if the response's body is being cached
before the middleware has a chance to inject them because when the cached
version is served no widgets are being rendered so they will not have a chance
to register their resources.
"""
import re
import logging

from webob import Request

import tw

from tw.core.util import MultipleReplacer

log = logging.getLogger(__name__)


__all__ = ["inject_resources", "injector_middleware"]

def injector_middleware(app, render_filter=None):
    """
    Middleware that injects resources (if any) into the page whenever the
    output is html (peeks into Content-Type header).

    Normally you don't have to stack thi yourself because
    :class:`tw.core.middleware.ToscaWidgetsMiddleware` does it when it is
    passed the ``inject_resources`` flag as True (default).
    """
    def _injector(environ, start_response):
        req = Request(environ)
        resp = req.get_response(app)
        content_type = resp.headers.get('Content-Type','text/plain').lower()
        if 'html' in content_type:
            # it crucial to us tw.framework here, and
            # not import framework from tw, as it used
            # to be, because otherwise we end up fetching
            # the wrong framework - always the default (which
            # defies the use of the StackedObjectProxy)
            resources = tw.framework.pop_resources()
            if resources:
                resp.body = inject_resources(resp.body, resources, resp.charset, render_filter=render_filter)
        return resp(environ, start_response)
    return _injector


class _ResourceInjector(MultipleReplacer):
    def __init__(self):
        return MultipleReplacer.__init__(self, {
            r'<head.*?>': self._injector_for_location('head'),
            r'</head.*?>': self._injector_for_location('headbottom', False),
            r'<body.*?>': self._injector_for_location('bodytop'),
            r'</body.*?>': self._injector_for_location('bodybottom', False)
            }, re.I|re.M)

    def _injector_for_location(self, key, after=True):
        def inject(group, resources, encoding, render_filter, environ):
            if render_filter is None:
                def render_filter(_environ, resource):
                    return resource.render()
            inj = u'\n'.join([render_filter(environ, r) for r in resources.get(key, [])])
            inj = inj.encode(encoding)
            if after:
                return group + inj
            return  inj + group
        return inject

    def __call__(self, html, resources=None, encoding=None, render_filter=None, environ=None):
        """Injects resources, if any, into html string when called.

        .. note::
           Ignore the ``self`` parameter if seeing this as
           :func:`tw.core.resource_injector.inject_resources` docstring
           since it is an alias for an instance method of a private class.

        ``html`` must be a ``encoding`` encoded string. If ``encoding`` is not
        given it will be tried to be derived from a <meta>.

        Resources for current request can be obtained by calling
        ``tw.framework.pop_resources()``. This will remove resources
        from request and a further call to ``pop_resources()`` will return an
        empty dict.
        """
        if resources is None:
            # it crucial to us tw.framework here, and
            # not import framework from tw, as it used
            # to be, because otherwise we end up fetching
            # the wrong framework - always the default (which
            # defies the use of the StackedObjectProxy)
            resources = tw.framework.pop_resources()
        if resources:
            # Only inject if there are resources registered for injection
            encoding = encoding or find_charset(html) or 'ascii'
            html = MultipleReplacer.__call__(self, html, resources, encoding, render_filter=render_filter, environ=None)
        return html


# Bind __call__ directly so docstring is included in docs
inject_resources = _ResourceInjector().__call__


_charset_re = re.compile(r"charset\s*=\s*(?P<charset>[\w-]+)([^\>])*",
                         re.I|re.M)
def find_charset(string):
    m = _charset_re.search(string)
    if m:
        return m.group('charset').lower()
