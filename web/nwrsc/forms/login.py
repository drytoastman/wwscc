
from tw import forms
from tw.api import WidgetsList
from tw.forms.validators import String

class LoginForm(forms.TableForm):
	template = "nwrsc.forms.table_form"

	class fields(WidgetsList):
		forward = forms.HiddenField()
		firstname = forms.TextField(label_text='First Name', validator=String(not_empty=True))
		lastname = forms.TextField(label_text='Last Name', validator=String(not_empty=True))
		email = forms.TextField(label_text='Email or Unique Id', validator=String(not_empty=True))

loginForm = LoginForm("loginForm")

