import logging
from decorator import decorator

from tw.api import retrieve_resources
from tw.mods.base import HostFramework
from tw.core.view import EngineManager

import pylons
from pylons.util import AttribSafeContextObj, ContextObj
from pylons.i18n import ugettext
from pylons.templating import render

from formencode import Invalid

__all__ = ["PylonsHostFramework", "validate", "render_response", "render",
           "valid"]

log = logging.getLogger(__name__)

class PylonsHostFramework(HostFramework):
    """HostFramework object for Pylons.

    Based on customization done in: http://wiki.pylonshq.com/display/pylonscookbook/An+Alternative+ToscaWidgets+Setup+with+Mako

    """
    def __init__(self, engines=None, default_view='mako', translator=ugettext,
                 template_paths=[], engine_options=None):
        if engines is None:
            opts = engine_options or {}
            opts.setdefault('mako.directories', template_paths)
            evf = opts.pop('extra_vars_func', None)
            engines = EngineManager(extra_vars_func=evf, options=opts)
        super(PylonsHostFramework, self).__init__(engines, default_view,
                                                  translator)
        



def validate(form=None, validators=None, error_handler=None, post_only=True, 
             state_factory=None):
    """This decorator will use valid() to automatically validate input.

    If validation is successful the decorated function will be called and the
    valid result dict will be saved as ``self.form_result``.
    
    Otherwise, the action will be re-run as if it was a GET without setting
    ``form_result`` and if the form is redisplayed it will display errors and
    previous input values.
    
    If the decorated method did not originally display the
    form, then ``error_handler`` should be the name of the method (in the same
    controller) that originally displayed it.
    
    If you'd like validate to also check GET (query) variables during its 
    validation, set the ``post_only`` keyword argument to False.
    """
    def wrapper(func, self, *args, **kwargs):
        """Decorator Wrapper function"""
        if not valid(self, form=form, validators=validators,
                     post_only=post_only,
                     state_factory=state_factory):
            if error_handler:
                environ = pylons.request.environ
                environ['REQUEST_METHOD'] = 'GET'
                environ['pylons.routes_dict']['action'] = error_handler
                return self._dispatch_call()
        return func(self, *args, **kwargs)
    return decorator(wrapper)


def valid(controller, form=None, validators=None, post_only=True,
          state_factory=None):
    """Validate input using a ToscaWidgetsForms form.
    
    Given a TW form or dict of validators, valid() will attempt to validate
    the form or validator dict as long as a POST request is made. No 
    validation is performed on GET requests unless post_only is False.
    
    If validation was succesfull, the valid result dict will be saved
    as ``controller.form_result`` and valid() will return True.

    Otherwise the invalid exception will be stored at
    ``controller.validation_exception`` and valid() will return False.

    If you'd like validate to also check GET (query) variables during its 
    validation, set the ``post_only`` keyword argument to False.
    """
    request = pylons.request._current_obj()

    if post_only:
        params = request.POST.copy()
    else:
        params = request.params.copy()

    errors = {}

    if state_factory:
        state = state_factory()
    else:
        from tw import framework
        # Pass registered translator for formencode
        state = type('State', (object,),
                     {'_':staticmethod(framework.translator)})

    if form:
        try:
            controller.form_result = form.validate(params, state=state)
        except Invalid, e:
            log.debug("Validation failed with:\n%s", e)
            controller.validation_exception = e
            errors = e.error_dict or e

    if validators:
        if isinstance(validators, dict):
            if not hasattr(controller, 'form_result'):
                controller.form_result = {}
            for field, validator in validators.iteritems():
                try:
                    controller.form_result[field] = \
                        validator.to_python(decoded[field] or None, state)
                except Invalid, error:
                    errors[field] = error
    if errors:
        controller.errors = errors
        return False
    return True


# Note: render and render_response are DEPRECATED

def _render_func_wrapper(func):
    def wrapper(*args, **kargs):
        import warnings
        warnings.warn(("%s is deprecated since collecting resources from "
                       "widgets at pylons.c.w is no longer needed to inject "
                       "them in the page.") % func.func_name,
                       DeprecationWarning, 2)
        from tw import framework
        if len(args) > 1 and args[0] in framework.engines:
            framework.default_view = args[0]

        global_widgets = getattr(pylons.g, 'w', None)
        request_widgets = getattr(pylons.c, 'w', None)
        other_resources = kargs.pop('resources', None)
        pylons.c.resources = retrieve_resources(
            [global_widgets, request_widgets, other_resources]
            )
        return func(*args, **kargs)

    try:
        wrapper.func_name = func.func_name
    except TypeError:
        # support 2.3
        pass
    tw_extra_doc = """\
This version is a ToscaWidgets wrapper which collects resources in
pylons.g.w and pylons.g.c and makies them available at pylons.c.resources
so the base template can render them.

It also sets the default_view if the engine name is overrided when calling me.
"""
    wrapper.__doc__ = func.__doc__ + "\n\n" + tw_extra_doc
    wrapper.__dict__ = func.__dict__
    return wrapper

render_response = _render_func_wrapper(pylons.templating.render_response)
render = _render_func_wrapper(pylons.templating.render)
