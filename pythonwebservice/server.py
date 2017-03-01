#!/usr/bin/env python

import cherrypy
import sys
from nwrsc import create_app

app = create_app()

if app.debug:
    # If set to debug, use the flask development server
    app.run(host='0.0.0.0', port=8080)

else:
    # Otherwise we run the cherrypy server
    cherrypy.tree.graft(app, "/")
    cherrypy.server.unsubscribe()
    server = cherrypy._cpserver.Server()
    server.socket_host = "0.0.0.0"
    server.socket_port = 80
    server.thread_pool = 60
    server.shutdown_timeout = 1
    server.subscribe()
    cherrypy.engine.start()
    cherrypy.engine.block()

