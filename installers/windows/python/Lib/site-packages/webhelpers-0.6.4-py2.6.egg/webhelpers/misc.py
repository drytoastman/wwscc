"""Helpers that are neither text, numeric, container, or date.
"""

import itertools

def all(seq, pred=None):
    """Is ``pred(elm)`` true for all elements?

    With the default predicate, this is the same as Python 2.5's ``all()``
    function; i.e., it returns true if all elements are true.

    >>> all(["A", "B"])
    True
    >>> all(["A", ""])
    False
    >>> all(["", ""])
    False
    >>> all(["A", "B", "C"], lambda x: x <= "C")
    True
    >>> all(["A", "B", "C"], lambda x: x < "C")
    False

    From recipe in itertools docs.
    """
    for elm in itertools.ifilterfalse(pred, seq):
        return False
    return True

def any(seq, pred=None):
    """Is ``pred(elm)`` is true for any element?

    With the default predicate, this is the same as Python 2.5's ``any()``
    function; i.e., it returns true if any element is true.

    >>> any(["A", "B"])
    True
    >>> any(["A", ""])
    True
    >>> any(["", ""])
    False
    >>> any(["A", "B", "C"], lambda x: x <= "C")
    True
    >>> any(["A", "B", "C"], lambda x: x < "C")
    True

    From recipe in itertools docs.
    """
    for elm in itertools.ifilter(pred, seq):
        return True
    return False

def no(seq, pred=None):
    """Is ``pred(elm)`` false for all elements?

    With the default predicate, this returns true if all elements are false.

    >>> no(["A", "B"])
    False
    >>> no(["A", ""])
    False
    >>> no(["", ""])
    True
    >>> no(["A", "B", "C"], lambda x: x <= "C")
    False
    >>> no(["X", "Y", "Z"], lambda x: x <="C")
    True

    From recipe in itertools docs.
    """
    for elm in itertools.ifilter(pred, seq):
        return False
    return True

def count_true(seq, pred=lambda x: x):
    """How many elements is ``pred(elm)`` true for?

    With the default predicate, this counts the number of true elements.

    >>> count_true([1, 2, 0, "A", ""])
    3
    >>> count_true([1, "A", 2], lambda x: isinstance(x, int))
    2

    This is equivalent to the ``itertools.quantify`` recipe, which I couldn't
    get to work.
    """
    ret = 0
    for x in seq:
        if pred(x):
            ret += 1
    return ret

def convert_or_none(value, type_):
    """Return the value converted to the type, or None if error.
    ``type_`` may be a Python type or any function taking one argument.

    >>> print convert_or_none("5", int)
    5
    >>> print convert_or_none("A", int)
    None
    """
    try:
        return type_(value)
    except Exception:
        return None

class DeclarativeException(Exception):
    """A simpler way to define an exception with a fixed message.

    Example:
    >>> class MyException(DeclarativeException):
    ...     message="can't frob the bar when foo is enabled"
    ...
    >>> try:
    ...     raise MyException()
    ... except Exception, e:
    ...      print e
    ...
    can't frob the bar when foo is enabled
    """
    message=""

    def __init__(self, message=None):
        Exception.__init__(self, message or self.message)

if __name__ == "__main__":
    import doctest
    doctest.testmod()
