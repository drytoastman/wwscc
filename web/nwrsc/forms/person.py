
from tw import forms
from tw.forms.validators import String
from tw.api import WidgetsList

class validateName(WidgetsList):
	firstname = forms.TextField(validator=String(not_empty=True), label_text='First Name')
	lastname = forms.TextField(validator=String(not_empty=True), label_text='Last Name')
	email = forms.TextField(validator=String(not_empty=True), label_text='Email or UniqueId')

class dontValidateName(WidgetsList):
	firstname = forms.TextField(label_text='First Name')
	lastname = forms.TextField(label_text='Last Name')
	email = forms.TextField(label_text='Email or UniqueId')

class commonFields(WidgetsList):
	membership = forms.TextField(label_text='Membership')
	space1 = forms.Spacer(label_text='')
	address = forms.TextField(label_text='Address')
	city = forms.TextField(label_text='City')
	state = forms.TextField(label_text='State')
	zip = forms.TextField(label_text='Zip')
	homephone = forms.TextField(label_text='Phone')
	brag = forms.TextField(label_text='Brag')
	sponsor = forms.TextField(label_text='Sponsor')

class PersonFormValidated(forms.TableForm):
	template = "nwrsc.forms.table_form"
	fields = validateName+commonFields

class PersonForm(forms.TableForm):
	template = "nwrsc.forms.table_form"
	fields = dontValidateName+commonFields


personFormValidated = PersonFormValidated("personFormValidated", children=validateName+commonFields)
personForm = PersonForm("personForm", children=dontValidateName+commonFields)

