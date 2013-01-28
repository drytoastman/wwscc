"""Pylons environment configuration"""
import os

#from mako.lookup import TemplateLookup
from paste.deploy.converters import asbool
from pylons.error import handle_mako_error
from pylons import config

import nwrsc.lib.app_globals as app_globals
import nwrsc.lib.helpers
from nwrsc.config.routing import make_map
from nwrsc.config.dblookup import DatabaseLookup

def load_environment(global_conf, app_conf):
    """Configure the Pylons environment via the ``pylons.config``
    object
    """
    # Pylons paths
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    paths = dict(root=root,
                 controllers=os.path.join(root, 'controllers'),
                 static_files=os.path.join(root, 'public'),
                 templates=[os.path.join(root, 'templates')])

    # Initialize config with the basic options
    config.init_app(global_conf, app_conf, package='nwrsc', paths=paths)

    config['routes.map'] = make_map()
    config['pylons.app_globals'] = app_globals.Globals()
    config['pylons.h'] = nwrsc.lib.helpers
    config['nwrsc.onsite'] = asbool(config.get('nwrsc.onsite', 'false'))
    config['nwrsc.private'] = asbool(config.get('nwrsc.private', 'false'))
    if 'archivedir' not in config:
        config['archivedir'] = '/doesnotexist'

    # Create the Mako TemplateLookup, with the default auto-escaping
    config['pylons.app_globals'].mako_lookup = DatabaseLookup(
        directories=paths['templates'],
        error_handler=handle_mako_error,
        module_directory=os.path.join(app_conf['cache_dir'], 'templates'),
        input_encoding='utf-8', output_encoding='utf-8',
        imports=['from webhelpers.html import escape; from nwrsc.lib.helpers import oneline'],
        default_filters=['escape'])
    
    # CONFIGURATION OPTIONS HERE (note: all config options will override
    # any Pylons config options)
