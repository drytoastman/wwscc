
from tw import forms
forms.FormField.engine_name = "mako"

from event import eventForm
from login import loginForm
from person import personForm, personFormValidated
from settings import settingsForm
from series import seriesCopyForm
from classlist import classEditForm, indexEditForm

__all__ = [ 'eventForm', 'loginForm', 'personForm', 'personFormValidated', 'settingsForm', 'seriesCopyForm', 'classEditForm', 'indexEditForm' ]

