"""
Default engine plugin for TGWidgets. 

Based on BuffetString which is Copyright (c) 2006 Christian Wyglendowski
"""

import string
import os

from pkg_resources import resource_filename

class ToscaWidgetsTemplatePlugin(object):
    # all template plugins need to define a default file extension
    extension = ".html"

    def __init__(self, extra_vars_func=None, config=None):
        """extra_vars_func == optional callable() that returns a dict
        config == optional dict() of configuration settings
        """
        self.get_extra_vars = extra_vars_func
        if config:
            self.config = config
        else:
            self.config = dict()

    def load_template(self, template_name, template_string=None):
        """
        template_name == dotted.path.to.template (without .ext)
        template_string == string containing the template
        
        The dotted notation is present because many template engines
        allow templates to be compiled down to Python modules.  TurboGears
        uses that feature to its adavantage, and for ease of integration
        the python.templating.engines plugin format requires the path to
        the template to be supplied as a dotted.path.to.template regardless
        of whether is is a module or not.

        In the case of string.Template templates, they are just simple text
        files, so we deal with the dotted notation and translate it into a
        standard file path to open the text file.
        """
        if template_string is not None:
            return string.Template(template_string)
        
        divider = template_name.rfind('.')
        if divider >= 0:
            package = template_name[:divider]
            basename = template_name[divider + 1:] + self.extension
            template_name = resource_filename(package, basename)

        template_file = open(template_name)
        template_obj = string.Template(template_file.read())
        template_file.close()
        
        return template_obj

    def render(self, info, format="html", fragment=False, template=None):
        """
        info == dict() of variables to stick into the template namespace
        format == output format if applicable
        fragment == special rules about rendering part of a page
        template == compiled template as returned by `load_template`
        """
        
        # check to see if we were passed a function get extra vars
        if callable(self.get_extra_vars):
            info.update(self.get_extra_vars())
        return template.safe_substitute(**info)
