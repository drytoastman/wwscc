import inspect
from pkg_resources import iter_entry_points

from tw.core.util import pre_post_hooks
from tw.core.exceptions import *


__all__ = [ "WidgetType", "WidgetsList"]

        
class WidgetType(type):
    def __new__(meta,name,bases,dct):
        dct['_cls_children'] = dct.pop('children',_children_from_bases(bases))
        if isinstance(dct.get('params'), dict):
            for param_name, doc in dct['params'].iteritems():
                dct[param_name+'__doc'] = doc
        params = frozenset_from_all_bases(dct,bases,'params')
        frozenset_from_all_bases(dct,bases,'source_vars')
        __init__ = dct.pop('__init__', None)
        if __init__:
            dct['__init__'] = pre_post_hooks(None, 'post_init')(__init__)
        new = type.__new__(meta,name,bases,dct)
        return new

    @classmethod
    def iter_classes(meta):
        """Iterates over all the Widget subclasses in the modules
        that are declared as `toscawidgets.widgets` entrypoints

        >>> from tw.api import Widget
        >>> len(list(Widget.iter_classes())) > 0
        True
        """
        seen = set()
        for ep in iter_entry_points('toscawidgets.widgets'):
            try:
                mod = ep.load(False)
            except ImportError, e:
                continue
            for name in dir(mod):
                obj = getattr(mod, name)
                if isinstance(obj, meta) and obj not in seen:
                    seen.add(obj)
                    yield obj

def _children_from_bases(bases):
    for base in bases:
        if hasattr(base, '_cls_children'):
            return base._cls_children
    return []

def frozenset_from_all_bases(clsdct, bases, name):
    _set = set(clsdct.pop(name,[]))
    [_set.update(getattr(b,name)) for b in bases if hasattr(b, name)]
    fs = clsdct[name] = frozenset(_set)
    return fs

    

class WidgetsListType(type):
    def __new__(meta,name,bases,dct):
        clones = []
        for id, w in dct.items():
            if hasattr(w,'_serial') and hasattr(w, 'clone'):
                dct.pop(id)
                clones.append((w._serial, id, w.clone) )
        clones.sort()
        def __init__(self, clones=clones): 
            # we instantiate the clones on initialization
            widgets = [w[2](id=w[1]) for w in clones]
            self.extend(widgets)
        dct.update({'__slots__':[], '__init__':__init__})
        return type.__new__(meta,name,bases,dct)
        
    def __add__(self, other):
        if isinstance(self, list) and isinstance(other,list):
            return list.__add__(self,other)

        elif isinstance(self, list) and inspect.isclass(other):
            return list.__add__(self,other())

        elif inspect.isclass(self) and isinstance(other,list):
            return list.__add__(self(),other)

        elif inspect.isclass(self) and inspect.isclass(other):
            return list.__add__(self(),other())


    def __radd__(self, other):
        if isinstance(self, list) and isinstance(other,list):
            return list.__add__(other,self)

        elif isinstance(self, list) and inspect.isclass(other):
            return list.__add__(other(),self)

        elif inspect.isclass(self) and isinstance(other,list):
            return list.__add__(other,self())

        elif inspect.isclass(self) and inspect.isclass(other):
            return list.__add__(other(),self())

class WidgetsList(list):
    """
    Syntactic sugar for declaring a list of widgets.

        >>> from tw.api import Widget
        >>> class Widgets(WidgetsList):
        ...     a = Widget()
        ...     b = Widget()
        ...     c = Widget()
        ...
        >>> widgets = Widgets()
        >>> [w.id for w in widgets]
        ['a', 'b', 'c']

    WidgetsLists can be passed to another widget as children by
    the instance or the class.

        >>> w = Widget('foo', children=widgets)
        >>> [c.id for c in w.children]
        ['foo_a', 'foo_b', 'foo_c']

        >>> w = Widget('foo', children=Widgets)
        >>> [c.id for c in w.children]
        ['foo_a', 'foo_b', 'foo_c']

    WidgetsLists subclasses can also be added to reuse common widgets

        >>> class Widgets2(WidgetsList):
        ...     d = Widget()
        ...
        >>> widgets = Widgets + Widgets2
        >>> [w.id for w in widgets]
        ['a', 'b', 'c', 'd']
        >>> widgets = Widgets2 + Widgets
        >>> [w.id for w in widgets]
        ['d', 'a', 'b', 'c']

    """
    __metaclass__ = WidgetsListType


if __name__ == "__main__":
    import doctest
    doctest.testmod()
