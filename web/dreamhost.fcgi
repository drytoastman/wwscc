#!/home/brett_wilson/bin/python

import sys
configfile = '/home/brett_wilson/dreamhost.ini'

# Load the WSGI application from the config file
from paste.deploy import loadapp
wsgi_app = loadapp('config:%s' % configfile)

from paste.script.util.logging_config import fileConfig
fileConfig(configfile)

# Deploy it using FastCGI
from fcgi import WSGIServer
WSGIServer(wsgi_app).run()

