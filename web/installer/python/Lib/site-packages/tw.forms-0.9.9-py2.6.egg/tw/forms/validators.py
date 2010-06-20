import re
import warnings
import time
import hmac
from datetime import datetime, date
import weakref

import formencode
from formencode.validators import *
from formencode.foreach import ForEach
from formencode.compound import *
from formencode.api import Invalid, NoDefault
from formencode.schema import Schema

class DateTimeConverter(FancyValidator):

    """
    Converts Python date and datetime objects into string representation and back.
    """
    messages = {
        'badFormat': 'Invalid datetime format',
        'empty': 'Empty values not allowed',
    }
    if_missing = None
    def __init__(self, format = "%Y/%m/%d %H:%M", tzinfo=None, *args, **kwargs):
        super(FancyValidator, self).__init__(*args, **kwargs)
        self.format = format
        self.tzinfo = tzinfo

    def _to_python(self, value, state):
        """ parse a string and return a datetime object. """
        if value and isinstance(value, (date, datetime)):
            return value
        else:
            try:
                tpl = time.strptime(value, self.format)
            except ValueError:
                raise Invalid(self.message('badFormat', state), value, state)
            # shoudn't use time.mktime() because it can give OverflowError,
            # depending on the date (e.g. pre 1970) and underlying C library
            return datetime(year=tpl.tm_year, month=tpl.tm_mon, day=tpl.tm_mday,
                            hour=tpl.tm_hour, minute=tpl.tm_min,
                            second=tpl.tm_sec, tzinfo=self.tzinfo)

    def _from_python(self, value, state):
        if not value:
            return None
        elif isinstance(value, datetime):
            # Python stdlib can only handle dates with year greater than 1900
            if value.year <= 1900:
                return strftime_before1900(value, self.format)
            else:
                return value.strftime(self.format)
        else:
            return value

_illegal_s = re.compile(r"((^|[^%])(%%)*%s)")

def _findall(text, substr):
     # Also finds overlaps
     sites = []
     i = 0
     while 1:
         j = text.find(substr, i)
         if j == -1:
             break
         sites.append(j)
         i = j+1
     return sites

def strftime_before1900(dt, fmt):
    """
    A strftime implementation that supports proleptic Gregorian dates before 1900.

    @see: http://aspn.activestate.com/ASPN/Cookbook/Python/Recipe/306860
    """
    import datetime
    if _illegal_s.search(fmt):
        raise TypeError("This strftime implementation does not handle %s")
    if dt.year > 1900:
        return dt.strftime(fmt)

    year = dt.year
    # For every non-leap year century, advance by
    # 6 years to get into the 28-year repeat cycle
    delta = 2000 - year
    off = 6*(delta // 100 + delta // 400)
    year = year + off

    # Move to around the year 2000
    year = year + ((2000 - year)//28)*28
    timetuple = dt.timetuple()
    s1 = time.strftime(fmt, (year,) + timetuple[1:])
    sites1 = _findall(s1, str(year))

    s2 = time.strftime(fmt, (year+28,) + timetuple[1:])
    sites2 = _findall(s2, str(year+28))

    sites = []
    for site in sites1:
        if site in sites2:
            sites.append(site)

    s = s1
    syear = "%4d" % (dt.year,)
    for site in sites:
        s = s[:site] + syear + s[site+4:]
    return s

class UnicodeString(UnicodeString):
    """The FormEncode UnicodeString validator encodes strings as utf-8 for display. However, this is not desired behaviour in tw.forms, as Genshi will fail when it receives such strings. Instead, this validator renders Python unicode objects where possible, strings otherwise."""
    def _from_python(self, value, state):
        if isinstance(value, basestring):
            return value
        elif hasattr(value, '__unicode__'):
            return unicode(value)
        else:
            return str(value)

class SecureTicketValidator(FancyValidator):
    """This validator helps you avoid cross-site request forgeries (CSRF) - a kind of web application security vulnerability."""
    def __init__(self, widget=None, session_secret_cb=None, **kw):
        # Weakref to container widget to avoid memory leaks
        widget = widget and weakref.proxy(widget) or widget
        self.widget = widget
        self.session_secret_cb = session_secret_cb

    def get_form_info(self):
        widgets = self.widget.root.walk()
        return '/'.join(["%s:%s" % (type(obj).__name__, obj.id) for obj in widgets])

    def get_hash(self):
        """
        Generate a hash that is associated with
            - Current user
            - Current session
            - The form
        """
        secret, stuff = map(str, self.session_secret_cb())
        stuff += self.get_form_info()
        return hmac.new(secret, stuff).hexdigest()

    def _to_python(self, value, state):
        if value != self.get_hash():
            msg = "Form token mismatch! Please try resubmitting the form."
            raise Invalid(msg, value, state)
        return None

__all__ = ["Invalid", "NoDefault"]
for name, value in locals().items():
    if isinstance(value, type) and issubclass(value, Validator):
        __all__.append(name)
