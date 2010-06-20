"""Container objects and list/dict helpers.

I would have called this "collections" except that Python 2 can't import a
top-level module that's the same name as a module in the current package.
"""

import sys

try:
    from collections import defaultdict
except ImportError:   # Python < 2.5
    class defaultdict(dict):
        """Backport of Python 2.5's ``defaultdict``.

        From the Python Cookbook.  Written by Jason Kirtland.
        http://aspn.activestate.com/ASPN/Cookbook/Python/Recipe/523034

        """
        def __init__(self, default_factory=None, *a, **kw):
            if (default_factory is not None and
                not hasattr(default_factory, '__call__')):
                raise TypeError('first argument must be callable')
            dict.__init__(self, *a, **kw)
            self.default_factory = default_factory
        def __getitem__(self, key):
            try:
                return dict.__getitem__(self, key)
            except KeyError:
                return self.__missing__(key)
        def __missing__(self, key):
            if self.default_factory is None:
                raise KeyError(key)
            self[key] = value = self.default_factory()
            return value
        def __reduce__(self):
            if self.default_factory is None:
                args = tuple()
            else:
                args = self.default_factory,
            return type(self), args, None, None, self.items()
        def copy(self):
            return self.__copy__()
        def __copy__(self):
            return type(self)(self.default_factory, self)
        def __deepcopy__(self, memo):
            import copy
            return type(self)(self.default_factory,
                              copy.deepcopy(self.items()))
        def __repr__(self):
            return 'defaultdict(%s, %s)' % (self.default_factory,
                                            dict.__repr__(self))

class NotGiven(object):
    """A default value for function args.

    Use this when you need to distinguish between ``None`` and no value.
    
    Example::
    
        >>> def foo(arg=NotGiven):
        ...     print arg is NotGiven
        ...
        >>> foo()
        True
        >>> foo(None)
        False

    """
    pass


class DumbObject(object):
    """A container for arbitrary attributes.

    Usage::
    
        >>> do = DumbObject(a=1, b=2)
        >>> do.b
        2
    
    Alternatives to this class include ``collections.namedtuple`` in Python
    2.6, and ``formencode.declarative.Declarative`` in Ian Bicking's FormEncode
    package.  Both alternatives offer more featues, but ``DumbObject``
    shines in its simplicity and lack of dependencies.

    """
    def __init__(self, **kw):
        self.__dict__.update(kw)


class Counter(object):
    """I count the number of occurrences of each value registered with me.
    
    Usage:
    >>> counter = Counter()
    >>> counter("foo")
    >>> counter("bar")
    >>> counter("foo")
    >>> sorted(counter.result.items())
    [('bar', 1), ('foo', 2)]

    >> counter.result
    {'foo': 2, 'bar': 1}

    To see the most frequently-occurring items in order:

    >>> counter.get_popular(1)
    [(2, 'foo')]
    >>> counter.get_popular()
    [(2, 'foo'), (1, 'bar')]

    Or if you prefer the list in item order:

    >>> counter.get_sorted_items()
    [('bar', 1), ('foo', 2)]
    """

    def __init__(self):
        self.result = defaultdict(int)
        self.total = 0  # Number of times instance has been called.

    def __call__(self, item):
        """Register an item with the counter."""
        self.result[item] += 1
        self.total += 1

    def get_popular(self, max_items=None):
        """Return the results as as a list of (count, item) pairs, with the
        most frequently occurring items first.
        If ``max_items`` is provided, return no more than that many items.
        """
        data = [(x[1], x[0]) for x in self.result.iteritems()]
        data.sort(key=lambda x: (sys.maxint - x[0], x[1]))
        if max_items:
            return data[:max_items]
        else:
            return data

    def get_sorted_items(self):
        """Return the result as a list of (item, count) pairs sorted by item.
        """
        data = self.result.items()
        data.sort()
        return data

    #@classmethod
    def correlate(class_, iterable):
        """Build a Counter from an iterable in one step.

        This is the same as adding each item individually.

        >>> counter = Counter.correlate(["A", "B", "A"])
        >>> counter.result["A"]
        2
        >>> counter.result["B"]
        1
        """
        counter = class_()
        for elm in iterable:
            counter(elm)
        return counter
    correlate = classmethod(correlate)


class Accumulator(object):
    """Accumulate a dict of all values for each key.

    Usage:
    >>> bowling_scores = Accumulator()
    >>> bowling_scores("Fred", 0)
    >>> bowling_scores("Barney", 10)
    >>> bowling_scores("Fred", 1)
    >>> bowling_scores("Barney", 9)
    >>> sorted(bowling_scores.result.items())
    [('Barney', [10, 9]), ('Fred', [0, 1])]

    >> bowling_scores.result
    {'Fred': [0, 1], 'Barney': [10, 9]}

    The values are stored in the order they're registered.

    Alternatives to this class include ``paste.util. multidict.MultiDict``
    in Ian Bicking's Paste package.
    """

    def __init__(self):
        self.result = defaultdict(list)

    def __call__(self, key, value):
        self.result[key].append(value)

    #@classmethod
    def correlate(class_, iterable, key):
        """Correlate several items into an Accumulator in one step.

        ``key`` is a function to calculate the key for each item, akin to
        ``list.sort(key=)``.

        This is the same as adding each item individually.
        """
        accumulator = class_()
        for v in iterable:
            k = key(v)
            accumulator(k, v)
        return accumulator
    correlate = classmethod(correlate)

class UniqueAccumulator(object):
    """Accumulate a dict of unique values for each key.

    The values are stored in an unordered set.
    """

    def __init__(self):
        self.result = defaultdict(set)

    def __call__(self, key, value):
        self.result[key].add(value)


def unique(it):
    """Return a list of unique elements in the iterable, preserving the order.

    Usage:
    >>> unique([None, "spam", 2, "spam", "A", "spam", "spam", "eggs", "spam"])
    [None, 'spam', 2, 'A', 'eggs']
    """
    seen = set()
    ret = []
    for elm in it:
        if elm not in seen:
            ret.append(elm)
            seen.add(elm)
    return ret

def only_some_keys(dic, keys):
    """Return a copy of the dict with only the specified keys present.  
    
    ``dic`` may be any mapping; the return value is always a Python dict.

    >> only_some_keys({"A": 1, "B": 2, "C": 3}, ["A", "C"])
    >>> sorted(only_some_keys({"A": 1, "B": 2, "C": 3}, ["A", "C"]).items())
    [('A', 1), ('C', 3)]
    """
    ret = {}
    for key in keys:
        ret[key] = dic[key]   # Raises KeyError.
    return ret

def except_keys(dic, keys):
    """Return a copy of the dict without the specified keys.

    >>> except_keys({"A": 1, "B": 2, "C": 3}, ["A", "C"])
    {'B': 2}
    """
    ret = dic.copy()
    for key in keys:
        try:
            del ret[key]
        except KeyError:
            pass
    return ret

def extract_keys(dic, keys):
    """Return two copies of the dict.  The first has only the keys specified.
    The second has all the *other* keys from the original dict.

    >> extract_keys({"From": "F", "To": "T", "Received", R"}, ["To", "From"]) 
    ({"From": "F", "To": "T"}, {"Recived": "R"})
    >>> regular, extra = extract_keys({"From": "F", "To": "T", "Received": "R"}, ["To", "From"]) 
    >>> sorted(regular.keys())
    ['From', 'To']
    >>> sorted(extra.keys())
    ['Received']
    """
    for k in keys:
        if k not in dic:
            raise KeyError("key %r is not in original mapping" % k)
    r1 = {}
    r2 = {}
    for k, v in dic.items():
        if k in keys:
            r1[k] = v
        else:
            r2[k] = v
    return r1, r2

def ordered_items(dic, key_order, other_keys=True, default=NotGiven):
    """Like dict.iteritems() but with a specified key order.

    ``dic`` is any mapping.
    ``key_order`` is a list of keys.  Items will be yielded in this order.
    ``other_keys`` is a boolean.
    ``default`` is a value returned if the key is not in the dict.

    This yields the items listed in ``key_order``.  If a key does not exist
    in the dict, yield the default value if specified, otherwise skip the
    missing key.  Afterwards, if ``other_keys`` is true, yield the remaining
    items in an arbitrary order.

    Usage:
    >>> dic = {"To": "you", "From": "me", "Date": "2008/1/4", "Subject": "X"}
    >>> dic["received"] = "..."
    >>> order = ["From", "To", "Subject"]
    >>> list(ordered_items(dic, order, False))
    [('From', 'me'), ('To', 'you'), ('Subject', 'X')]
    """
    d = dict(dic)
    for key in key_order:
        if key in d:
            yield key, d.pop(key)
        elif default is not NotGiven:
            yield key, default
    if other_keys:
        for key, value in d.iteritems():
            yield key, value

def del_quiet(dic, keys):
    """Delete several keys from a dict, ignoring those that don't exist.
    
    This modifies the dict in place.

    >>> d ={"A": 1, "B": 2, "C": 3}
    >>> del_quiet(d, ["A", "C"])
    >>> d
    {'B': 2}
    """
    for key in keys:
        try:
            del dic[key]
        except KeyError:
            pass

def correlate_dicts(dicts, key):
    """Correlate several dicts under one superdict.

    E.g., If you have several dicts each with a 'name' key, this can
    put them in a container dict keyed by name.

    >>> d1 = {"name": "Fred", "age": 41}
    >>> d2 = {"name": "Barney", "age": 31}
    >>> flintstones = correlate_dicts([d1, d2], "name")
    >>> sorted(flintstones.keys())
    ['Barney', 'Fred']
    >>> flintstones["Fred"]["age"]
    41

    If you're having trouble spelling this method correctly, remember:
    "relate" has one 'l'.  The 'r' is doubled because it occurs after a prefix.
    Thus "correlate".
    """
    ret = {}
    i = 0
    for d in dicts:
        try:
            my_key = d[key]
        except KeyError:
            msg = "'dicts' element %d contains no key '%s'"
            tup = i, key 
            raise KeyError(msg % tup)
        ret[my_key] = d
        i += 1
    return ret



def correlate_objects(objects, attr):
    """Correlate several objects under one dict.

    E.g., If you have several objects each with a 'name' attribute, this can
    create a dict containing each object keyed by name.

    >>> class Flintstone(DumbObject):
    ...    pass
    ...
    >>> fred = Flintstone(name="Fred", age=41)
    >>> barney = Flintstone(name="Barney", age=31)
    >>> flintstones = correlate_objects([fred, barney], "name")
    >>> sorted(flintstones.keys())
    ['Barney', 'Fred']
    >>> flintstones["Barney"].age
    31

    If you're having trouble spelling this method correctly, remember:
    "relate" has one 'l'.  The 'r' is doubled because it occurs after a prefix.
    Thus "correlate".
    """
    ret = {}
    i = 0
    for obj in objects:
        try:
            my_key = getattr(obj, attr)
        except AttributeError:
            msg = "'%s' object at 'objects[%d]' contains no attribute '%s'"
            tup = type(obj).__name__, i, attr 
            raise AttributeError(msg % tup)
        ret[my_key] = obj
        i += 1
    return ret


def distribute(lis, columns, direction, fill=None):
    """Distribute a list into a N-column table (list of lists).

    ``lis`` is a list of values to distribute.

    ``columns`` is an int greater than 1, specifying the number of columns in
    the table.

    ``direction`` is a string beginning with "H" (horizontal) or "V"
    (vertical), case insensitive.  This affects how values are distributed in
    the table, as described below.

    ``fill`` is a value that will be placed in any remaining cells if the data
    runs out before the last row or column is completed.  This must be an 
    immutable value such as ``None`` , ``""``, 0, "&nbsp;", etc.  If you
    use a mutable value like ``[]`` and later change any cell containing the
    fill value, all other cells containing the fill value will also be changed.

    The return value is a list of lists, where each sublist represents a row in
    the table.
    ``table[0]`` is the first row.
    ``table[0][0]`` is the first column in the first row.
    ``table[0][1]`` is the second column in the first row.

    This can be displayed in an HTML table via the following Mako template:

    .. code-block:: html+mako

        <table>
        % for row in table:
          <tr>
        % for cell in row:
            <td>${cell}</td>
        % endfor   cell
          </tr>
        % endfor   row
        </table>

    In a horizontal table, each row is filled before going on to the next row.
    This is the same as dividing the list into chunks.

    .. code-block:: pycon
    
        >>> distribute([1, 2, 3, 4, 5, 6, 7, 8], 3, "H")
        [[1, 2, 3], [4, 5, 6], [7, 8, None]]

    In a vertical table, the first element of each sublist is filled before
    going on to the second element.  This is useful for displaying an
    alphabetical list in columns, or when the entire column will be placed in
    a single <td> with a <br /> between each element.

    .. code-block:: pycon

        >>> food = ["apple", "banana", "carrot", "daikon", "egg", "fish", "gelato", "honey"]
        >>> table = distribute(food, 3, "V", "")
        >>> table
        [['apple', 'daikon', 'gelato'], ['banana', 'egg', 'honey'], ['carrot', 'fish', '']]
        >>> for row in table:
        ...    for item in row:
        ...         print "%-9s" % item,
        ...    print "."   # To show where the line ends.
        ...
        apple     daikon    gelato    .
        banana    egg       honey     .
        carrot    fish                .

    Alternatives to this function include a NumPy matrix of objects.

    """
    if columns < 1:
        raise ValueError("arg 'columns' must be >= 1")
    dir = direction[0].upper()
    if dir == "H":   # Horizontal table (row-wise)
        table = []
        for i in range(0, len(lis), columns):
            row = lis[i:i+columns]
            row_len = len(row)
            if row_len < columns:
                extra = [fill] * (columns - row_len)
                row.extend(extra)
            table.append(row)
        return table
    elif dir == "V":  # Vertical table (column-wise)
        total = len(lis)
        rows, remainder = divmod(total, columns)
        if remainder:
            rows += 1
        table = [[fill] * columns for x in range(rows)]
        #print table
        for i, elm in enumerate(lis):
            col, row = divmod(i, rows)
            #print "i=%d, row=%d, col=%d, element=%r" % (i, row, col, elm)
            table[row][col] = elm
        return table
    else:
        raise ValueError("arg ``direction`` must start with 'H' or 'V'")

def transpose(array):
    """Turn a list of lists sideways, making columns into rows and vice-versa.

    The result is undefined if the array is not rectangular; i.e., if
    ``len(array[n]) != len(array[0])``.  You may get an ``IndexError`` or
    missing items.

    Picture the first example as:
       A B C    =>    A D
       D E F          B E
                      C F

    The source array is row-major (``array[n]`` is a row, ``array[n][0]`` is
    the first element of the row), which is good for an HTML table which is
    also row-major (columns within rows).  The result is column-major
    (``array[n]`` is a column, ``array[n][0]`` is the first row in the column),
    which is good for a group of <div> columns with <br /> between rows.

    >>> transpose([["A", "B", "C"], ["D", "E", "F"]])
    [['A', 'D'], ['B', 'E'], ['C', 'F']]
    >>> transpose([["A", "B"], ["C", "D"], ["E", "F"]])
    [['A', 'C', 'E'], ['B', 'D', 'F']]
    >>> transpose([])
    []
    """
    if not array:
        return []
    ret = []
    for c in range(len(array[0])):
        col = [row[c] for row in array]
        ret.append(col)
    return ret
        


if __name__ == "__main__":
    import doctest
    doctest.testmod()
