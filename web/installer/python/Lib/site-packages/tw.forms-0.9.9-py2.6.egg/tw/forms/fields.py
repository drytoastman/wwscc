import math
from warnings import warn
from inspect import isclass
import logging, re
from itertools import chain, count

import formencode
from formencode.foreach import ForEach
from formencode import Invalid

import tw
from tw.api import Widget, lazystring
from tw.core.util import iwarn
from tw.core import view
from tw.forms import (InputWidget, InputWidgetRepeater, validators)

log = logging.getLogger(__name__)

__all__ = [
    "FormField", "FormFieldRepeater", "ContainerMixin", "Form", "FieldSet",
    "TableMixin", "ListMixin", "ListForm", "ListFieldSet", "TextArea",
    "InputField", "TextField", "PasswordField", "HiddenField", "FileField",
    "Button", "SubmitButton", "ResetButton", "ImageButton", "SelectionField",
    "SingleSelectField", "SelectionList", "SingleSelectionMixin",
    "MultipleSelectionMixin", "MultipleSelectField", "RadioButtonList",
    "CheckBoxList", "TableForm", "CheckBox", "RadioButton",
    "SecureTicketField", "SecureFormMixin", "BooleanRadioButtonList",
    "TableFieldSet", "CheckBoxTable", "Spacer", "Label", "LabelHiddenField",
    ]

_ = lazystring

def name2label(name):
    """
    Convert a column name to a Human Readable name.

    Yanked from TGFastData
    """
    # Create label from the name:
    #   1) Convert _ to spaces
    #   2) Convert CamelCase to Camel Case
    #   3) Upcase first character of Each Word
    # Note: I *think* it would be thread-safe to
    #       memoize this thing.
    return ' '.join([s.capitalize() for s in
               re.findall(r'([A-Z][a-z0-9]+|[a-z0-9]+|[A-Z0-9]+)', name)])


class FormField(InputWidget):
    """
    Base class for all Widgets that can be attached to a Form or FieldSet.

    Form and FieldSets are in turn FormFields so they can be arbitrarily nested.
    These widgets can provide a validator that should validate and coerce the
    input they generate when submitted.
    """
    params = [
        "is_required", "label_text", "help_text", "attrs", "show_error",
        "disabled", "style", "container_attrs", "suppress_label",
        ]
    show_error = False
    show_error__doc = ("Should the field display it's own errors? Defaults to "
                       "False because normally they're displayed by the "
                       "container widget")
    disabled = None
    disabled__doc = ("Should the field be disbaled on render and it's input "
                     "ignored by the validator? UNIMPLEMENTED")
    attrs = {}
    attrs__doc = ("Extra attributes for the outermost DOM node")
    help_text = None
    help_text__doc = ("Description of the field to aid the user")
    label_text = None
    label_text__doc = "The text that should label this field"
    style = None
    style__doc = ("Style properties for the field. It's recommended to use "
                  "css classes and stylesheets instead of this parameter")
    container_attrs = {}
    container_attrs__doc = ("Extra attributes to include in the container tag "
                            "around this widget")
    suppress_label = False
    suppress_label__doc = ("Allows individual widgets to suppress the attached "
                           "label in their container")

    available_engines = ['mako', 'genshi']
    engine_name = 'genshi'

    @property
    def is_required(self):
        try:
            self.validate('', use_request_local=False)
            return False
        except (formencode.Invalid, KeyError):
            # Catch KeyError too since now  FieldsMatch raises it when
            # one of the fields it should match is missing. This is probably
            # A FE bug introduced in 1.1...
            return True
    is_required__doc = ("Computed flag indicating if input is required from "
                        "this field")

    file_upload = False

    def __init__(self, id=None, parent=None, children=[], **kw):
        super(FormField, self).__init__(id,parent,children, **kw)
        if self.label_text is None and self.name is not None:
            pos = self.name.rfind('.')
            name = self.name[pos+1:]
            self.label_text = name2label(name)


    def update_params(self,d):
        super(FormField, self).update_params(d)
        if self.is_required:
            d.css_classes.append('required')
        if d.disabled:
            d.attrs['disabled'] = 'disabled'

    def update_attrs(self, d, *args):
        """
        Fetches values from the dict and inserts the in the attrs dict.

        This is useful when you want to avoid boiler-place at the template:

        Instead of::

            <foo bar='$bar' zoo='$zoo' />

        Do::


            <foo py:attrs="attrs" />

        And inside update_params:

        .. code-block:: python

            self.update_attrs(d, 'bar', 'zoo')

        ('bar' and 'zoo' need to be listed at ``params``)

        """
        for name in args:
            d.setdefault('attrs',{}).setdefault(name, d[name])


class ContainerMixin(Widget):
    """
    A mix-in class for FormFields that contain other FormFields

    The following parameters are available:

    `show_children_errors`
      A flag indicating if the container should display it's children's errors

    It provides the template with these iterators:

    `fields`
      A list with all the container's visible FormFields present in `children`

    `hidden_fields`
      A list with all the container's hidden FormFields

    `ifields` (DEPRECATED: use `fields` instead)
      Iterates over all the container's visible FormFields present in `children`

    `ihidden_fields` (DEPRECATED: use `hidden_fields` instead)
      Iterates over all the container's hidden FormFields
    """
    params = ["show_children_errors"]
    show_error = False
    show_children_errors = True

    def __new__(cls, id=None, parent=None, children=[], **kw):
        fields = kw.pop('fields', None)
        if fields is not None:
            children = fields
        else:
            children = getattr(cls, 'fields', children)
        return super(ContainerMixin, cls).__new__(cls, id,parent,children,**kw)

    @property
    def ifields(self):
        return self.ifilter_children(
            lambda x: isinstance(x,FormField) and not isinstance(x,HiddenField)
            )

    @property
    def ihidden_fields(self):
        return self.ifilter_children(lambda x: isinstance(x,HiddenField))

    def _has_file_upload(self):
        for field in self.ifilter_children(
            lambda x: getattr(x, 'file_upload', False)
        ):
            return True
        return False

    def post_init(self, *args, **kw):
        log.debug("Setting 'file_upload' for %r", self)
        self.file_upload = self._has_file_upload()

    def update_params(self,d):
        super(ContainerMixin, self).update_params(d)
        d.fields = list(self.ifields)
        d.hidden_fields = list(self.ihidden_fields)

        d.ifields = iwarn(d.fields,
            "ifields is deprecated, use fields instead", DeprecationWarning, 2)
        d.ihidden_fields = iwarn(d.hidden_fields,
            "ihidden_fields is deprecated, use hidden_fields instead",
            DeprecationWarning, 2)


class FormFieldRepeater(InputWidgetRepeater, ContainerMixin, FormField):
    show_error = True   # Trick containers not to display its errors
    # Override engine stuff inherited from FormField
    available_engines = []
    engine_name = 'toscawidgets'


class Form(ContainerMixin, FormField):
    """
    A base class for all forms.

    Use this class as a base for your custom form. You should override it's
    template because it's a dummy one which does not display errors, help text
    or anything besides it's fields.

    The form will take care of setting its ``enctype`` if it contains any
    FileField
    """
    template = "tw.forms.templates.form"
    params = ["action", "method", "submit_text"]
    action = ''
    action__doc = "The url where the form's contents should be submitted"
    method = 'post'
    method__doc = "The HTTP request method to be used"
    submit_text = "Submit"
    submit_text__doc = ("Text that should appear in the auto-generated Submit "
                        "button. If None then no submit button will be "
                        "autogenerated.")
    submit_label_text = ''
    submit_label__doc = ("Label text for the auto-generated submit button. "
                         "If empty then no label will be generated.")

    def __init__(self, id=None, parent=None, children=[], **kw):
        super(Form, self).__init__(id, parent, children, **kw)
        if not hasattr(self.c, 'submit') and self.submit_text is not None:
            SubmitButton('submit', self, default=self.submit_text,
                         label_text=self.submit_label_text)

    def post_init(self, *args, **kw):
        log.debug("Setting 'enctype' for %r", self)
        if self._has_file_upload():
            self.attrs.setdefault('enctype', 'multipart/form-data')
        self.strip_name = kw.get('strip_name', self.is_root)

    def update_params(self, d):
        super(Form, self).update_params(d)
        d.method = d.method.lower()
        # Fails W3C validation if present
        d.attrs.pop('disabled', None)


class FieldSet(ContainerMixin, FormField):
    """
    Base class for a fieldset.

    Use this class for your custom fieldset. You should override it's template
    because it's a dummy one which does not display errors, help text or
    anything besides it's fields.
    """
    template = "tw.forms.templates.fieldset"
    params = ["legend"]
    legend__doc = ("The legend for the fieldset. If none is provided it will "
                   "use its name")

    def __init__(self, id=None, parent=None, children=[], **kw):
        super(FieldSet, self).__init__(id,parent,children, **kw)
        if self.legend is None:
            self.legend = self.label_text


class TableMixin(object):
    """
    Mix-in class for containers that use a table to render their fields
    """
    params = ["table_attrs", "show_labels", "hover_help"]
    table_attrs = {}
    show_labels = True
    hover_help = False

class ListMixin(object):
    """
    Mix-in class for containers that use a list to render their fields
    """
    params = ["list_attrs", "show_labels", "hover_help"]
    list_attrs = {}
    show_labels = True
    hover_help = False


class ListForm(Form, ListMixin):
    """
    A form that renders it's fields as an unordered list
    """
    template = "tw.forms.templates.list_form"

class TableForm(Form, TableMixin):
    """
    A form that renders it's fields in a table
    """
    template = "tw.forms.templates.table_form"


class ListFieldSet(FieldSet, ListMixin):
    """
    A fieldset that renders it's fields as an unordered list
    """
    template = "tw.forms.templates.list_fieldset"


class TableFieldSet(FieldSet, TableMixin):
    """
    A fieldset that renders it's fields in a table
    """
    template = "tw.forms.templates.table_fieldset"


class TextArea(FormField):
    """
    Displays a textarea.
    """
    params = ["rows", "cols"]
    rows__doc = "Number of rows to render"
    cols__doc = "Number of columns to render"
    template = "tw.forms.templates.textarea"
    rows = 7
    cols = 50

    def update_params(self,d):
        super(TextArea, self).update_params(d)
        self.update_attrs(d, "rows", "cols")


class InputField(FormField):
    """Base class for <input> fields"""
    params = ["type"]
    template = "tw.forms.templates.input_field"


class TextField(InputField):
    """A text field"""
    params = ["size", "max_size", "maxlength"]
    size__doc = "The size of the text field."
    maxlength__doc = "The maximum size of the field"
    max_size__doc = ("The maximum size of the field (DEPRECATED: use maxlength "
                     "instead)")
    type = "text"
    def update_params(self,d):
        super(TextField, self).update_params(d)
        if d.max_size is not None:
            d.maxlength = d.max_size
            warn("max_size is deprecated, use maxlength instead",
                 DeprecationWarning, 6)
        self.update_attrs(d, "size", "maxlength")


class PasswordField(InputField):
    """A password field."""
    type = "password"


class HiddenField(InputField):
    """A hidden field """
    type = "hidden"


class FileField(InputField):
    """A file upload field"""
    type = "file"
    file_upload = True

    def adapt_value(self, value):
        # This is needed because genshi doesn't seem to like displaying
        # cgi.FieldStorage instances
        return None


class Button(InputField):
    """A button field"""
    type = "button"
    params = ["named_button"]
    named_button = False

    def __init__(self, id=None, parent=None, children=[], **kw):
        super(Button, self).__init__(id,parent,children, **kw)
        if not self.named_button:
            self.validator = None

    def _full_name(self):
        if not self.named_button:
            return None
        return super(Button, self)._full_name()
    name = property(_full_name)


class SubmitButton(Button):
    type = "submit"

    def update_params(self,d):
        super(SubmitButton, self).update_params(d)
        # A submit button with and id of 'submit' will make
        # form.submit == 'object' in JS code. See ticket #1295
        if d.id == 'submit':
            d.id = None


class ResetButton(Button):
    type = "reset"


class ImageButton(Button):
    params = ["src", "width", "height", "alt"]
    type = "image"
    def update_params(self,d):
        super(ImageButton, self).update_params(d)
        if isinstance(d.get('src', self.src), tw.api.Link):
            d['src'] = d.get('src', self.src).link
        self.update_attrs(d, "src", "width", "height", "alt")


class CheckBox(InputField):
    type = "checkbox"
    validator = validators.Bool
    def update_params(self, d):
        super(CheckBox, self).update_params(d)
        try:
            checked = self.validator.to_python(d.value)
        except Invalid:
            checked = False
        d.attrs['checked'] = checked or None


class RadioButton(InputField):
    type = "radio"


class SelectionField(FormField):
    selected_verb = None
    params = ["options"]
    options = []

    def update_params(self, d):
        super(SelectionField, self).update_params(d)
        grouped_options = []
        options = []
        d['options'] = self._iterate_options(d['options'])
        # Coerce value if possible so _is_options_selected can compare python
        # values. This is needed when validation fails because FE will send
        # uncoerced values.
        value = self.safe_validate(d['value'])
        for optgroup in d["options"]:
            if isinstance(optgroup[1], (list,tuple)):
                group = True
                optlist = optgroup[1][:]
            else:
                group = False
                optlist = [optgroup]
            for i, option in enumerate(self._iterate_options(optlist)):
                if len(option) is 2:
                    option_attrs = {}
                elif len(option) is 3:
                    option_attrs = dict(option[2])
                if self._is_option_selected(option[0], value):
                    option_attrs[self.selected_verb] = self.selected_verb
                optlist[i] = (self.adjust_value(option[0]), option[1],
                              option_attrs)
            options.extend(optlist)
            if group:
                grouped_options.append((optgroup[0], optlist))
        # options provides a list of *flat* options leaving out any eventual
        # group, useful for backward compatibility and simpler widgets
        d["options"] = options
        if grouped_options:
            d["grouped_options"] = grouped_options
        else:
            d["grouped_options"] = [(None, options)]


    def _iterate_options(self, options):
        for option in options:
            if not isinstance(option, (tuple,list)):
                yield (option, option)
            else:
                yield option


class SelectionList(ListMixin, SelectionField):
    params = ["field_type", "id_counter"]
    selected_verb = "checked"
    template = "tw.forms.templates.selection_list"

    def id_counter(self):
        return count(0)


class SingleSelectionMixin(object):
    def _is_option_selected(self, option_value, value):
        return option_value == value


class MultipleSelectionMixin(object):
    def _is_option_selected(self, option_value, value):
        return value is not None and option_value in value

    def post_init(self, *args, **kw):
        # Only override the user-provided validator if it's not a ForEach one,
        # which usually means the user needs to perform validation on the list
        # as a whole.
        self._original_validator = self.validator
        if not (isinstance(self.validator, ForEach) or
                (isclass(self.validator) and
                 issubclass(self.validator, ForEach))):
            self.validator = ForEach(self.validator)


    def adjust_value(self, value, validator=None):
        # Our ForEach validator will return a single element list. We only
        # want that single element
        return SelectionField.adjust_value(self, value,
                                           self._original_validator)


class SingleSelectField(SingleSelectionMixin, SelectionField):
    selected_verb = 'selected'
    template = "tw.forms.templates.select_field"


class MultipleSelectField(MultipleSelectionMixin, SelectionField):
    params = ["size"]
    size = 5
    selected_verb = 'selected'
    template = "tw.forms.templates.select_field"

    def update_params(self,d):
        super(MultipleSelectField, self).update_params(d)
        self.update_attrs(d, "size")
        d['attrs']['multiple'] = True


class RadioButtonList(SingleSelectionMixin, SelectionList):
    field_type = "radio"


class BooleanRadioButtonList(RadioButtonList):
    options = [(False, _("No")), (True, _("Yes"))]
    validator = validators.StringBoolean


class CheckBoxList(MultipleSelectionMixin, SelectionList):
    field_type = "checkbox"


class SecureTicketField(HiddenField):
    """
    Hidden form field that offers some protection against Cross-Site
    Request Forgery:

        http://en.wikipedia.org/wiki/Cross-site_request_forgery

    This protection is not complete against XSS or web browser bugs, see

        http://www.cgisecurity.com/articles/csrf-faq.shtml

    A per-session per-form authentication key is generated, and
    injected to this hidden field. They are compared on form
    validation. On mismatch, validation error is displayed.
    """

    def __init__(self, id=None, parent=None, children=[],
                 session_secret_cb=None, **kw):
        """Initialize the CSRF form token field.

        session_secret_cb() should return (session_secret, token)
        where session_secret is a random per-session secret string,
        and token some string associated with the current user.
        """
        super(SecureTicketField, self).__init__(id, parent, children, **kw)
        self.validator = validators.SecureTicketValidator(self,
                                                          session_secret_cb)

    def update_params(self, d):
        super(SecureTicketField, self).update_params(d)
        d['value'] = self.validator.get_hash()
        return d


class SecureFormMixin(FormField):
    """
    Protect against Cross-site request forgery, by adding
    SecureTicketField to the form.

    This can be use like this::

        class MyForm(ListForm, SecureFormMixin):
            ...

        def session_secret_cb():
            "Return session-specific secret data string and some data.
             Both of them should stay constant per form per user."
            secret = session['secret']
            user_id = c.user.user_name
            return str(secret), str(user_id)

        form = MyForm(session_secret_cb=session_secret_cb)

    or::

        class MyForm(ListForm, SecureFormMixin):

            ...

            def session_secret_cb():
                ...

        form = MyForm()
    """

    def post_init(self, *args, **kw):
        session_secret_cb = kw.pop('session_secret_cb', None)
        #super(SecureFormMixin, self).__init__(id, parent, children, **kw)
        if hasattr(self, 'session_secret_cb') and session_secret_cb is None:
            session_secret_cb = self.session_secret_cb
        SecureTicketField("form_token__", self,
                          session_secret_cb=session_secret_cb)


def group(seq, size):
    if not hasattr(seq, 'next'):
        seq = iter(seq)
    while True:
        chunk = []
        try:
            for i in xrange(size):
                chunk.append(seq.next())
            yield chunk
        except StopIteration:
            if chunk:
                yield chunk
            break


class CheckBoxTable(CheckBoxList):
    """
    A checkboxlist that renders a table of checkboxes of num_cols columns
    """
    template = "tw.forms.templates.check_box_table"
    params = ["num_cols"]
    num_cols = 1

    def update_params(self, d):
        super(CheckBoxTable, self).update_params(d)
        d.options_rows = group(d.options, d.num_cols)
        d.grouped_options_rows = [(g, group(o, d.num_cols)) for g, o in d.grouped_options]


class Spacer(FormField):
    """
    A widget to insert spacing within a form
    """
    template = "tw.forms.templates.spacer"
    validator = None


class Label(FormField):
    """
    A textual label
    """
    params = ['text']
    text = ''
    template = "tw.forms.templates.label"
    validator = None
    suppress_label = True


class LabelHiddenField(InputField):
    """A hidden field with a label showing its contents"""
    template = "tw.forms.templates.label_hidden"

