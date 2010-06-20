from datetime import datetime
import logging
import errno

import tw
from tw.api import CSSLink, JSLink, js_function
from tw.forms import FormField, validators


__all__ = ["CalendarDatePicker", "CalendarDateTimePicker", "calendar_js", "calendar_setup"]


setup_calendar = js_function("Calendar.setup")

log = logging.getLogger(__name__)

calendar_css = CSSLink(
    modname='tw.forms', filename='static/calendar/calendar-system.css')
calendar_js = JSLink(
    modname='tw.forms', filename='static/calendar/calendar.js')
calendar_setup = JSLink(
    javascript=[calendar_js],
    modname='tw.forms', filename='static/calendar/calendar-setup.js')

class CalendarDatePicker(FormField):
    """
    Uses a javascript calendar system to allow picking of calendar dates.
    The date_format is in mm/dd/yyyy unless otherwise specified
    """
    css = [calendar_css]
    javascript = [calendar_js, calendar_setup]
    template = "tw.forms.templates.calendar"
    params = [
        "calendar_lang", "not_empty", "button_text", "date_format",
        "picker_shows_time", "tzinfo",
        ]
    calendar_lang = 'en'
    not_empty = True
    button_text = "Choose"
    date_format = "%m/%d/%Y"
    picker_shows_time = False
    validator = None

    _default = None

    def __init__(self, *args, **kw):
        super(CalendarDatePicker, self).__init__(*args, **kw)
        if self.default is None and self.not_empty:
            self.default = lambda: datetime.now()
        self.validator = self.validator or validators.DateTimeConverter(
            format=self.date_format, not_empty=self.not_empty,
            tzinfo=self.tzinfo
            )


    def get_calendar_lang_file_link(self, lang):
        """
        Returns a CalendarLangFileLink containing a list of name
        patterns to try in turn to find the correct calendar locale
        file to use.
        """
        fname = 'static/calendar/lang/calendar-%s.js' % lang.lower()
        return JSLink(modname='tw.forms',
                      filename=fname,
                      javascript=self.javascript)

    def update_params(self, d):
        super(CalendarDatePicker, self).update_params(d)
        log.debug("Value received by Calendar: %r", d.value)
        try:
            d.strdate = d.value.strftime(d.date_format)
        except AttributeError:
            d.strdate = d.value
        options = dict(
            inputField = self.id,
            ifFormat = d.date_format,
            button = self.id + '_trigger',
            showsTime = d.picker_shows_time,
            )
        self.get_calendar_lang_file_link(d.calendar_lang).inject()
        self.add_call(setup_calendar(options))


class CalendarDateTimePicker(CalendarDatePicker):
    """
    Use a javascript calendar system to allow picking of calendar dates and
    time.
    The date_format is in mm/dd/yyyy hh:mm unless otherwise specified
    """
    date_format = "%Y/%m/%d %H:%M"
    picker_shows_time = True
