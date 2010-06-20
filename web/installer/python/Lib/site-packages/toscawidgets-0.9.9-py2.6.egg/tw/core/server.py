from inspect import isclass
from textwrap import dedent

import webob

import tw
from tw.core.resources import registry



def authorize_callback(callback, request):
    widget = callback.im_self
    if widget.callback_authorization is not None:
        return widget.callback_authorization(callback, request)

    return tw.framework.middleware.callback_security_default(callback, request)


def serverside_callback(method):
    method.is_callback = True
    method.authorization = authorize_callback
    return method


resource_prefix = registry.prefix.lstrip("/")



class ServerSideCallbackMixin(object):

    params = dict(callback_authorization = dedent("""
    The WSGI-app that is used to check if
    the current request is authorized to proceed to
    the actual callback.

    The wsgi-app has to have the signature

      (callback, `webob.Request`) -> `webob.Response`

    The callback is passed in to enable a different response
    based on the callback in question.
    """))


    callback_authorization = None


    def post_init(self, *args, **kwargs):
        for name, value in self.__class__.__dict__.iteritems():
            try:
                value.is_callback
            except AttributeError:
                pass
            else:
                registry.register_callback(getattr(self, value.func_name))


    def url_for_callback(self, callback):
        # TODO-droggisch: validate this is really working
        prefix = tw.framework.middleware.prefix
        script_name = tw.framework.script_name
        return "/".join([script_name + prefix, resource_prefix, registry.path_for_callback(callback)])


def always_deny(callback, request):
    response = webob.Response()
    response.status = 403
    return response


def always_allow(callback, request):
    response = webob.Response()
    response.status = 200
    return response
