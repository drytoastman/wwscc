from flask_wtf import FlaskForm
from wtforms import HiddenField, PasswordField, StringField, SubmitField
from wtforms.fields.html5 import EmailField
from wtforms.validators import Length, Email

def addlengthfields(field, kwargs):
    for v in field.validators:
        if isinstance(v, Length):
            if v.min > 0: kwargs.setdefault('minlength', field.validators[0].min)
            if v.max > 0: kwargs.setdefault('maxlength', field.validators[0].max)
            if v.min > 0 and v.max > 0: kwargs.setdefault('required', 1)
    kwargs.setdefault('placeholder', field.name[field.name.find('-')+1:])
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
        kwargs.setdefault('placeholder', self.name[self.name.find('-')+1:])
        return EmailField.__call__(self, **kwargs)


class MyFlaskForm(FlaskForm):

    def html(self, idx, action, method):
        ret = list()
        ret.append("<form id='{}' action='{}' method='{}'>".format(idx, action, method))
        ret.append(str(self.csrf_token))
        for f in self:
            if f.widget.input_type != 'submit':
                ret.append("<div class='row align-items-center'>")
                if f.widget.input_type != 'hidden':
                    ret.append(f.label(class_='col-md-3'))
                ret.append(f(class_='col-md-6'))
                ret.append("</div>")

        ret.append("<div class='row'>")
        ret.append("<div class='col-md-3'></div>")
        ret.append(self.submit(class_="col-md-6 btn btn-primary"))
        ret.append("</div>")
        ret.append("</form>")
        return '\n'.join(ret)

    

def formIntoAttrBase(form, base):
    """ Take form data and put into an AttrBase object """
    for k in base.toplevel:
        if hasattr(form, k):
            setattr(base, k, getattr(form, k).data)
    # leftover fields that aren't in the top level object
    ignore = base.toplevel | set(['csrf_token', 'submit'])
    for k in set(form._fields) - ignore:
        print(k)
        base.attr[k] = getattr(form, k).data

def attrBaseIntoForm(base, form):
    """ Take AttrBase data and place it in form data """
    for k in base.toplevel:
        if hasattr(form, k):
            getattr(form, k).data = getattr(base, k)
    for k in base.attr:
        if hasattr(form, k):
            getattr(form, k).data = base.attr[k]


class ResetPasswordForm(MyFlaskForm):
    username = MyStringField(  'username', [Length(min=6, max=32)])
    password = MyPasswordField('password', [Length(min=6, max=32)])
    submit   = SubmitField(    'Reset')

class LoginForm(MyFlaskForm):
    gotoseries = HiddenField(    'gotoseries')
    username   = MyStringField(  'username', [Length(min=6, max=32)])
    password   = MyPasswordField('password', [Length(min=6, max=32)])
    submit     = SubmitField(    'Login')

class ResetForm(MyFlaskForm):
    firstname = MyStringField('firstname', [Length(min=2, max=32)])
    lastname  = MyStringField('lastname',  [Length(min=2, max=32)])
    email     = MyEmailField( 'email',     [Email()])
    submit    = SubmitField(  'Send Reset Information')

class RegisterForm(MyFlaskForm):
    gotoseries = HiddenField( '  gotoseries')
    firstname = MyStringField('  firstname', [Length(min=2, max=32)])
    lastname  = MyStringField('  lastname',  [Length(min=2, max=32)])
    email     = MyEmailField( '  email',     [Email()])
    username  = MyStringField('  username',  [Length(min=6, max=32)])
    password  = MyPasswordField('password',  [Length(min=6, max=32)])
    submit    = SubmitField(  '  Register')
    
class ProfileForm(MyFlaskForm):
    firstname = MyStringField('First Name', [Length(min=2, max=32)])
    lastname  = MyStringField('Last Name',  [Length(min=2, max=32)])
    email     = MyEmailField( 'Email',     [Email()])
    membership= MyStringField('Membership',[Length(max=64)])
    address   = MyStringField('Address',   [Length(max=64)])
    city      = MyStringField('City   ',   [Length(max=64)])
    state     = MyStringField('State',     [Length(max=16)])
    zip       = MyStringField('Zip',       [Length(max=8)])
    phone     = MyStringField('Phone',     [Length(max=16)])
    brag      = MyStringField('Brag',      [Length(max=64)])
    sponsor   = MyStringField('Sponsor',   [Length(max=64)])
    submit    = SubmitField(  'Update')

