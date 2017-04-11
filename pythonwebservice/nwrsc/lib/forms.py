import logging

from flask import request
from flask_wtf import FlaskForm
from wtforms import BooleanField, HiddenField, PasswordField, SelectField, StringField, SubmitField
from wtforms.fields.html5 import EmailField, IntegerField
from wtforms.validators import Length, Email, Required

log = logging.getLogger(__name__)

def addlengthfields(field, kwargs):
    for v in field.validators:
        if isinstance(v, Length):
            if v.min > 0: kwargs.setdefault('minlength', field.validators[0].min)
            if v.max > 0: kwargs.setdefault('maxlength', field.validators[0].max)
            if v.min > 0 and v.max > 0: kwargs.setdefault('required', 1)
    return kwargs

class MyStringField(StringField):
    def __call__(self, **kwargs):
        return StringField.__call__(self, **addlengthfields(self, kwargs))

class MyPasswordField(PasswordField):
    def __call__(self, **kwargs):
        return PasswordField.__call__(self, **addlengthfields(self, kwargs))

class MyEmailField(EmailField):
    def __call__(self, **kwargs):
        kwargs.setdefault('required', 1)
        return EmailField.__call__(self, **kwargs)

class MyFlaskForm(FlaskForm):

    def html(self, idx, action, method, labelcol='col-md-3', inputcol='col-md-9'):
        ret = list()
        ret.append("<form id='{}' action='{}' method='{}'>".format(idx, action, method))
        for f in self:
            if not hasattr(f.widget, 'input_type') or f.widget.input_type != 'submit':
                ret.append("<div class='row'>")
                if not hasattr(f.widget, 'input_type') or f.widget.input_type != 'hidden':
                    ret.append(f.label(class_=labelcol))
                ret.append(f(class_=inputcol))
                ret.append("</div>")

        ret.append("<div class='row'>")
        ret.append("<input type='text' name='message' />")
        ret.append("</div>")

        ret.append("<div class='row'>")
        ret.append("<div class='{}'></div>".format(labelcol))
        ret.append(self.submit(class_="{} btn btn-primary".format(inputcol)))
        ret.append("</div>")
        ret.append("</form>")
        return '\n'.join(ret)

    def validate(self):
        if request.form.get('message'): # super simple bot test (advanced bots will get by this)
            log.warning("Suspect form submission from (put IP here), ignoring")
            abort(404)
        return FlaskForm.validate(self)


def formIntoAttrBase(form, base):
    """ Take form data and put into an AttrBase object """
    for k in base.toplevel:
        if hasattr(form, k):
            setattr(base, k, getattr(form, k).data)
    # leftover fields that aren't in the top level object
    ignore = set(base.toplevel + ['csrf_token', 'submit'])
    for k in set(form._fields) - ignore:
        base.attr[k] = getattr(form, k).data

def attrBaseIntoForm(base, form):
    """ Take AttrBase data and place it in form data """
    for k in base.toplevel:
        if hasattr(form, k):
            getattr(form, k).data = getattr(base, k)
    for k in base.attr:
        if hasattr(form, k):
            getattr(form, k).data = base.attr[k]


"""
class ResetPasswordForm(MyFlaskForm):
    username = MyStringField(  'Username', [Length(min=6, max=32)])
    password = MyPasswordField('Password', [Length(min=6, max=32)])
    submit   = SubmitField(    'Reset')
"""

class PasswordForm(MyFlaskForm):
    gotoseries = HiddenField(    'gotoseries')
    username   = MyStringField(  'Username', [Length(min=6, max=32)])
    password   = MyPasswordField('Password', [Length(min=6, max=32)])
    submit     = SubmitField(    'Login')

class ResetForm(MyFlaskForm):
    firstname = MyStringField('First Name', [Length(min=2, max=32)])
    lastname  = MyStringField('Last Name',  [Length(min=2, max=32)])
    email     = MyEmailField( 'Email',     [Email()])
    submit    = SubmitField(  'Send Reset Information')

class RegisterForm(MyFlaskForm):
    gotoseries = HiddenField( '  gotoseries')
    firstname = MyStringField('  First Name', [Length(min=2, max=32)])
    lastname  = MyStringField('  Last Name',  [Length(min=2, max=32)])
    email     = MyEmailField( '  Email',     [Email()])
    username  = MyStringField('  Username',  [Length(min=6, max=32)])
    password  = MyPasswordField('Password',  [Length(min=6, max=32)])
    submit    = SubmitField(  '  Register')
    
class ProfileForm(MyFlaskForm):
    firstname = MyStringField('First Name', [Length(min=2, max=32)])
    lastname  = MyStringField('Last Name',  [Length(min=2, max=32)])
    email     = MyEmailField( 'Email',      [Email()])
    membership= MyStringField('Barcode',    [Length(max=16)])
    address   = MyStringField('Address',    [Length(max=64)])
    city      = MyStringField('City   ',    [Length(max=64)])
    state     = MyStringField('State',      [Length(max=16)])
    zip       = MyStringField('Zip',        [Length(max=8)])
    phone     = MyStringField('Phone',      [Length(max=16)])
    brag      = MyStringField('Brag',       [Length(max=64)])
    sponsor   = MyStringField('Sponsor',    [Length(max=64)])
    submit    = SubmitField(  'Update')

class CarForm(MyFlaskForm):
    driverid    = HiddenField(  'driverid')
    carid       = HiddenField(  'carid')
    year        = MyStringField('Year',  [Length(max=8)])
    make        = MyStringField('Make',  [Length(max=16)])
    model       = MyStringField('Model', [Length(max=16)])
    color       = MyStringField('Color', [Length(max=16)])
    classcode   = SelectField(  'Class', [Required()])
    indexcode   = SelectField(  'Index')
    useclsmult  = BooleanField( 'MultFlag')
    number      = IntegerField( 'Number', [Required()])
    submit      = SubmitField(  'Update')

