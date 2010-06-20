"""Helpers for the Pylons web framework

These helpers depend on Pylons' ``request``, ``response``, ``session``
objects or some other aspect of Pylons.  Most of them can be easily ported to
another framework by changing the API calls.
"""

# Do not import Pylons at module level; only within functions.  All WebHelpers
# modules should be importable on any Python system for the standard
# regression tests.

class Flash(object):
    """Accumulate a list of messages to show at the next page request.

    This class is useful when you want to redirect to another page and also
    show a status message on that page, such as "Changes saved" or 
    "No previous search found; returning to home page".

    THIS IMPLEMENTATION DEPENDS ON PYLONS.  However, it can easily be adapted
    for another web framework.

    Normally you instantiate a Flash object in myapp/lib/helpers.py::

        from webhelpers.pylonslib import Flash as _Flash
        flash = _Flash()

    The helpers module is then imported into your controllers and
    templates as `h`.  Whenever you want to set a message, do this::

        h.flash("Record deleted.")

    You can set additional messages too::

        h.flash("Hope you didn't need it.")

    Now make a place in your site template for the messages.  In Mako you
    might do:
    
    .. code-block:: mako
    
        <% messages = h.flash.pop_messages() %>
        % if messages:
        <ul id="flash-messages">
            % for message in messages:
            <li>${message}</li>
            % endfor
        </ul>
        % endif

    You can style this to look however you want:

    .. code-block:: css

        ul#flash-messages {
            color: red;
            background-color: #FFFFCC;
            font-size: larger;
            font-style: italic;
            margin-left: 40px;
            padding: 4px;
            list-style: none;
            }
    """
    def __init__(self, session_key="flash"):
        self.session_key = session_key

    def __call__(self, message):
        from pylons import session
        session.setdefault(self.session_key, []).append(message)
        session.save()

    def pop_messages(self):
        from pylons import session
        messages = session.pop(self.session_key, [])
        session.save()
        return messages
