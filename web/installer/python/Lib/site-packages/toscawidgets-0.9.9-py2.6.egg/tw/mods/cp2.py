from pkg_resources import require
require("CherryPy <3.0")

import os, logging
import pprint

import cherrypy
from cherrypy.filters.basefilter import BaseFilter

import tw
from tw.core import resource_injector, resources
from tw.core.registry import Registry

log = logging.getLogger(__name__)

def _extract_config():
    from cherrypy.config import configs
    c = configs.get('global', {}).copy()
    c.update(configs['/'])
    return c

class TWInitFilter(BaseFilter):
    """Sort-of-emulates TWWidgetsMiddleware + Paste's RegsitryManager. Takes
    care of preparing the hostframework for a request."""

    def __init__(self, host_framework, prefix='/toscawidgets',
                 serve_files=True):
        self.serve_files = serve_files
        self.prefix = prefix
        self.host_framework = host_framework

    def on_start_resource(self):
        log.debug("TWFilter: on_start_resource")
        environ = cherrypy.request.wsgi_environ
        registry = environ.setdefault('paste.registry', Registry())
        environ['toscawidgets.prefix'] = self.prefix
        registry.prepare()
        registry.register(tw.framework, self.host_framework)
        self.host_framework.start_request(environ)

    def before_main(self):
        """Intercepts requests for static files and serves them."""
        if not self.serve_files:
            return
            
        req, resp = cherrypy.request, cherrypy.response
        path = req.path
        if path.startswith(self.host_framework.webpath):
            path = path[len(self.host_framework.webpath):]        
        if path.startswith(self.prefix):
            reg = resources.registry
            path = path[len(self.prefix)+len(reg.prefix):]
            class FakeRequest(object):
                path_info = path
                params = req.params
                environ = {}
            stream, ct, enc = reg.get_stream_type_encoding(
                FakeRequest())
            if stream:
                resp.body = stream
                if ct:
                    if enc:
                        ct += '; charset=' + enc
                    resp.headers['Content-Type'] = ct
                req.execute_main = False


    def before_finalize(self):
        # Injects resources
        log.debug("TWFilter: before_finalize")
        response = cherrypy.response
        ct = response.headers.get('content-type', 'text/html').lower()
        if 'html' in ct:
            cs = resource_injector.find_charset(ct)
            html = ''.join(response.body)
            resources = tw.framework.pop_resources()
            log.debug("Injecting Resources:")
            map(log.debug, pprint.pformat(resources).split('\n'))

            html = resource_injector.inject_resources(html=html,
                                                     resources=resources,
                                                     encoding=cs)
            response.body = [html]
            # Delete Content-Length header so finalize() recalcs it.
            response.headers.pop("Content-Length", None)
        
    def on_end_resource(self):
        log.debug("TWFilter: on_end_resource")
        try:
            environ = cherrypy.request.wsgi_environ
            self.host_framework.end_request(environ)
        finally:
            registry = environ['paste.registry']
            registry.cleanup()

def start_extension(host_framework, **filter_args):
    cherrypy.root._cp_filters.append(TWInitFilter(host_framework, 
                                     **filter_args))
    log.info("Added TWInitFilter")
