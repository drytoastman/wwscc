from formencode import Schema, ForEach
from formencode.validators import Number, String
from formencode.variabledecode import NestedVariables

class Decimal(Number):
	def _from_python(self, value, state):
		return "%0.3f" % (value)

"""
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
"""

class IndexEntry(Schema):
	allow_extra_fields = True
	filter_extra_fields = True
	code = String(not_empty=False)
	descrip = String(not_empty=False)
	value = Decimal(not_empty=False)

class IndexSchema(Schema):
	pre_validators = [NestedVariables()] 
	allow_extra_fields = True
	filter_extra_fields = True
	idxlist = formencode.ForEach(IndexEntry())


