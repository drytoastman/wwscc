
from tw import forms
from tw.forms.validators import Int
from tw.api import WidgetsList

class SeriesCopyForm(forms.TableForm):
	template = "nwrsc.forms.table_form"

	class fields(WidgetsList):
		name = forms.TextField(help_text='the new series name', size='40')
		password = forms.TextField()
		settings = forms.CheckBox(help_text='copy settings over', label_text='Copy Settings')
		data = forms.CheckBox(help_text='copy result and card templates over', label_text='Copy Templates')
		classes = forms.CheckBox(help_text='copy classes/indexes over', label_text='Copy Classes/Indexes')
		drivers = forms.CheckBox(help_text='copy drivers over', label_text='Copy Drivers')
		cars = forms.CheckBox(help_text='copy cars over, useless without drivers', label_text='Copy Cars')
		prevlist = forms.CheckBox(help_text='create a prevlist from this series', label_text='PrevList')

seriesCopyForm = SeriesCopyForm("seriesCopyForm")

