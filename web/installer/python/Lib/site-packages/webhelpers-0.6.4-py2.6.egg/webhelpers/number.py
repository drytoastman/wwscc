"""Number formatting and numeric helpers"""

import re

def percent_of(part, whole):
    """What percent of ``whole`` is ``part``?

    >>> percent_of(5, 100)
    5.0
    >>> percent_of(13, 26)
    50.0
    """
    # Use float to force true division.
    return float(part * 100) / whole

def mean(r):
    """Return the mean of a sequence of numbers.
    
    The mean is the average of all the numbers.

    >>> mean([5, 10])
    7.5
    """
    try:
        return float(sum(r)) / len(r)
    except ZeroDivisionError:
        raise ValueError("can't calculate mean of empty collection")

average = mean

def median(r):
    """Return the median of an iterable of numbers.

    The median is the point at which half the numbers are lower than it and
    half the numbers are higher.  This gives a better sense of the majority
    level than the mean (average) does, because the mean can be skewed by a few
    extreme numbers at either end.  For instance, say you want to calculate
    the typical household income in a community and you've sampled four
    households:

    >>> incomes = [18000]       # Fast food crew
    >>> incomes.append(24000)   # Janitor
    >>> incomes.append(32000)   # Journeyman
    >>> incomes.append(44000)   # Experienced journeyman
    >>> incomes.append(67000)   # Manager
    >>> incomes.append(9999999) # Bill Gates
    >>> median(incomes)
    49500.0
    >>> mean(incomes)
    1697499.8333333333

    The median here is somewhat close to the majority of incomes, while the
    mean is far from anybody's income.

        [20 000,
        40 000,
        60 000,
        9 999 999]
    
    The median would be around 50 000, which is close to what the majority of
    respondents make.  The average would be in the millions, which is far from
    what any of the respondents make.
    
    This implementation makes a temporary list of all numbers in memory.
    """
    s = list(r)
    s_len = len(s)
    if s_len == 0:
        raise ValueError("can't calculate mean of empty collection")
    s.sort()
    center = s_len // 2
    is_odd = s_len % 2
    if is_odd:
        return s[center]   # Return the center element.
    # Return the average of the two elements nearest the center.
    low = s[center-1]
    high = s[center+1]
    return mean([low, high])

def standard_deviation(r, sample=True):
    """Standard deviation, `from the Python Cookbook
    <http://aspn.activestate.com/ASPN/Cookbook/Python/Recipe/442412>`_
    Population mode contributed by Lorenzo Catucci.

    Standard deviation shows the variability within a sequence of numbers.
    A small standard deviation shows the numbers are close to the same.  A
    large standard deviation shows they are widely different.  In fact it
    shows how far the numbers tend to deviate from the average.  This can be
    used to detect whether the average has been skewed by a few extremely high
    or extremely low values.

    By default the helper computes the unbiased estimate
    for the population standard deviation, by applying an unbiasing
    factor of sqrt(N/(N-1)).

    If you'd rather have the function compute the population standard
    deviation, pass ``sample=False``.

    The following examples are taken from Wikipedia.
    http://en.wikipedia.org/wiki/Standard_deviation

        >>> standard_deviation([0, 0, 14, 14]) # doctest: +ELLIPSIS
        8.082903768654761...
        >>> standard_deviation([0, 6, 8, 14]) # doctest: +ELLIPSIS
        5.773502691896258...
        >>> standard_deviation([6, 6, 8, 8])
        1.1547005383792515
        >>> standard_deviation([0, 0, 14, 14], sample=False)
        7.0
        >>> standard_deviation([0, 6, 8, 14], sample=False)
        5.0
        >>> standard_deviation([6, 6, 8, 8], sample=False)
        1.0

    (The results reported in Wikipedia are those expected for whole
    population statistics and therefore are equal to the ones we get
    by setting ``sample=False`` in the later tests.)
    
    .. code-block:: pycon
    
        # Fictitious average monthly temperatures in Southern California.
        #                       Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec
        >>> standard_deviation([70, 70, 70, 75, 80, 85, 90, 95, 90, 80, 75, 70]) # doctest: +ELLIPSIS
        9.003366373785...
        >>> standard_deviation([70, 70, 70, 75, 80, 85, 90, 95, 90, 80, 75, 70], sample=False) # doctest: +ELLIPSIS
        8.620067027323...

        # Fictitious average monthly temperatures in Montana.
        #                       Jan  Feb  Mar Apr May Jun Jul  Aug Sep Oct Nov Dec
        >>> standard_deviation([-32, -10, 20, 30, 60, 90, 100, 80, 60, 30, 10, -32]) # doctest: +ELLIPSIS
        45.1378360405574...
        >>> standard_deviation([-32, -10, 20, 30, 60, 90, 100, 80, 60, 30, 10, -32], sample=False) # doctest: +ELLIPSIS
        43.2161878106906...

    Most natural and random phenomena follow the normal distribution (aka the
    bell curve), which says that most values are close to average but a few are
    extreme.  E.g., most people are close to 5'9" tall but a few are very tall
    or very short.  If the data does follow the bell curve, 68% of the values
    will be within 1 standard deviation (stdev) of the average, and 95% will be
    within 2 standard deviations.  So a university professor grading exams on a
    curve might give a "C" (mediocre) grade to students within 1 stdev of the
    average score, "B" (better than average) to those within 2 stdevs above,
    and "A" (perfect) to the 0.25% higher than 2 stdevs.  Those between 1 and 2
    stdevs below get a "D" (poor), and those below 2 stdevs... we won't talk
    about them.
    """
    avg = average(r)
    sdsq = sum([(i - avg) ** 2 for i in r])
    if sample:
        normal_denom=len(r) - 1 or 1
    else:
        normal_denom=len(r)
    return (sdsq / normal_denom) ** 0.5

class SimpleStats(object):
    """Calculate a few simple stats on data.
    
    This class calculates the minimum, maximum, and count of all the values
    given to it.  The values are not saved in the object.  Usage::

        >>> stats = SimpleStats()
        >>> stats(2)               # Add one data value.
        >>> stats.extend([6, 4])   # Add several data values at once.  

    The statistics are available as instance attributes::

        >>> stats.count
        3
        >>> stats.min
        2
        >>> stats.max
        6

    Non-numeric data is also allowed:

    >>> stats2 = SimpleStats()
    >>> stats2("foo")
    >>> stats2("bar")
    >>> stats2.count
    2
    >>> stats2.min
    'bar'
    >>> stats2.max
    'foo'

    If the ``numeric`` constructor arg is true, only ``int``, ``long``, and 
    ``float`` values will be accepted.  This flag is intended to enable
    additional numeric statistics, although none are currently implemented.

    ``.min`` and ``.max`` are ``None`` until the first data value is
    registered.

    Subclasses can override ``._init_stats`` and ``._update_stats`` to add
    additional statistics.
    """
    __version__ = 1

    def __init__(self, numeric=False):
        self.numeric = numeric
        self.count = 0
        self.min = None
        self.max = None
        self._init_stats()
        
    def __nonzero__(self):
        """The instance is true if it has seen any data."""
        return bool(self.count)

    def __call__(self, value):
        """Add a data value."""
        if self.numeric:
            value + 0   # Raises TypeError if value is not numeric.
        if self.count == 0:
            self.min = self.max = value
        else:
            self.min = min(self.min, value)
            self.max = max(self.max, value)
        self.count += 1
        self._update_stats(value)

    def extend(self, values):
        """Add several data values at once, akin to ``list.extend``."""
        for value in values:
            self(value)

    ### Hooks for subclasses
    def _init_stats(self):
        """Initialize state data used by subclass statistics."""
        pass

    def _update_stats(self, value):
        """Add a value to the subclass statistics."""
        pass


class Stats(SimpleStats):
    """A container for data and statistics.

    This class extends ``SimpleStats`` by calculating additional statistics,
    and by storing all data seen.  All values must be numeric (``int``,
    ``long``, and/or ``float``), and you must call ``.finish()`` to generate
    the additional statistics.  That's because the statistics here cannot be
    calculated incrementally, but only after all data is known.

    
    >>> stats = Stats()
    >>> stats.extend([5, 10, 10])
    >>> stats.count
    3
    >>> stats.finish()
    >>> stats.mean # doctest: +ELLIPSIS
    8.33333333333333...
    >>> stats.median
    10
    >>> stats.standard_deviation
    2.8867513459481287

    All data is stored in a list and a set for later use::

        >>> stats.list
        [5, 10, 10]

        >>  stats.set
        set([5, 10])

    (The double prompt ">>" is used to hide the example from doctest.)

    The stat attributes are ``None`` until you call ``.finish()``.  It's
    permissible -- though not recommended -- to add data after calling
    ``.finish()`` and then call ``.finish()`` again.  This recalculates the
    stats over the entire data set.

    The ``SimpleStats`` hook methods are available for subclasses, and 
    additionally the ``._finish_stats`` method.
    """
    __version__ = 1

    def __init__(self):
        SimpleStats.__init__(self, numeric=True)
        self.list = []
        self.set = set()
        self.mean = None
        self.median = None
        self.standard_deviation = None
        self._init_stats()

    def __call__(self, value):
        if self.count == 0:
            self.min = self.max = value
        else:
            self.min = min(self.min, value)
            self.max = max(self.max, value)
        self.count += 1
        self._update_stats(value)
        self.list.append(value)
        self.set.add(value)

    def finish(self):
        self.mean = mean(self.list)
        self.median = median(self.list)
        self.standard_deviation = standard_deviation(self.list)
        self._finish_stats()

    ### Hooks for subclasses.
    def _finish_stats(self):
        """Finish the subclass statistics now that all data are known."""
        pass


def format_number(n, thousands=",", decimal="."):
    """Format a number with a thousands separator and decimal delimeter.

    ``n`` may be an int, long, float, or numeric string.
    ``thousands`` is a separator to put after each thousand.
    ``decimal`` is the delimiter to put before the fractional portion if any.

    The default style has a thousands comma and decimal point per American
    usage:

    >>> format_number(1234567.89)
    '1,234,567.89'
    >>> format_number(123456)
    '123,456'
    >>> format_number(-123)
    '-123'

    Various European and international styles are also possible:

    >>> format_number(1234567.89, " ")
    '1 234 567.89'
    >>> format_number(1234567.89, " ", ",")
    '1 234 567,89'
    >>> format_number(1234567.89, ".", ",")
    '1.234.567,89'
    """
    parts = str(n).split(".")
    parts[0] = re.sub(
        R"(\d)(?=(\d\d\d)+(?!\d))", 
        R"\1%s" % thousands, 
        parts[0])
    return decimal.join(parts)

if __name__ == "__main__":
    import doctest
    doctest.testmod()
