from urllib import unquote

import pkg_resources

from webob import Request

import tw
from tw.mods import base
from tw.core import resources
from tw.core.util import asbool
from tw.core.server import always_deny

__all__ = ["ToscaWidgetsMiddleware", "make_middleware"]

class ToscaWidgetsMiddleware(object):
    """
    This WSGI middleware piece takes care of creating a per-request context for
    ToscaWidgets and injecting resource links into html responses.

    It can also take care of serving those resources if `serve_resources` is
    True (default).
    """
    def __init__(self, application, host_framework, prefix='/toscawidgets',
                 inject_resources=True, serve_resources=True, require_once=False,
                 render_filter=None,
                 callback_security_default=always_deny,
                 ):
        self.callback_security_default = callback_security_default        
        self.host_framework = host_framework
        self.host_framework.middleware = self
        self.prefix = prefix
        self.serve_resources = serve_resources
        self.inject_resources = inject_resources
        self.require_once = require_once

        self.application = application

        if self.inject_resources:
            from tw.core.resource_injector import injector_middleware
            self.application = injector_middleware(self.application, render_filter)

    def __call__(self, environ, start_response):
        return self.wsgi_app(environ, start_response)

    def wsgi_app(self, environ, start_response):
        self.host_framework.start_request(environ)
        environ['paste.registry'].register(tw.framework, self.host_framework)
        #XXX Do we really need to stuff these in environ?
        environ['toscawidgets.prefix'] = self.prefix
        environ.setdefault('toscawidgets.framework', self.host_framework)
        environ.setdefault('toscawidgets.javascript.require_once', self.require_once)

        req = Request(environ)
        try:
            tw.framework.script_name = req.script_name
            if self.serve_resources and req.path_info.startswith(self.prefix):
                # Intercept request to possibly serve a static resource
                req.path_info = req.path_info[len(self.prefix):]
                req.script_name += self.prefix
                resources_app = resources.registry
                if req.path_info.startswith(resources_app.prefix):
                    req.path_info = req.path_info[len(resources_app.prefix):]
                    req.script_name += resources_app.prefix
                    resp = req.get_response(resources_app)
                    return resp(environ, start_response)
            else:
                # Pass request downstream
                resp = req.get_response(self.application)
            return resp(environ, start_response)
        finally:
            self.host_framework.end_request(environ)

#TODO: Wrap to issue deprecation warnings
TGWidgetsMiddleware = ToscaWidgetsMiddleware

def _load_from_entry_point(name):
    for ep in pkg_resources.iter_entry_points('toscawidgets.host_frameworks'):
        if ep.name == name:
            return ep

def _extract_args(args, prefix, adapters={}):
    l = len(prefix)
    nop = lambda v: v
    return dict((k[l:], adapters.get(k[l:], nop)(v))
                for k,v in args.iteritems() if k.startswith(prefix))

def _load_host_framework(host_framework):
    if ':' not in host_framework:
        ep = _load_from_entry_point(host_framework)
    else:
        ep = pkg_resources.EntryPoint.parse("hf="+host_framework)
    if ep:
        hf = ep.load(False)
    else:
        hf = None
    if not hf:
        raise LookupError("Could not load %s" % host_framework)
    return hf

def make_middleware(app, config=None, **kw):
    """
    Initializes :class:`tw.core.middleware.ToscaWidgetsMiddleware` and a
    :class:`tw.mods.base.HostFramework` and wraps the WSGI application ``app``
    with it.

    Configuration can be passed in a dict as the ``config`` parameter.


    **Available options:**

    toscawidgets.middleware.*
        These parameters will be passed to
        :class:`tw.core.middleware.ToscaWidgetsMiddleware`
        when instatntiating it. See its docstrings for details.

    toscawidgets.framework
        Name of the ``toscawidgets.host_frameworks`` entry point or
        :class:`tw.mods.base.HostFramework` subclass which shall interface with
        the framework. ``wsgi``, ``pylons`` are available. Default is
        ``wsgi``

    toscawidgets.framework.*
        Parameters for the :class:`tw.modes.base.HostFramework`. See their
        respective docstrings for accepted parameters.


    This is the ToscaWidgets#middleware paste.filter_app_factory entrypoint.
    """
    config = (config or {}).copy()
    config.update(kw)
    host_framework = config.pop('toscawidgets.framework', 'wsgi')
    if isinstance(host_framework, basestring):
        host_framework = _load_host_framework(host_framework)
    middleware_args = _extract_args(config, 'toscawidgets.middleware.', {
        'inject_resources': asbool,
        'serve_resources': asbool,
        'require_once' : asbool,
        })
    hf_args = _extract_args(config, 'toscawidgets.framework.', {
        'enable_runtime_checks': asbool,
        })
    app = ToscaWidgetsMiddleware(app, host_framework=host_framework(**hf_args),
                                 **middleware_args)
    if config.get('stack_registry', False):
        from tw.core.registry import RegistryManager
        app = RegistryManager(app)
    return app
