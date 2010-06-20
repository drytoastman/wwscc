import re, weakref, logging
from copy import copy
from warnings import warn
from itertools import ifilter, count, chain, izip, islice, ifilterfalse
from inspect import isclass

import tw
from util import (assert_bool_attr, callable_wo_args,
                  unflatten_args, OrderedSet, make_bunch,
                  install_framework, ThreadSafeDescriptor,
                  RequestLocalDescriptor, LRUCache)

install_framework()

import view
from exceptions import *
from meta import WidgetType, WidgetsList

__all__ = [
    "Widget",
    "WidgetsList",
    "WidgetRepeater",
    "RepeatedWidget",
    "WidgetBunch",
    "RepeatingWidgetBunch",
    "Child",
    "valid_id",
    "adapt_value",
    ]


log = logging.getLogger(__name__)



only_if_initialized = assert_bool_attr(
    '_is_initialized', True, WidgetUninitialized
    )
only_if_uninitialized = assert_bool_attr(
    '_is_initialized', False, WidgetInitialized
    )

def only_if_unlocked(func):
    def __setattr__(self, k, *args):
        descriptor = getattr(self.__class__, k, None)
        if not isinstance(descriptor, ThreadSafeDescriptor) and self._is_locked:
            raise WidgetLocked
        func(self, k, *args)
    return __setattr__

valid_id_re = re.compile(r'^[a-zA-Z][\w\-\_\:\.]*$')
_deprecated_id_re = re.compile(r'^\w+$')



def valid_id(s):
    if valid_id_re.match(s):
        return True
    elif s == '_method':
        #ignore this one small hack to help pages use HTTP verbs
        return True
    elif _deprecated_id_re.match(s):
        #warn("The id %s will no longer be supported since it doesn't conform "
        #     "to the W3C Spec: http://www.w3.org/TR/xhtml1/#C_8" % s,
        #     DeprecationWarning, 3)
        return True
    else:
        return False

serial_generator = count()


class Widget(view.Renderable):
    """
    Base class for all widgets.

    Example:

    .. sourcecode:: python

        >>> w = Widget('foo')
        >>> w.id
        'foo'

        >>> w = Widget('foo', children=[Widget('c1'), Widget('c2')])
        >>> [c.id for c in w.children]
        ['foo_c1', 'foo_c2']
        >>> [c.parent.id for c in w.children]
        ['foo', 'foo']

    It is a **must** that all initial state is entirely determined by the
    arguments to this function. This means that two widgets
    (of the same class) that receive the same parameters
    must behave in exactly the same way. You should not rely on external
    sources inside __init__ to set initial state.

    If you need to fetch data from external sources, do it at
    :meth:`update_params` instead.

    Essential pre, and post initialization is done in :meth:`__new__` and
    :meth:`post_init` respectively. :meth:`post_init` is guaranteed to run after
    the instance finishes initialization and it's behavior is rather special
    as all post_init's in mro will be called to have a chance to set final
    state in the instance.

    Basic pre-initialization consists of binding all kw arguments to the
    widget instance, attaching the widget to it's parent (if given),
    attaching the children and copying mutable arguments listed at
    :attr:`params` from the class to the instance to avoid accidental
    manipulation.

    .. sourcecode:: python

        >>> w = Widget('foo', a=1, b=2)
        >>> w.id
        'foo'
        >>> w.a
        1
        >>> w.b
        2

    Basic post-initialization consists of caching required CSS and JS
    resources and setting the widget as initialized preventing further
    modification of it's attributes.

    .. sourcecode:: python

        >>> w = Widget('foo', a='1', b='2')
        >>> w.a = 'bar'
        Traceback (most recent call last):
            ...
        WidgetLocked: The widget is locked. It's unthread-safe to alter it's attributes after initialization.

    Widget attributes can only be modified in this method because widgets
    should behave in a state-less way as they are shared among threads for
    multiple requests.

    Per request modification of variables sent to the template should be
    done inside :meth:`update_params` and all state flowing from parent to
    children should occur inside that dict.

    Widgets should be instantiated at import time and reused among requests,
    most widgets allow overriding most of their parameters (not neccesarily all
    of them) at display time to change behavior. You should try avoiding
    instantiating widgets for every request as their initialization could be
    quite expensive for heavily nested widgets.

    Request-local storage provided by the hosting framework in
    ``tw.framework.request`` can be used to pass state among widgets
    which don't share the same root.

    """
    __metaclass__ = WidgetType

    parent = None
    default = None
    params = {
        'id': ('The id of this widget. This id is used to reference a widget '
               'from its parent ``children`` attribute and is usually the '
               'DOM id of outermost HTML tag of the widget.'),
        'css_class': 'Main CSS class for this widget',
        'css_classes': 'A list with extra css classes for the widget.'
        }
    css = []
    javascript = []
    css_classes = []

    _is_initialized = False
    _is_locked = False

    def displays_on(self):
        if self.is_root:
            return tw.framework.default_view
        else:
            return self.parent.engine_name
    displays_on = property(displays_on, doc="""\
        Where the widget is being displayed on
        """)

    #XXX: Some of these properties could be implemented as static attributes
    def id(self):
        return '_'.join(reversed(
            [w.id_path_elem for w in self.path if w.id_path_elem]
            )) or None
    id = property(id, doc="""\
        The calculated id of the widget. This string will provide a unique
        id for each widget in the tree in a format which allows to re-recreate
        the nested structure.
        Example::

            >>> A = Widget("A", children=[
            ...     Widget("B", children=[
            ...         Widget("C")
            ...         ])
            ...     ])
            ...
            >>> C = A.c.B.c.C
            >>> C.id
            'A_B_C'

        """)

    def key(self):
        return '.' + '.'.join(reversed(
            [w.id_path_elem for w in self.path if w.id_path_elem][:-1]
            )) or None
    key = property(key, doc="""\
        A string that can be used as a key to index the dictionary of
        parameters sent to the root widget so it reaches this widget when
        displaying.

        Example::

            >>> A = Widget("A", children=[
            ...     Widget("B", children=[
            ...         Widget("C")
            ...         ])
            ...     ])
            ...
            >>> C = A.c.B.c.C
            >>> C.key
            '.B.C'
        """)


    def path(self):
        item = self
        while item:
            yield item
            item = item.parent
    path = property(path, doc="""\
        Iterates a walk from this widget to the root of the tree
        """)


    @property
    def id_path_elem(self):
        return self._id


    def root(self):
        return list(self.path)[-1]
    root = property(root, doc="The root of this widget tree")

    def is_root(self):
        return self.parent is None
    is_root = property(is_root, doc="True if the widget doesn't have a parent")

    def __new__(cls, id=None, parent=None, children=[], **kw):
        """
        Takes care of Widget instances creation.Should not need to be
        overridden likely.
        """
        obj = view.Renderable.__new__(cls, **kw)
        # The previous version of this used the __dict__ attribute to update
        # things, but that doesn't fire any fset properties, and can actually
        # lose data if a property is set, the following will not behave like
        # that
        for k,v in kw.iteritems():
            if not k.startswith('_'):
                try:
                    setattr(obj, k, v)
                except AttributeError, e:
                    #skip setting the value of a read only property
                    pass
        obj.orig_kw = kw.copy()

        if id is not None and not valid_id(id):
            raise ValueError("%r is not a valid id for a Widget"%id)
        obj._id = id
        obj._serial = serial_generator.next()

        # Attach the widget to its parent
        if parent is not None:
            if parent._is_initialized:
                raise WidgetInitialized
            obj.parent = weakref.proxy(parent)
            obj.parent.children.append(obj)

        # Append children passed as args or defined in the class, former
        # override later
        obj.c = obj.children = WidgetBunch()
        if isclass(children) and issubclass(children, WidgetsList):
            children = children()
        if not [obj._append_child(c) for c in children]:
            cls_children = cls._cls_children
            if isclass(cls_children) and issubclass(cls_children, WidgetsList):
                cls_children = cls_children()
            [obj._append_child(c) for c in cls_children]



        # Copy mutable attrs from __class__ into self, if not found in self
        # set to None
        for name in chain(cls.params, ['css', 'javascript']):
            try:
                attr = getattr(obj, name, None)
                if isinstance(attr, (list,dict)):
                    attr = copy(attr)
                setattr(obj, name, attr)
            except AttributeError:
                # In case we try to set a read-only property
                pass

        # Initialize the static js calls list
        obj._js_calls = []

        # Initialize resources OrderedSet
        obj._resources = OrderedSet()

        # Set default css class for the widget
        if not getattr(obj, 'css_class', None):
            obj.css_class = obj.__class__.__name__.lower()

        return obj

    @only_if_initialized
    def clone(self, *args, **kw):
        """
        Returns a cloned version of the widget instance, optionally
        overriding initialization parameters.

        This is the only way to safely "modify" a widget instance.

        Example::

            >>> w = Widget('foo', a=2)
            >>> w.id, w.a
            ('foo', 2)
            >>> w2 = w.clone(a=3)
            >>> w2.id, w2.a
            ('foo', 3)

        """
        #log.debug("Cloning %r", self)
        return Child(
            self.__class__, self._id, children=self.children, **self.orig_kw
            )(*args, **kw)


    @only_if_initialized
    def _as_repeated(self, *args, **kw):
        cls = self.__class__
        new_name = 'Repeated'+cls.__name__
        new_class = type(new_name, (RepeatedWidget, cls), {})
        #log.debug("Generating %r for repeating %r", new_class, self)
        return Child(
            new_class, self._id, children=self.children, **self.orig_kw
            )(*args, **kw)




    def __init__(self, id=None, parent=None, children=[], **kw):
        """Initializes a Widget instance.

        `id`
            The widget's id. All widgets in the same level of nested
            widgets trees should have distinct ids.

        `parent`
            A reference to the widget's parent. This parent needs to be in an
            uninitialized state which means it can only be passed to a child
            inside the parent's __init__ method.

        `children`
            A list, or WidgetsList (instance or class) or any other iterable
            with a reference to the children this widget should have.

        `\*\*kw`
            Any other extra keyword arguments for the widget. All of these will
            get bound to the Widget instance.

        """
        # we need a dummy init here for the metaclass to wrap
        pass



    def _collect_resources(self):
        """picks up resources from self and all children"""
        oset = self._resources
        oset.add_all(chain(*[c._resources for c in self.css]))
        oset.add_all(chain(*[c._resources for c in self.javascript]))
        oset.add_all(chain(*[c._resources for c in self.children]))



    def post_init(self, *args, **kw):
        """
        This method is called for all :class:`tw.api.Widget` base classes to
        perform final setup after the widget is initialized but before it is
        locked.
        """
        if len(self._js_calls) > 0:
            from tw.core.resources import JSFunctionCalls
            #log.debug("Creating JSFunctionCalls for %r. Functions: %s",
                #self, self._js_calls,
                #)
            self.javascript.append(
                JSFunctionCalls(function_calls=self._js_calls)
                )
        self._collect_resources()
        #log.debug("Finished initializing %r", self)
        assert not self._is_initialized
        self._is_initialized = True
        assert not self._is_locked
        self._is_locked = True


    def walk(self, filter=None, recur_if_filtered=True):
        """
        Does a pre-order walk on widget tree rooted at self optionally
        applying a filter on them.

        Example::

            >>> W = Widget
            >>> w = W('a', children=[W('b', children=[W('c')]), W('d')])
            >>> ''.join(i._id for i in w.walk())
            'abcd'
            >>> ''.join(i._id for i in w.walk(lambda x: not x.is_root))
            'bcd'
            >>> ''.join(i._id for i in w.walk(lambda x: x._id == 'c'))
            'c'

            Recursion can be prevented on children that not match filter.

            >>> ''.join(i._id for i in w.walk(lambda x: x._id == 'c', False))
            ''

        """
        def _walk():
            yield self
            iterator = iter(self.children)
            if filter and not recur_if_filtered:
                iterator = self.ifilter_children(filter)
            for c in iterator:
                for w in c.walk(filter, recur_if_filtered):
                    yield w
        if filter:
            return ifilter(filter, _walk())
        return _walk()

    def add_call(self, call, location="bodybottom"):
        """
        Adds a :func:`tw.api.js_function` call that will be made when the
        widget is rendered.
        """
        if self._is_initialized:
            #log.debug("Adding call <%s> for %r dynamically.", call, self)
            from tw.core.resources import dynamic_js_calls
            dynamic_js_calls.inject_call(call, location)
        else:
            #log.debug("Adding call <%s> for %r statically.", call, self)
            self._js_calls.append(str(call))





    @only_if_initialized
    def retrieve_css(self):
        warn("retrieve_css is deprecated. Please use retrieve_resources "
             "instead and filter them yourself", DeprecationWarning, 2)
        return []

    @only_if_initialized
    def retrieve_javascript(self):
        warn("retrieve_javascript is deprecated. Please use retrieve_resources "
             "instead and filter them yourself", DeprecationWarning, 2)
        return []

    @only_if_initialized
    def retrieve_resources(self):
        """
        Returns a dict keyed by location with ordered collections of
        resources from this widget and its children as values.
        """
        from tw.api import locations
        resources = dict((k, OrderedSet()) for k in locations)
        for r in self._resources:
            resources[r.location].add(r)
        return resources


    def adapt_value(self, value):
        """
        Adapt object *value* for rendering in this widget. Should return one of:
         * A list of objects for repeated widgets.
         * A dict for widgets with children, keyed by the children's ids.
         * Any other object the widget understands.
         """
        # Handle MultiDict instances
        if hasattr(value, 'dict_of_lists'):
            for k, v in value.dict_of_lists().items():
                if len(v) == 1:
                    value[k] = v[0]
                else:
                    value[k] = v
        # If we have children, create a dict from the attributes of value
        elif len(self.children_deep) > 0 and not isinstance(value, (dict,list, tuple)):
            value = dict([
                (w._id, getattr(value, w._id))
                    for w in self.children_deep if w._id and hasattr(value, w._id)
                ])
        return value

    def register_resources(self):
        """
        Register the resources required by this Widget with
        :attr:`tw.framework` for inclusion in the page.

        This method is called whenever a :class:`Widget` is rendered
        """
        if self.is_root:
            tw.framework.register_resources(self.retrieve_resources())

    def render(self, value=None, **kw):
        """
        Renders a widget as an unicode string.
        """
        kw = self.prepare_dict(value, kw)
        self.register_resources()
        return super(Widget, self).render(**kw)

    def display(self, value=None, **kw):
        """
        Renders a widget and adapts the output. This method **must** be used
        to display child widgets inside their parent's template so output is
        adapted.

        Unlike :meth:`tw.api.Widget.render`, :meth:`tw.api.Widget.display`
        returns adapted output compatible with the template the widget is being
        rendered on. For example, this is needed so Genshi doesn't autoescape
        string output from mako and to serialize Genshi output on the other way
        around.
        """
        kw = self.prepare_dict(value, kw)
        self.register_resources()
        return super(Widget, self).display(**kw)

    def __call__(self, value=None, **kw):
        return self.display(value, **kw)

    def get_default(self):
        """Returns the default value for the widget. If the default is a funtion
        that it can be called without arguments it will be called on each
        render to retrieve a value"""
        if callable_wo_args(self.default):
            value = self.default()
        else:
            value = self.default
        return value

    @property
    def children_deep(self):
        return self.children

    def prepare_dict(self, value, d, adapt=True):
        """
        Prepares the all kw arguments sent to `display` or `render` before
        passing the kw argument's dict to `update_params`.
        """
        if value is None:
            value = self.get_default()
        if adapt:
            d['value'] = self.adapt_value(value)
        else:
            d['value'] = value

        # Move args passed to child widgets into child_args
        child_args = d.setdefault('child_args', {})
        for k in d.keys():
            if '.' in k:# or '-' in k:
                child_args[k.lstrip('.')] = d.pop(k)

        d['args_for'] = self._get_child_args_getter(child_args)
        d['value_for'] = self._get_child_value_getter(d['value'])
        d['c'] = d['children'] = self.children
        d = make_bunch(d)
        self.update_params(d)
        s = set([d['css_class'],])
        classes = d['css_classes']
        for item in classes:
            s.add(item)
        # Compute the final css_class string
        d['css_class']= ' '.join(s)
        # reset the getters here so update_params has a chance to alter
        # the arguments to children and the value
        d['args_for'] = self._get_child_args_getter(d['child_args'])
        d['value_for'] = self._get_child_value_getter(d.get('value'))
        # Provide a shortcut to display a child field in the template
        d['display_child'] = self._child_displayer(self.children,
                                                   d['value_for'],
                                                   d['args_for'])
        return d


    def update_params(self, d):
        """
        Updates the dict sent to the template for the current request.

        It is called when displaying or rendering a widget with all keyword
        arguments passed stuffed inside dict.

        Widget subclasses can call super cooperatively to avoid
        boiler-plate code as `Widget.update_params` takes care of pre-populating
        this dict with all attributes from self listed at `params`
        (copying them if mutable) and preparing arguments for child widgets.

        Any parameter sent to `display` or `render` will override those fetched
        from the instance or the class.

        Any function listed at `params` which can be called without arguments
        will be automatically called to fetch fresh results on every request.
        Parameters not found either on the class, the instance or the keyword
        args to `display` or `render` will be set to None.

        .. sourcecode:: python

            >>> class MyWidget(Widget):
            ...     params = ["foo", "bar", "null"]
            ...     foo = "foo"
            ...
            >>> w = MyWidget('test', bar=lambda: "bar")
            >>> d = {}
            >>> w.update_params(d)
            >>> d['bar']
            'bar'
            >>> d['foo']
            'foo'
            >>> d['null'] is None
            True
            >>> d = {'foo':'overriden'}
            >>> w.update_params(d)
            >>> d['foo']
            'overriden'

        """
        # Populate dict with attrs from self listed at params
        for k in ifilterfalse(d.__contains__, self.params):
            attr = getattr(self, k, None)
            if attr is not None:
                if isinstance(attr, (list, dict)):
                    attr = copy(attr)
                # Variables that are callable with no args are automatically
                # called here
                elif not isinstance(attr, Widget) and callable_wo_args(attr):
                    log.debug("Autocalling param '%s'", k)
                    attr = attr()
            d[k] = attr


    def ifilter_children(self, filter):
        """
        Returns an iterator for all children applying a filter to them.

        .. sourcecode:: python

            >>> class Widgets(WidgetsList):
            ...     aa = Widget()
            ...     ab = Widget()
            ...     ba = Widget()
            ...     bb = Widget()
            ...
            >>> w = Widget(children=Widgets)
            >>> [c.id for c in w.ifilter_children(lambda w: w.id.startswith('a'))]
            ['aa', 'ab']

        """
        return ifilter(filter, self.children)


    def _get_child_value_getter(self, value):
        def value_getter(child_id):
            if value:
                if (hasattr(child_id, 'repetition') and
                    isinstance(value,list)
                ):
                    child_id = child_id.repetition
                elif isinstance(child_id, Widget) and isinstance(value,dict):
                    child_id = child_id._id
                try:
                    return value[child_id]
                except (IndexError,KeyError,TypeError):
                    None
        return value_getter


    def _get_child_args_getter(self, child_args):
        if isinstance(child_args, dict):
            child_args = unflatten_args(child_args)
        def args_getter(child_id):
            if (hasattr(child_id, 'repetition') and
                isinstance(child_args, list)
            ):
                child_id = child_id.repetition
            elif (isinstance(child_id, Widget) and
                isinstance(child_args, dict)
            ):
                child_id = child_id._id
            try:
                return child_args[child_id]
            except (IndexError,KeyError,TypeError):
                return {}
        return args_getter



    @only_if_uninitialized
    def _append_child(self,obj):
        """Append an object as a child"""
        if isinstance(obj, Widget):
            obj._append_to(self)
        elif isinstance(obj, Child):
            obj(self)
        else:
            raise ValueError("Can only append Widgets or Childs, not %r" % obj)


    @only_if_initialized
    def _append_to(self, parent=None):
        return self.clone(parent)



    @only_if_unlocked
    def __setattr__(self,k,v):
        object.__setattr__(self,k,v)


    @only_if_unlocked
    def __delattr__(self,k):
        object.__delattr__(self,k)


    def __repr__(self):
        name = self.__class__.__name__
        return "%s(%r, children=%r, **%r)" % (
            name, self._id, self.children, self.orig_kw
            )

    def __str__(self):
        return self.render()

    def __ne__(self, other):
        return not (self == other)

    def __eq__(self, other):
        return (
          (getattr(other, '__class__', None) is self.__class__) and
          # Check _id so ancestors are not taken into account
          (other._id == self._id) and
          (other.children == self.children) and (other.orig_kw == self.orig_kw)
          )

    @staticmethod
    def _child_displayer(children, value_for, args_for):
        def display_child(widget, **kw):
            if isinstance(widget, (basestring,int)):
                widget = children[widget]
            child_kw = args_for(widget)
            child_kw.update(kw)
            return widget.display(value_for(widget), **child_kw)
        return display_child

# Available as a public symbol for easier extension with PEAK-Rules for
# backwards compatibility
adapt_value = Widget.adapt_value.im_func




class Child(object):
    """
    Prepares a Widget to being attached to a parent Widget.

    Creates a Widget instance with supplied arguments to the constructor when
    called (optionally overriding default arguments).

        >>> c = Child(Widget, 'foo')
        >>> w = c()
        >>> w.id
        'foo'

    Parameters can be overriden when called.

        >>> w = c(id='bar')
        >>> w.id
        'bar'

    """
    __slots__ = ("widget_class", "id", "children", "kw")

    def __init__(self, widget_class, id=None, children=[], **kw):
        self.widget_class, self.id  = widget_class, id,
        self.children, self.kw = children, kw

    def __call__(self, parent=None, **kw):
        kw_ = self.kw.copy()
        kw_.update(id=self.id, parent=parent, children=self.children)
        kw_.update(kw)
        return self.widget_class(**kw_)





#XXX: Should enhance and clean up so setting widgets is not allowed if the
#     WidgetBunch is used as a widget's 'children' attribute and the widget
#     is already initialized. BTW, this WidgetBunch_attrs thingie is becomming
#     ugly ... should re-implement properly someday...
_WidgetBunch_attrs = frozenset(['_widget_lst', '_widget_dct', '_widget',
                                '_repetitions', '_parent', '_repetition_cache'])
class WidgetBunch(object):
    """
    An ordered collection of widgets.

        >>> from tw.core import Widget
        >>> wb = WidgetBunch(Widget('foo'), Widget('bar'))
        >>> wb[0].id == 'foo'
        True
        >>> wb[1].id == 'bar'
        True

    Exposes a mixed dict/list interface which permits indexing both by widget
    id and position.

        >>> wb['foo'] == wb[0]
        True
        >>> wb['bar'] == wb[1]
        True

    Also permits attribute access as long as the attribute doesn't conflict
    with an internal attribute, in case of conflict the internal attrbute
    will prevail.

        >>> wb.foo.id == 'foo'
        True
        >>> wb.append(Widget('append'))
        >>> wb.append.id
        Traceback (most recent call last):
            ...
        AttributeError: 'function' object has no attribute 'id'
        >>> wb['append'].id == 'append'
        True

    Iteration is also supported

        >>> [w.id for w in wb]
        ['foo', 'bar', 'append']

    Some dict-like iterators too

        >>> [id for id in wb.iterkeys()]
        ['foo', 'bar', 'append']
        >>> [id for id in wb.keys()]
        ['foo', 'bar', 'append']
        >>> [w.id for w in wb.itervalues()]
        ['foo', 'bar', 'append']
        >>> [w.id for w in wb.values()]
        ['foo', 'bar', 'append']

    """

    def __init__(self, *args):
        self._widget_lst = []
        self._widget_dct = {}
        for wid in args:
            self.append(wid)

    def append(self,wid):
        self.__setattr__(wid._id, wid)

    def retrieve_css(self):
        oset = OrderedSet()
        for child in self:
            oset.add_all(child.retrieve_css())
        return oset

    def retrieve_javascript(self):
        oset = OrderedSet()
        for child in self:
            oset.add_all(child.retrieve_javascript())
        return oset

    def retrieve_resources(self):
        from tw.core.resources import merge_resources, locations
        resources = dict((k, OrderedSet()) for k in locations)
        for w in self:
            merge_resources(resources, w.retrieve_resources())
        return resources

    def __getitem__(self, item):
        if isinstance(item, basestring):
            return self._widget_dct[item]
        elif isinstance(item, int):
            return self._widget_lst[item]
        raise KeyError, "No widget by %r" % item

    def __getattr__(self, name):
        if name in _WidgetBunch_attrs:
            return object.__getattribute__(self,name)
        try:
            return self[name]
        except KeyError:
            raise AttributeError, "No widget by %r" % name

    def __setattr__(self, name, value):
        # Note that this setattr allows name = None so append can use it.
        if name in _WidgetBunch_attrs:
            return object.__setattr__(self,name,value)
        self._widget_lst.append(value)
        self._widget_dct[name] = value

    def __iter__(self):
        for wid in self._widget_lst:
            yield wid

    def __ne__(self, other):
        return not (self==other)

    def __eq__(self, other):
        try:
            if len(self) == len(other):
                for a,b in izip(other,self):
                    if a!=b: return False
                return True
        except TypeError: pass
        return False

    def __getslice__(self,start=0,stop=-1,step=1):
        return islice(self,start,stop,step)

    def __len__(self):
        return len(self._widget_lst)

    def __nonzero__(self):
        return bool(len(self))

    def __contains__(self,item):
        dct = self._widget_dct
        try:
            return dct[item._id] == item
        except KeyError:
            return False

    def keys(self):
        return list(self.iterkeys())

    def iterkeys(self):
        for w in self: yield w._id

    def values(self):
        return list(self.itervalues())

    itervalues = __iter__

    def __repr__(self):
        return `self._widget_lst`


class RepeatingWidgetBunch(WidgetBunch):
    _repetitions = 0
    _widget = None

    def __init__(self, *args, **kw):
        super(RepeatingWidgetBunch, self).__init__(*args, **kw)
        self._repetition_cache = LRUCache(100)

    def __len__(self):
        return self._repetitions

    def __iter__(self, reps=None):
        if reps is None:
            reps = self._repetitions
        for i in xrange(reps):
            yield self[i]

    def __getitem__(self, item):
        if not isinstance(item, int):
            raise KeyError("Must specify an integer")
        try:
            rep = self._repetition_cache[item]
        except KeyError:
            rep = self._widget.clone(repetition=item)
            # Work around widget's locking mechanism since it is thread-safe
            # to do it *here*. Note that the parent is not directly aware that
            # it has this repeated widget as a child and it doesn't care
            # really since I create those children on the fly
            object.__setattr__(rep, 'parent', self._parent)
            self._repetition_cache[item] = rep
        return rep


class WidgetRepeater(Widget):
    params = ["repetitions", "extra"]
    widget = None
    repetitions = 3
    max_repetitions = None
    extra = 0
    id_path_elem = None

    template = "$output"

    @property
    def key(self):
        raise AttributeError("A WidgetRepeater has no meaningful key")


    def __new__(cls, id=None, parent=None, children=[], **kw):
        widget = kw.get('widget', getattr(cls, 'widget', None))
        if widget is None:
            warn("No repeated widget specified in %s" % cls.__name__)
            widget = Widget()
        if isclass(widget) and issubclass(widget, Widget):
            widget = widget()
        obj = super(WidgetRepeater, cls).__new__(cls, id,parent,**kw)
        obj.c = obj.children = RepeatingWidgetBunch()
        obj.c._parent = weakref.proxy(obj)
        obj.c._repetitions = kw.get('repetitions', cls.repetitions)
        obj.c._widget = widget._as_repeated(obj, id=obj._id)
        return obj


    def update_params(self, d):
        super(WidgetRepeater,self).update_params(d)
        if d.get('max_repetitions', self.max_repetitions) is not None:
            warn("max_repetitions is deperecated and no longer has any effect",
                 DeprecationWarning, 3)
        v_f = d['value_for']
        a_f = d['args_for']
        outputs = [
            w.render(v_f(w), isextra=(w.repetition >= len(d['value'])), **a_f(w)) for w in d['children'].__iter__(
                max(d['repetitions'], len(d['value']) + d.extra))
            ]
        d["output"] = '\n'.join(o for o in outputs if o)


class RepeatedWidget(Widget):
    params = ["repetition"]
    repetition = 0

    @property
    def id_path_elem(self):
        return '%s-%d' % (self._id, self.repetition or 0)

    @only_if_initialized
    def _as_repeated(self, *args, **kw):
        return self.clone(*args, **kw)


if __name__ == "__main__":
    import doctest
    doctest.testmod(optionflags = doctest.ELLIPSIS|doctest.NORMALIZE_WHITESPACE)
