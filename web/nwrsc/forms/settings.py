
from tw import forms
from tw.forms.validators import Int
from tw.api import WidgetsList

class SettingsForm(forms.TableForm):
	template = "nwrsc.forms.table_form"

	class fields(WidgetsList):
		seriesname = forms.TextField(help_text='the name of the series', label_text='Series Name', size='40')
		password = forms.TextField(help_text='the series password', label_text='Password', size='40')
		useevents = forms.TextField(help_text='number of events to use in championship calculation',
									label_text='Best X Events', validator = Int(), size='4')
		sponsorlink = forms.TextField(help_text='URL link for sponsor banner', label_text='Sponsor Link', size='40')
		ppoints = forms.TextField(help_text='Ordering of points if using static points', label_text='Points', size='40')

		
settingsForm = SettingsForm("settingsForm")

