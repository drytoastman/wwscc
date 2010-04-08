from tw import forms
from tw import dynforms
from tw.forms.validators import Int, Number


class Decimal(Number):
	def _from_python(self, value, state):
		return "%0.3f" % (value)

class ClassListTableForm(dynforms.GrowingTableForm):
	template = "nwrsc.forms.growing_table_form"
	children = [
		forms.TextField('code', label_text='Code', size=6),
		forms.TextField('descrip', label_text='Description', size=40),
		forms.CheckBox('eventtrophy', label_text='Event<br>Trophy', help_text='Receives trophies at events'),
		forms.CheckBox('champtrophy', label_text='Champ<br>Trophy', help_text='Receives trophies for the series'),
		forms.CheckBox('carindexed', label_text='Car<br>Indexed', help_text='Cars are individually indexed by index value'),
		forms.CheckBox('classindexed', label_text='Class<br>Indexed',
				help_text='Entire class is indexed matching class code to an index code'),
		forms.TextField('classmultiplier', label_text='Class<br>Multiplier', size=5, validator=Decimal(),
				help_text='This multiplier is applied to entire class, i.e. street tire factor'),
		forms.HiddenField('numorder')
	]

class IndexListTableForm(dynforms.GrowingTableForm):
	template = "nwrsc.forms.growing_table_form"
	children = [
		forms.TextField('code', label_text='Code', size=6),
		forms.TextField('descrip', label_text='Description', size=50),
		forms.TextField('value', label_text='Value', size=5, validator=Decimal()),
	]


classEditForm = ClassListTableForm("classEditForm")
indexEditForm = IndexListTableForm("indexEditForm")

