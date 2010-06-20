from tw.api import WidgetsList, CSSSource, JSSource, js_function
from tw.forms import *
from tw.forms.validators import *
from formencode.national import USPostalCode as PostalCode, \
                                USStateProvince as StateProvince

__all__ = ["AddUserForm"]

# Lil' chunk o' CSS for tasty eye-candy ;)
# External css files can be wrapped with CSSLink
css = CSSSource("""
label.required, .fielderror {
    font-weight: bold;
    color: red;
};
""")


# We define the source for some JS functions we're going to interface
# External js files can be wrapped with JSLink
functions = JSSource("""
var focus_element = function (elem) {
    var elem = document.getElementById(elem);
    elem.focus(); elem.select();
    }; 
    """,
    )

alert = js_function('alert')
focus_element = js_function('focus_element')


# This is needed because of the way TurboGears validates as it adds
# spurious variables to the value being validated.
class FilteringSchema(Schema):
    filter_extra_fields = True
    allow_extra_fields = True


# Now the form widgets....


class AddressFieldset(ListFieldSet):
    class fields(WidgetsList):
        street = TextField(validator=UnicodeString)
        number = TextField(validator=Int, size=4)
        zip_code = TextField(validator=PostalCode())
        state = TextField(default='NY',validator=StateProvince())

    validator = FilteringSchema

class AddUserForm(ListForm):
    class fields(WidgetsList):
        id = HiddenField(default="I'm hidden!")
        name = TextField(
            validator = UnicodeString(not_empty=True), 
            default = "Your name here"
            )
        gender = RadioButtonList(
            options = "Male Female".split(),
            )
        age = SingleSelectField(
            validator = Int, 
            options = range(100)
            )
        email = TextField(
            validator = Email()
            )
        date = CalendarDateTimePicker()
        roles = CheckBoxList(
            options = "Manager Admin Editor User".split(),
            )
        groups = MultipleSelectField(
            options = "Group1 Group2 Group3".split(),
            )
        password = PasswordField(
            validator = String(not_empty=True), 
            max_size = 10
            )
        password_confirm = PasswordField(
            validator = String(not_empty=True), 
            max_size=10
            )
        # We wrap the address fieldset with a FormFieldRepeater to handle
        # repetitions. This can be done with *any* FormField.
        address = FormFieldRepeater(
            widget = AddressFieldset(), 
            repetitions = 2, 
            max_repetitions = 5
            )

    # allow adding js calls dynamically for a request
    include_dynamic_js_calls = True

    css = [css]
    javascript = [functions]
    validator = FilteringSchema(
        chained_validators = [FieldsMatch('password','password_confirm')],
        )


    def update_params(self, d):
        super(AddUserForm, self).update_params(d)
        # Focus and select the 'name' field on the form
        # The adapter we just wrote lets us pass formfields as parameters and
        # the right thing will be done.
        if not d.error:
            self.add_call(focus_element(d.c.name))
        else:
            self.add_call(
                alert('The form contains invalid data\n%s'% unicode(d.error))
                )

class DemoSingleSelect(SingleSelectField):
    options = [
        "Python",
        "Haskell",
        "Java",
        "Ruby",
        "Erlang",
        "Javascript"
        ]

class DemoMultipleSelect(MultipleSelectField):
    options = [
        "Python",
        "Haskell",
        "Java",
        "Ruby",
        "Erlang",
        "Javascript"
        ]

class DemoCheckBoxList(CheckBoxList):
    options = [
        "Python",
        "Haskell",
        "Java",
        "Ruby",
        "Erlang",
        "Javascript"
        ]

class DemoRadioButtonList(RadioButtonList):
    options = [
        "Python",
        "Haskell",
        "Java",
        "Ruby",
        "Erlang",
        "Javascript"
        ]

class DemoCheckBoxTable(CheckBoxTable):
    num_cols = 2
    options = [
        "Python",
        "Haskell",
        "Java",
        "Ruby",
        "Erlang",
        "Javascript"
        ]

class Person(object):
    def __init__(self, name, age):
        self.__dict__.update(locals())

class DemoDataGrid(DataGrid):
    fields = [("Name", "name"), ("Age","age")]
    default = [
        Person('Lucy', 29),
        Person('Peter', 15),
        Person('Tiffany', 17),
        ]
