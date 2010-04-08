
from tw import forms
from tw.forms.validators import Int
from tw.api import WidgetsList

class EventForm(forms.TableForm):
	template = "nwrsc.forms.table_form"

	class fields(WidgetsList):
		name = forms.TextField(help_text='the event name, what else', size='40')
		date = forms.CalendarDatePicker(help_text='date when the event occurs')
		password = forms.TextField()
		location = forms.TextField(help_text='the event location', size='40')
		sponsor = forms.TextField(help_text='the event or series sponsor', size='40')
		host = forms.TextField(help_text='the hosting club', size='40')
		designer = forms.TextField(help_text='the course designer', size='40')
		chair = forms.TextField(help_text='the event chair', size='40')
		ispro = forms.CheckBox(help_text='check if a ProSolo', label_text='Is a Pro')
		courses = forms.TextField(help_text='number of courses, usually 1', validator = Int(), size='4')
		runs = forms.TextField(validator = Int(), size='4')
		segments = forms.TextField(size='40')
		regopened = forms.CalendarDateTimePicker(help_text='When prereg should open', label_text='Registration Opens')
		regclosed = forms.CalendarDateTimePicker(help_text='When prereg should close', label_text='Registration Closes')
		perlimit = forms.TextField(help_text='Preregistred cars allowed per person', 
									validator = Int(), 
									label_text='Per-Person Limit', 
									size='4')
		totlimit = forms.TextField(help_text='Preregistred cars allowed for the whole event', 
									validator = Int(),
									label_text='Event Limit',
									size='4')
		paypal = forms.TextField(help_text='Enter a paypal email address to enable paypal payments',
									label_text='Paypal Address',
									size='40')
		cost = forms.TextField(help_text='Prepayment amount for paypal', validator = Int(), label_text='Prepay Cost', size='4')
		snail = forms.TextArea(label_text='Mailing Address', help_text='A payment mailing address, use HTML to format')
		notes = forms.TextArea(help_text='Event specific notes, use HTML to format')

eventForm = EventForm("eventForm")

