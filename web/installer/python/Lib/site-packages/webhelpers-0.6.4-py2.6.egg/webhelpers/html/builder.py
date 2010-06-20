"""HTML/XHTML tag builder

HTML Builder provides an ``HTML`` object that creates (X)HTML tags in a
Pythonic way,  a ``literal`` class used to mark strings containing intentional
HTML markup, and a smart ``escape()`` function that preserves literals but
escapes other strings that may accidentally contain markup characters ("<",
">", "&") or malicious Javascript tags.  Escaped strings are returned as
literals to prevent them from being double-escaped later.

``literal`` is a subclass of ``unicode``, so it works with all string methods
and expressions.  The only thing special about it is the ``.__html__`` method,
which returns the string itself.  ``escape()`` follows a simple protocol: if
the object has an ``.__html__`` method, it calls that rather than ``.__str__``
to get the HTML representation.  Third-party libraries that do not want to
import ``literal`` (and this create a dependency on WebHelpers) can put an
``.__html__`` method in their own classes returning the desired HTML
representation.

When used in a mixed expression containing both literals and ordinary strings,
``literal`` tries hard to escape the strings and return a literal.  However,
this depends on which value has "control" of the expression.  ``literal`` seems
to be able to take control with all combinations of the ``+`` operator, but
with ``%`` and ``join`` it must be on the left side of the expression.  So
these all work::

    "A" + literal("B")
    literal(", ").join(["A", literal("B")])
    literal("%s %s") % (16, literal("kg"))

But these return an ordinary string which is prone to double-escaping later:

    "\n".join([literal('<span class="foo">Foo!</span>'), literal('Bar!')])
    "%s %s" % (literal("16"), literal("&lt;em&gt;kg&lt;/em&gt;"))

Third-party libraries that don't want to import ``literal`` and thus avoid a
dependency on WebHelpers can add an ``.__html__`` method to any class, which
can return the same as ``.__str__`` or something else.  ``escape()`` trusts the
HTML method and does not escape the return value.  So only strings that lack
an ``.__html__`` method will be escaped.

The ``HTML`` object has the following methods for tag building:

``HTML(*strings)``
    Escape the string args, concatenate them, and return a literal.  This is
    the same as ``escape(s)`` but accepts multiple strings.  Multiple args are
    useful when mixing child tags with text, such as::

        html = HTML("The king is a >>", HTML.strong("fink"), "<<!")

``HTML.literal(*strings)``
    Same as ``literal`` but concatenates multiple arguments.

``HTML.comment(*strings)``
    Escape and concatenate the strings, and wrap the result in an HTML 
    comment.

``HTML.tag(tag, *content, **attrs)``
    Create an HTML tag ``tag`` with the keyword args converted to attributes.
    The other positional args become the content for the tag, and are escaped
    and concatenated.  If an attribute name conflicts with a Python keyword
    (notably "class"), append an underscore.  If an attribute value is
    ``None``, the attribute is not inserted.  Two special keyword args are
    recognized:
    
    ``c``
        Specifies the content.  This cannot be combined with content in
        positional args.  The purpose of this argument is to position the
        content at the end of the argument list to match the native HTML
        syntax more closely.  Its use is entirely optional.  The value can
        be a string, a tuple, or a tag.

    ``_closed``
        If present and false, do not close the tag.  Otherwise the tag will be
        closed with a closing tag or an XHTML-style trailing slash as described
        below.

    Example:

    >>> HTML.tag("a", href="http://www.yahoo.com", name=None, 
    ... c="Click Here")
    literal(u'<a href="http://www.yahoo.com">Click Here</a>')


``HTML.__getattr__``
    Same as ``HTML.tag`` but using attribute access.  Example:

    >>> HTML.a("Foo", href="http://example.com/", class_="important")
    literal(u'<a class="important" href="http://example.com/">Foo</a>')

The protocol is simple: if an object has an ``.__html__`` method, ``escape()``
calls it rather than ``.__str__()`` to obtain a string representation.

About XHTML and HTML
--------------------

This builder always produces tags that are valid as *both* HTML and
XHTML.  "Empty" tags (like ``<br>``, ``<input>`` etc) are written like ``<br />``,
with a space and a trailing ``/``.

*Only* empty tags get this treatment.  The library will never, for example,
product ``<script src="..." />``, which is invalid HTML.

The `W3C HTML validator <http://validator.w3.org/>`_ validates these
constructs as valid HTML Strict.  It does produce warnings, but those
warnings warn about the ambiguity if this same XML-style self-closing
tags are used for HTML elements that can take content (``<script>``,
``<textarea>``, etc).  This library never produces markup like that.

Rather than add options to generate different kinds of behavior, we
felt it was better to create markup that could be used in different
contexts without any real problems and without the overhead of passing
options around or maintaining different contexts, where you'd have to
keep track of whether markup is being rendered in an HTML or XHTML
context.

If you _really_ want tags without training slashes (e.g., ``<br>`)`, you can
"abuse" ``_closed=False`` to produce them.

"""
import re
from urllib import quote as url_escape
from UserDict import DictMixin
try:
    set
except NameError:
    from sets import Set as set

from webhelpers.util import cgi_escape

__all__ = ["HTML", "escape", "literal", "url_escape", "lit_sub"]

class UnfinishedTag(object):
    
    """Represents an unfinished or empty tag."""
    
    def __init__(self, tag):
        """Initialize with the tag name."""
        self._tag = tag

    def __call__(self, *args, **kw):
        """Create the tag with the arguments passed in."""
        return make_tag(self._tag, *args, **kw)

    def __str__(self):
        """Return a literal representation."""
        return literal('<%s />' % self._tag)

    def __html__(self):
        """Return the HTML escaped tag."""
        return str(self)


class UnfinishedComment(object):
    
    """Represents an unfinished or empty comment."""
    
    def __call__(self, *args):
        """Create the HTML comment."""
        return literal('<!--%s-->' % ''.join([str(x) for x in args]))
        
    def __html__(self):
        """Return the HTML escaped tag."""
        raise NotImplementedError(
            "You must call html.comment with some text")


class UnfinishedLiteral(object):
    
    """Represent an unfinished literal value."""
    
    def __call__(self, *args):
        """Return the literal HTML."""
        return literal(*args)

    def __html__(self):
        """Return the HTML escaped text."""
        raise NotImplementedError(
            "You must call html.literal with some text")


class HTMLBuilder(object):
    
    """Base HTML object."""
    
    comment = UnfinishedComment()
    literal = UnfinishedLiteral()
    
    def __getattr__(self, attr):
        """Generate the tag for the given attribute name."""
        if attr.startswith('_'):
            raise AttributeError
        result = self.__dict__[attr] = UnfinishedTag(attr.lower())
        return result

    def __call__(self, *args):
        """Join raw HTML and HTML escape it."""
        return literal(''.join([escape(x) for x in args]))

    def tag(self, tag, *args, **kw):
        return make_tag(tag, *args, **kw)


def _attr_decode(v):
    """Parse out attributes that begin with '_'."""
    if v.endswith('_'):
        return v[:-1]
    else:
        return v


def make_tag(tag, *args, **kw):
    if kw.has_key("c"):
        assert not args, "The special 'c' keyword argument cannot be used "\
"in conjunction with non-keyword arguments"
        args = kw.pop("c")
    closed = kw.pop("_closed", True)
    htmlArgs = [' %s="%s"' % (_attr_decode(attr), escape(value))
                for attr, value in sorted(kw.iteritems())
                if value is not None]
    if not args and tag in empty_tags and closed:
        substr = '<%s%s />'
        return literal(substr % (tag, "".join(htmlArgs)))
    else:
        close_tag = ""
        if closed:
            close_tag = "</%s>" %(tag)
        return literal("<%s%s>%s%s" % (
            tag,
            "".join(htmlArgs),
            "".join([escape(x) for x in args]),
            close_tag))


class literal(unicode):
    """Represents an HTML literal.
    
    This subclass of unicode has a ``.__html__()`` method that is 
    detected by the ``escape()`` function.
    
    Also, if you add another string to this string, the other string 
    will be quoted and you will get back another literal object.  Also
    ``literal(...) % obj`` will quote any value(s) from ``obj``.  If
    you do something like ``literal(...) + literal(...)``, neither
    string will be changed because ``escape(literal(...))`` doesn't
    change the original literal.
    
    """
    def __new__(cls, string='', encoding='utf-8', errors="strict"):
        """Create the new literal string object."""
        if isinstance(string, unicode):
            obj = unicode.__new__(cls, string)
        else:
            obj = unicode.__new__(cls, string, encoding, errors)
        obj.encoding = encoding
        obj.error_mode = errors
        return obj

    def __str__(self):
        return self.encode(self.encoding)

    def __repr__(self):
        return '%s(%s)' % (self.__class__.__name__, unicode.__repr__(self))
        
    def __html__(self):
        return self
        
    def __add__(self, other):
        if hasattr(other, '__html__') or isinstance(other, basestring):
            return self.__class__(unicode.__add__(self, escape(other)))
        return NotImplemented
        
    def __radd__(self, other):
        if hasattr(other, '__html__') or isinstance(other, basestring):
            return self.__class__(unicode.__add__(escape(other), self))
        return NotImplemented
    
    def __mul__(self, count):
        return self.__class__(unicode.__mul__(self, count))
    
    def __mod__(self, obj):
        if isinstance(obj, tuple):
            escaped = [_EscapedItem(item, self.encoding,
                                    self.error_mode) for item in obj]
            return self.__class__(unicode.__mod__(self, tuple(escaped)))
        else:
            return self.__class__(unicode.__mod__(self, _EscapedItem(obj, self.encoding,
                                                                     self.error_mode)))
        
    def join(self, items):
        return self.__class__(unicode.join(self, ([escape(i) for i in items])))
    
    def split(self, *args, **kwargs):
        return [literal(x) for x in unicode.split(self, *args, **kwargs)]

    def rsplit(self, *args, **kwargs):
        return [literal(x) for x in unicode.rsplit(self, *args, **kwargs)]
    
    def splitlines(self, *args, **kwargs):
        return [literal(x) for x in unicode.splitlines(self, *args, **kwargs)]


# Yes, this is rather sucky, but I really don't want to write all these
# damn methods, so we write in all the appropriate literal results of these
# functions on module load
for k in dir(literal):
    if k in ['__getslice__', '__getitem__', 'capitalize', 'center', 
             'expandtabs', 'ljust', 'lower', 'lstrip', 'partition',
             'replace', 'rjust', 'rpartition', 'rstrip', 'strip',
             'swapcase', 'title', 'translate', 'upper', 'zfill']:
        def wrapper(func):
            def entangle(*args, **kwargs):
                return literal(func(*args, **kwargs))
            try:
                entangle.__name__ = func.__name__
            except TypeError:
                # < Python 2.4 
                pass
            entangle.__doc__ = func.__doc__
            return entangle
        fun = getattr(unicode, k)
        setattr(literal, k, wrapper(fun))


def lit_sub(*args, **kw):
    """Literal-safe version of re.sub.  If the string to be operated on is
    a literal, return a literal result.
    """
    lit = hasattr(args[2], '__html__')
    cls = args[2].__class__
    result = re.sub(*args, **kw)
    if lit:
        return cls(result)
    else:
        return result


def escape(val, force=False):
    """Does HTML-escaping of a value.
    
    Objects with a ``.__html__()`` method will have that method called,
    and the return value will *not* be quoted.  Thus objects with that
    magic method can be used to represent HTML that should not be
    quoted.
    
    As a special case, ``escape(None)`` returns ''
    
    If ``force`` is true, then it will always be quoted regardless of
    ``__html__()``.
    
    """
    if val is None:
        return literal('')
    elif not force and hasattr(val, '__html__'):
        return literal(val.__html__())
    elif isinstance(val, basestring):
        return literal(cgi_escape(val, True))
    else:
        return literal(cgi_escape(unicode(val), True))

class _EscapedItem(DictMixin):
    
    """Wrapper/helper for literal(...) % obj
    
    This quotes the object during string substitution, and if the
    object is dictionary(-like) it will quote all the values in the
    dictionary.
    
    """
    
    def __init__(self, obj, encoding, error_mode):
        self.obj = obj
        self.encoding = encoding
        self.error_mode = error_mode
        
    def __getitem__(self, key):
        return _EscapedItem(self.obj[key], self.encoding, self.error_mode)
        
    def __str__(self):
        v = escape(self.obj)
        if isinstance(v, unicode):
            v = v.encode(self.encoding)
        return v
        
    def __unicode__(self):
        v = escape(self.obj)
        if isinstance(v, str):
            v = v.decode(self.encoding, self.error_mode)
        return v
    
    def __int__(self):
        return int(self.obj)
    
    def __float__(self):
        return float(self.obj)
    
    def __repr__(self):
        return escape(repr(self.obj))


empty_tags = set("area base basefont br col frame hr img input isindex link meta param".split())

HTML = HTMLBuilder()
