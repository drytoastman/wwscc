import sys
import logging
import re
import threading

from pkg_resources import iter_entry_points, load_entry_point

import tw
from tw.core.exceptions import WidgetException
from tw.core.util import LRUCache


__all__ = ["EngineManager", "Renderable", "EngineException", "display",
           "render"]


log = logging.getLogger(__name__)


class EngineException(WidgetException):
    pass

#XXX: This make_renderer/get_render_method/etc... is a mess which should be
#     cleaned sometime
def make_renderer(method, doc=None):
    def _renderer(self, renderable, **kw):
        template = kw.pop('template', renderable.template)
        if template is None:
            return
        origin = dynamic_best_engine(renderable, kw)
        destination = kw.get('displays_on', renderable.displays_on)
        if origin != 'cheetah':
            template = self.load_template(template, origin)
        renderer = self.get_render_method(origin, destination, method)
        output = renderer(info=kw, template=template)
        if method == 'display':
            output = self.adapt_output(output, destination)
        # Make sure string output is always unicode
        if isinstance(output, str):
            output = unicode(output, 'utf-8')
        return output
    _renderer.func_name = method
    _renderer.__doc__ = doc
    return _renderer

class EngineManager(dict):
    """
    Manages availble templating engines.
    """
    default_view = 'toscawidgets'

    def __init__(self, extra_vars_func=None, options=None, load_all=False):
        self.extra_vars_func = extra_vars_func
        self.options = options
        self._cache = LRUCache(50)
        self._lock = threading.Lock()
        if load_all:
            self.load_all()


    def __repr__(self):
        return "< %s >" % self.__class__.__name__


    def load_engine(self, name, options=None, extra_vars_func=None,
                    distribution=None):
        factory = None
        if distribution:
            factory = load_entry_point(
                distribution, "python.templating.engines", name)
        else:
            for entrypoint in iter_entry_points("python.templating.engines"):
                if entrypoint.name == name:
                    factory = entrypoint.load()

        if factory is None:
            raise EngineException("No plugin available for engine '%s'" % name)

        options = options or self.options or {}
        options = options.copy()
        # emulate Kid and Genshi's dotted-path notation lookup
        options.setdefault('mako.directories', []).extend(sys.path)
        # make sure mako produces utf-8 output so we can decode it and use
        # unicode internally
        options['mako.output_encoding'] = 'utf-8'
        extra_vars_func = extra_vars_func or self.extra_vars_func

        self._lock.acquire()
        try:
            self[name] = factory(extra_vars_func, options)
        finally:
            self._lock.release()

    def __getitem__(self, name):
        """
        Return a Buffet plugin by name. If the plugin is not loaded it
        will try to load it with default arguments.
        """
        try:
            return dict.__getitem__(self, name)
        except KeyError:
            self.load_engine(name)
            return dict.__getitem__(self, name)


    def load_all(self, engine_options=None, stdvars=None, raise_error=False):
        for ep in iter_entry_points("python.templating.engines"):
            try:
                self.load_engine(ep.name, engine_options, stdvars)
            except:
                log.warn("Failed to load '%s' template engine: %r",
                         ep.name, sys.exc_info())
                if raise_error:
                    raise


    def load_template(self, template, engine_name):
        """Return's a compiled template and it's enginename"""
        output = None
        if isinstance(template, basestring) and _is_inline_template(template):
            # Assume inline template, try to fetch from cache
            key = (template, engine_name)
            try:
                output = self._cache[key]
                #log.debug("Cache hit for: %s", `key`)
            except KeyError:
                #log.debug("Cache miss for: %s", `key`)
                output = self._cache[key] = self[engine_name].load_template(
                    None, template_string = template
                    )
        elif isinstance(template, basestring):
            # Assume template path
            output = self[engine_name].load_template(template)
        else:
            # Assume compiled template
            output = template
        return output

    display = make_renderer('display', doc=\
        """
        Displays the renderable. Returns appropiate output for target template
        engine
        """)

    render = make_renderer('render', doc=\
        """
        Returns the serialized output in a string.
        Useful for debugging or to return to the browser as-is.
        """)

    def get_render_method(self, origin, destination, method):
        engine = self[origin]
        # Only pass-through Element/Stream output if rendering on the same
        # template engine as the one producing output
        if method == 'display' and \
          origin == destination and origin in ['kid', 'genshi']:
            return engine.transform
        # In any other case render as a string
        def _render_xhtml(**kw):
            kw.update(format='xhtml')
            return engine.render(**kw)
        return _render_xhtml


    def adapt_output(self, output, destination):
        # Handle rendering strings on Genshi so they're not escaped
        if isinstance(output, basestring) and destination == 'genshi':
            from genshi.input import HTML
            output = HTML(output)
        # Handle rendering strings on Kid so they're not escaped
        elif isinstance(output, basestring) and destination == 'kid':
            from kid import XML
            output = XML(output)
        return output

# Available as a public symbol for easier extension with PEAK-Rules for
# backwards compatibility
display = EngineManager.display.im_func
render = EngineManager.render.im_func

def choose_engine(obj, engine_name=None):
    tpl = obj.template
    if isinstance(tpl, basestring) and not _is_inline_template(tpl):
        colon = tpl.find(":")
        if colon > -1:
            engine_name = tpl[:colon]
            tpl = tpl[colon+1:]
    return engine_name, tpl

def dynamic_best_engine(renderable, params):
    try:
        ideal_engine = params['displays_on']
    except KeyError:
        ideal_engine = renderable.displays_on

    if ideal_engine not in renderable.available_engines:
        try:
            best_engine = params['engine_name']
        except KeyError:
            best_engine = renderable.engine_name
    else:
        best_engine = ideal_engine
    assert best_engine is not None
    return best_engine

_is_inline_template = re.compile(r'(<|\n|\$)').search

class Renderable(object):
    """Base class for all objects that the EngineManager can render"""

    engine_name = 'toscawidgets'
    available_engines = []
    template = None


    def displays_on(self):
        return tw.framework.default_view
    displays_on = property(displays_on, doc="""\
        Where the Renderable is being displayed on
        """)

    def __new__(cls, *args, **kw):
        obj = object.__new__(cls)
        obj.template = kw.pop("template", obj.template)
        engine_name = kw.pop("engine_name", None)
        if obj.template is not None:
            colon_based_engine_name, obj.template = choose_engine(obj)
            # if there is a colon in the engine name, the available engines should be narrowed
            # to that engine.
            if colon_based_engine_name:
                obj.available_engines = [colon_based_engine_name]
            engine_name = colon_based_engine_name
        if engine_name:
            obj.engine_name = engine_name
        return obj

    def render(self, **kw):
        kw.setdefault('_', tw.framework.translator)
        return tw.framework.engines.render(self, **kw)

    def display(self, **kw):
        kw.setdefault('_', tw.framework.translator)
        return tw.framework.engines.display(self, **kw)
