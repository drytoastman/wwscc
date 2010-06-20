import pkg_resources

from tw.core.view import EngineManager
from tw.core.util import RequestLocalDescriptor, disable_runtime_checks
from tw.core.registry import StackedObjectProxy, Registry

__all__ = ["HostFramework"]

class RequestLocal(object):
    def __init__(self, environ):
        self.environ = environ
        self.resources = {}

class HostFramework(object):
    """
    This class is the interface between ToscaWidgets and the framework or
    web application that's using them.

    The an instance of this class should be passed as second argument to
    :class:`tw.core.middleware.ToscaWidgetsMiddleware` which will call its
    :meth:`start_request` method at the beginning of every
    request and :meth:`end_request` when the request is over so I have a chance
    to register our per-request context.

    A request-local proxy to a configured instance is placed at the beginning
    of the request at :attr:`tw.framework`

    **Constructor's arguments:**

    engines
       An instance of :class:`tw.core.view.EngineManager`.

    default_view
       The name of the template engine used by default in the container app's
       templates. It's used to determine what conversion is neccesary when
       displaying root widgets on a template.

    translator
       Function used to translate strings.

    enable_runtime_checks
       Enables runtime checks for possible programming errors regarding
       modifying widget attributes once a widget has been initialized.
       Disabling this option can significantly reduce Widget initializatio
       time.

       .. note::
           This operation modifies the Widget class and will affect any
           application using ToscaWidgets in the same process.


    aggregation_config
      This option can either be None, or must be a dictionary. The dictionary can
      have two keys, **js** and **css**. The value of each key is a list of dicts
      with the following keys:

        - **modname** the module to create the resource link from.
        - **filename** the filename relative to the **modname** of the aggregated resources.
        - **map** the mapping-file for the aggregated resources. If not given, defaults to
          ``<filename>.map``.


    """
    request_local = StackedObjectProxy(name="ToscaWidgets per-request storage")
    request_local_class = RequestLocal

    default_view = RequestLocalDescriptor('default_view', 'toscawidgets')

    def __init__(self, engines=None, default_view='toscawidgets',
                 translator=lambda s: s, enable_runtime_checks=True, default_engine=None,
                 aggregation_config=None):
        if engines is None:
            engines = EngineManager()
        self.engines = engines
        self._default_view = default_view
        self._default_engine = default_engine
        if default_engine is None:
            self._default_engine = default_view
        self.translator = translator
        if not enable_runtime_checks:
            disable_runtime_checks()

        # these are for now not set up,
        # this is done lazy, see belowe
        self.aggregation_config = aggregation_config
        self.aggregated_js_mapping = {}
        self.aggregated_css_mapping = {}
        self.middleware = None


    def start_request(self, environ):
        """
        Called by the middleware when a request has just begun so I have a
        chance to register the request context Widgets will use for various
        things.
        """
        registry = environ['paste.registry']
        registry.register(self.request_local, self.request_local_class(environ))
        self.request_local.default_view = self._default_view

    def end_request(self, environ):
        """
        Called by the middleware when a request has just finished so I can
        clean up.
        """
        pass

    def url(self, url):
        """
        Returns the absolute path for the given url.
        """
        prefix = self.request_local.environ['toscawidgets.prefix']
        script_name = self.request_local.environ['SCRIPT_NAME']
        if hasattr(url, 'url_mapping'):
            url = url.url_mapping['normal']
        return ''.join([script_name, prefix, url])

    def register_resources(self, resources):
        """
        Registers resources for injection in the current request.
        """
        from tw.api import merge_resources
        merge_resources(self.request_local.resources, resources)

    def pop_resources(self):
        """
        Returns returns the resources that have been registered for this
        request and removes them from request-local storage area.
        """
        resources =  self.request_local.resources
        self.request_local.resources = {}
         # deal with aggregated resources
        if resources and "head" in resources:
            # This is lazy, because we otherwise run
            # into circular import issues
            if self.aggregation_config is not None:
                self._setup_aggregation_mapping()


            if self.aggregated_js_mapping:
                self._replace_resources_with_aggregates(resources,
                                                        self.aggregated_js_mapping,
                                                        JSLink,
                                                        )
            if self.aggregated_css_mapping:
                self._replace_resources_with_aggregates(resources,
                                                        self.aggregated_css_mapping,
                                                        CSSLink,
                                                        )
        return resources


    def _setup_aggregation_mapping(self):
        from tw.core.resources import JSLink, CSSLink, AggregatedJSLink, AggregatedCSSLink
        # insert into the globals. This is nasty...
        globals()["JSLink"] = JSLink
        globals()["CSSLink"] = CSSLink
        if self.aggregation_config is not None:
            config = self.aggregation_config
            if "js" in config:
                self._compute_mapping(config["js"], self.aggregated_js_mapping, AggregatedJSLink)

            if "css" in config:
                self._compute_mapping(config["css"], self.aggregated_css_mapping, AggregatedCSSLink)

        self.aggregation_config = None

    def _compute_mapping(self, configs, mapping, kind):
        aggregate_mappings = set()
        for config in configs:
            modname = config["modname"]
            filename = config["filename"]
            if "map" in config:
                map_name = config["map"]
            else:
                map_name = "%s.map" % filename

            # we don't allow double-registration
            # of an aggregate file
            if (modname, filename) in aggregate_mappings:
                raise Exception("Double aggregation file mapping: %s/%s" % (modname, filename))

            aggregate_mappings.add((modname, filename))

            resource = kind(modname=modname, filename=filename)

            for line in pkg_resources.resource_stream(modname, map_name):
                line = line.strip()
                if not line:
                    continue
                jslink_desc = tuple(line.split("|", 1))
                if jslink_desc in mapping and mapping[jslink_desc] is not resource:
                    raise Exception("Double js file mapping: %r" % (jslink_desc,))

                mapping[jslink_desc] = resource


    def _replace_resources_with_aggregates(self, resources, mapping, kind):
        replaced_resources = set()
        new_resources = []
        for resource in resources["head"]:
            if not isinstance(resource, kind):
                new_resources.append(resource)
                continue
            desc = resource.modname, resource.active_filename()
            if desc in mapping:
                aggregated_resource = mapping[desc]
                if aggregated_resource in replaced_resources:
                    continue
                replaced_resources.add(aggregated_resource)
                new_resources.append(aggregated_resource)
            else:
                new_resources.append(resource)
        resources["head"] = new_resources


