import inspect
import re
import sys
import warnings
import time
import weakref

from itertools import count, izip



__all__ = [
    "OrderedSet",
    "RequestLocalDescriptor",
    "lazystring",
    "asbool",
    "LRUCache",
    "disable_runtime_checks",
    ]

# Stolen from PasteDeploy
def asbool(obj):
    if isinstance(obj, (str, unicode)):
        obj = obj.strip().lower()
        if obj in ['true', 'yes', 'on', 'y', 't', '1']:
            return True
        elif obj in ['false', 'no', 'off', 'n', 'f', '0']:
            return False
        else:
            raise ValueError(
                "String is not true/false: %r" % obj)
    return bool(obj)


def assert_bool_attr(name, state, exc):
    """
    Raises 'exc' if given flag is not equal to 'state'.
    """
    def entangle(func):
        def wrapper(self, *args, **kw):
            if getattr(self,name) == state:
                return func(self, *args, **kw)
            raise exc
        wrapper.func_name = func.func_name
        wrapper.__dict__ = func.__dict__.copy()
        return wrapper
    return entangle




def pre_post_wrapper(pre=None, post=None, lock=None):
    """
    Decorates a method excecuting pre and post methods around it.

    Can be used to decorate the same method in different subclasses and takes
    care that pre is executed only once at the first cooperative call and post
    once at the end of the cooperative call-chain.

        >>> entries = 0
        >>> exits = 0
        >>> def pre(*args, **kw): global entries; entries += 1
        >>> def post(*args, **kw): global exits; exits += 1
        >>> class A(object):
        ...     @pre_post_wrapper(pre, post)
        ...     def say_name(self, name):
        ...         print name
        >>> class B(A):
        ...     @pre_post_wrapper(pre, post)
        ...     def say_name(self, name):
        ...         super(B, self).say_name(name)
        ...         print name
        >>> class C(B):
        ...     @pre_post_wrapper(pre, post)
        ...     def say_name(self, name):
        ...         super(C, self).say_name(name)
        ...         print name
        >>> c = C()
        >>> c.say_name('foo')
        foo
        foo
        foo
        >>> entries
        1
        >>> exits
        1

    A reentrant lock can be passed to syncronize the wrapped method. It is a
    must if the instance is shared among several threads.
    """
    def entangle(func):
        def wrapper(self, *args, **kw):
            counter_name = '__%s_wrapper_counter' % func.func_name
            if lock:
                lock.aquire()
            try:
                counter = getattr(self, counter_name, 0) + 1
                setattr(self, counter_name, counter)
                if counter == 1 and pre:
                    pre(self, *args, **kw)
                output = func(self, *args, **kw)
                counter = getattr(self, counter_name)
                setattr(self, counter_name, counter - 1)
                if counter == 1:
                    delattr(self, counter_name)
                    if post: post(self, *args, **kw)
            finally:
                if lock:
                    lock.release()
            return output
        wrapper.func_name = func.func_name
        wrapper.__dict__ = func.__dict__.copy()
        return wrapper
    return entangle

def pre_post_hooks(pre_name=None, post_name=None, lock=None):
    def entangle(func):
        def wrapper(self, *args, **kw):
            counter_name = '__%s_wrapper_counter' % func.func_name
            bases = list(inspect.getmro(self.__class__))
            if lock:
                lock.aquire()
            try:
                counter = getattr(self, counter_name, 0) + 1
                setattr(self, counter_name, counter)
                if counter == 1 and pre_name:
                    for base in bases:
                        try:
                            # make sure we use the hook  defined in base
                            base.__dict__[pre_name](self, *args, **kw)
                        except KeyError:
                            pass
                output = func(self, *args, **kw)
                counter = getattr(self, counter_name)
                setattr(self, counter_name, counter - 1)
                if counter == 1:
                    delattr(self, counter_name)
                    if post_name:
                        for base in bases:
                            try:
                                base.__dict__[post_name](self, *args, **kw)
                            except KeyError:
                                pass
            finally:
                if lock:
                    lock.release()
            return output
        wrapper.func_name = func.func_name
        wrapper.__dict__ = func.__dict__.copy()
        return wrapper
    return entangle

class CachedInspect(object):
    """
    In general "inspect" calls are extremely expensive.  However, if we cache them
    on an object by object basis, we should be able to speed things up a 
    """
    def __init__(self):
        self.functions = weakref.WeakKeyDictionary()
        self.methods = weakref.WeakKeyDictionary()
        self.argspec = weakref.WeakKeyDictionary()
    
    def is_function(self, obj):
        truth = self.functions.get(obj, None)
        if truth is None:
            truth = self.functions[obj] = inspect.isfunction(obj)
        return truth

    def is_method(self, obj):
        truth = self.methods.get(obj, None)
        if truth is None:
            truth = self.methods[obj] = inspect.ismethod(obj)
        return truth
    
    def getargspec(self, obj):
        spec = self.argspec.get(obj, None)
        if spec is None:
            spec = self.argspec[obj] = inspect.getargspec(obj)
        return spec
callable_cache = CachedInspect()

def callable_wo_args(obj):
    if callable(obj) and (callable_cache.is_function(obj) or callable_cache.is_method(obj)):
        argspec = callable_cache.getargspec(obj)
        args = argspec[0]
        defaults = argspec[3]
        arg_length = callable_cache.is_method(obj) and 1 or 0
        return (
            (args is not None and (len(args) == arg_length)) or
            (defaults is not None and len(args) == len(defaults))
            )
    return False



_id_RE = re.compile(r'(\w+)+(?:-(\d))*')
def unflatten_args(child_args):
    new = {}
    for k,v in child_args.iteritems():
        splitted = k.split('.',1)
        id = splitted[0]
        if len(splitted) == 2:
            rest = splitted[1]
            if not id:
                new[rest] = v
                continue
            for_id = new.setdefault(id, {})
            for_id.setdefault('child_args', {})[rest] = v
        else:
            for_id = new.setdefault(id,{})
            for key, val  in v.iteritems():
                for_id.setdefault(key,val)

    convert = set()
    for k,v in new.items():
        m = _id_RE.match(k)
        if not m:
            raise ValueError(
                "%r is not a valid id to reference a child" % k
                )
        id, n = m.groups()
        if n is not None:
            new.pop(k,None)
            convert.add(id)
            for_id = new.setdefault(id, {})
            for_id[int(n)] = v

    for k in convert:
        for_k = new[k]
        l = []
        for n in count():
            l.append(for_k.pop(n, {}))
            if not for_k: break
        new[k] = {'child_args':l}
    return new




class OrderedSet(list):
    """Set preserving item order."""

    def add(self, item):
        if item not in self:
            self.append(item)

    def add_all(self, iterable):
        for item in iterable:
            self.add(item)


class _RaiseAttributeError: pass

class ThreadSafeDescriptor(object):
    """Base class for threadsafe widget descriptors."""

class RequestLocalDescriptor(ThreadSafeDescriptor):
    """A descriptor that proxies to tw.framework.request_local for
    passing state among widgets in a thread-safe manner for the current request.

    Do not abuse this or code can become increasingly unmantainable!
    """
    def __init__(self, name, default=_RaiseAttributeError, __doc__=None, qualify_with_id=False):
        """Proxy to request local storage attribute named by ``name``.

        If ``default`` is provided no AttributeError will be raised if attribute
        does not exist ar request local storage when getting or deleting.

        If ``qualify_with_id`` is True, the name is extended with the id() of the widget.
        **Not** with the widget.id though!

        This prevents the collapsing of namespaces for several forms on one page
        being rendered after validation.
        """
        self.name = name
        self.default = default
        self.qualify_with_id = qualify_with_id
        self.__doc__ = __doc__ or "'%s' at request-local storage" % name


    def __get__(self, obj, typ=None):
        if obj is None:
            # If called on Widget class return descriptor
            return self
        import tw
        request_local = tw.framework.request_local
        name = self.name
        if self.qualify_with_id:
            name += "_%i" % id(obj)
        try:
            return getattr(request_local, name )
        except AttributeError:
            if self.default is _RaiseAttributeError:
                raise
            else:
                try:
                    value = self.default()
                except TypeError:
                    value = self.default
                setattr(request_local, name, value)
                return value

    def __set__(self, obj, value):
        import tw
        request_local = tw.framework.request_local
        name = self.name
        if self.qualify_with_id:
            name += "_%i" % id(obj)

        setattr(request_local, name, value)

    def __delete__(self, obj):
        import tw
        request_local = tw.framework.request_local
        name = self.name
        if self.qualify_with_id:
            name += "_%i" % id(obj)

        try:
            delattr(request_local, name)
        except AttributeError:
            if self.default is _RaiseAttributeError:
                raise


def install_framework(force=False):
    import tw
    if not hasattr(tw, 'framework') or force:
        from tw.core.registry import StackedObjectProxy, Registry
        from tw.mods import base
        dummy_framework = base.HostFramework()
        # start up a dummy request with dummy environ
        dummy_registry = Registry()
        dummy_registry.prepare()
        dummy_framework.start_request({
            'SCRIPT_NAME': '',
            'toscawidgets.prefix': '/toscawidgets',
            'paste.registry' : dummy_registry,
        })
        tw.framework = StackedObjectProxy(
            dummy_framework,
            name='ToscaWidgetsFramework',
            )

class Enum(dict):
    """Less strict Enum than the one provided by TG"""
    def __init__(self, *args):
        for arg in args:
            self[arg]= arg

    def __getattr__(self, name):
        try:
            return self[name]
        except KeyError:
            raise AttributeError(name)

class Bunch(dict):
    __setattr__ = dict.__setitem__

    def __delattr__(self, name):
        try:
            del self[name]
        except KeyError:
            raise AttributeError(name)

    def __getattr__(self, name):
        try:
            return self[name]
        except KeyError:
            raise AttributeError(name)

def make_bunch(d):
    """Converts a dict instance into a Bunch"""
    return Bunch(d)



# Adapted form turbogears'

class lazystring(object):
    """Has a number of lazily evaluated functions replicating a string.
    """

    def __init__(self, string):
        self.string = string

    def eval(self):
        from tw import framework
        return framework.translator(self.string)

    def __unicode__(self):
        return unicode(self.eval())

    def __str__(self):
        return str(self.eval())

    def __mod__(self, other):
        return self.eval() % other

    def __cmp__(self, other):
        return cmp(self.eval(), other)

    def __eq__(self, other):
        return self.eval() == other

class MultipleReplacer(object):
    """Performs several regexp substitutions on a string with a single pass.

    ``dct`` is a dictionary keyed by a regular expression string and with a
    callable as value that will get called to produce a subsituted value.

    The callable takes the matched text as first argument and may take any
    number of positional and keyword arguments. These arguments are any extra
    args passed when calling the instance.

    For performance, a single regular expression is built.

    Example::

        >>> string = "aaaabbbcc"
        >>> replacer = MultipleReplacer({
        ...     'a+':lambda g, context: g + context['after_a'],
        ...     'b+':lambda g, context: g + context['after_b'],
        ...     'c+':lambda g, context: context['before_c'] + g,
        ... })
        >>> replacer("aaaabbbcc", dict(
        ...     after_a = "1",
        ...     after_b = "2",
        ...     before_c = "3"
        ...     ))
        'aaaa1bbb23cc'
    """
    def __init__(self, dct, options=0):
        self._raw_regexp = r"|".join("(%s)" % k for k in dct.keys())
        self._substitutors = dct.values()
        self._regexp = re.compile(self._raw_regexp, options)

    def __repr__(self):
        return "<%s at %d (%r)>" % (self.__class__.__name__, id(self),
                                    self._raw_regexp)

    def _subsitutor(self, *args, **kw):
        def substitutor(match):
            for substitutor, group in izip(self._substitutors, match.groups()):
                if group is not None:
                    return substitutor(group, *args, **kw)
        return substitutor

    def __call__(self, string, *args, **kw):
        return self._regexp.sub(self._subsitutor(*args, **kw), string)


def iwarn(iterable, message, category=None, stacklevel=1):
    """Make an iterator that run warnings.warn(message, category, stacklevel)
    right before the first value from iterator is returned.
    """
    warnings.warn(message, category, stacklevel)
    for x in iterable:
        yield x

# Stolen from Mako
class LRUCache(dict):
    """A dictionary-like object that stores a limited number of items, discarding
    lesser used items periodically.

    this is a rewrite of LRUCache from Myghty to use a periodic timestamp-based
    paradigm so that synchronization is not really needed.  the size management
    is inexact.
    """

    class _Item(object):
        def __init__(self, key, value):
            self.key = key
            self.value = value
            self.timestamp = time.time()
        def __repr__(self):
            return repr(self.value)

    def __init__(self, capacity, threshold=.5):
        self.capacity = capacity
        self.threshold = threshold

    def __getitem__(self, key):
        item = dict.__getitem__(self, key)
        item.timestamp = time.time()
        return item.value

    def values(self):
        return [i.value for i in dict.values(self)]

    def setdefault(self, key, value):
        if key in self:
            return self[key]
        else:
            self[key] = value
            return value

    def __setitem__(self, key, value):
        item = dict.get(self, key)
        if item is None:
            item = self._Item(key, value)
            dict.__setitem__(self, key, item)
        else:
            item.value = value
        self._manage_size()

    def _manage_size(self):
        while len(self) > self.capacity + self.capacity * self.threshold:
            bytime = dict.values(self)
            bytime.sort(lambda a, b: cmp(b.timestamp, a.timestamp))
            for item in bytime[self.capacity:]:
                try:
                    del self[item.key]
                except KeyError:
                    # if we couldnt find a key, most likely some other thread broke in
                    # on us. loop around and try again
                    break

def disable_runtime_checks():
    """
    Disables runtime checks for possible programming errors regarding
    modifying widget attributes once a widget has been initialized.
    This option can significantly reduce Widget initialization time.
    NOTE: This operation modifies the Widget class and will affect any
        application using ToscaWidgets in the same process.
    """
    from tw.api import Widget
    del Widget.__setattr__
    del Widget.__delattr__

if __name__ == "__main__":
    import doctest
    doctest.testmod()
