import os.path
import logging
from logging.handlers import RotatingFileHandler

from psycopg2.pool import ThreadedConnectionPool
from psycopg2.extras import DictCursor
from psycopg2.extensions import ISOLATION_LEVEL_AUTOCOMMIT
from werkzeug.debug.tbtools import get_current_traceback
from flask import Flask, request, g, current_app, render_template
from flask_compress import Compress

from nwrsc.controllers.results import Results
from nwrsc.controllers.xml import Xml
from nwrsc.model import Series

def create_app(config=None):
    """ Setup the application for the WSGI server """

    def errorlog(exception):
        """ We want to log exception information to file for later investigation """
        traceback = get_current_traceback(ignore_system_exceptions=True, show_hidden_frames=True)
        theapp.logger.error(traceback.plaintext)
        last = traceback.frames[-1]
        return "%s:%s %s" % (os.path.basename(last.filename), last.lineno, exception);

    def preprocessor(endpoint, values):
        """ Remove the requirement for blueprint functions to put subapp/series in their function definitions """
        if values is not None:
            g.subapp = values.pop('subapp', None)
            g.series = values.pop('series', None)
            g.eventid = values.pop('eventid', None)

    def urldefaults(endpoint, values):
        """ Make sure 'series' from the subapp URLs is available for url_for relative calls """
        for u in ('subapp', 'series', 'eventid'):
            if u not in values and getattr(g, u) and current_app.url_map.is_endpoint_expecting(endpoint, u):
                values[u] = getattr(g, u)

    def serieslist(subapp='results'):
        return render_template('serieslist.html', serieslist=Series.list())

    def t3(val):
        if val is None: return ""
        if type(val) is int: return str(val)
        try:
            return "%0.3f" % (val)
        except:
            return str(val)

    # Setup the application
    theapp = Flask("nwrsc")
    theapp.config.from_json("config/defaultconfig.json")
    theapp.url_value_preprocessor(preprocessor)
    theapp.url_defaults(urldefaults)
    theapp.add_url_rule('/',          'serieslist', redirect_to='/results')
    theapp.add_url_rule('/<subapp>/', 'serieslist', serieslist)
    theapp.register_blueprint(Results, url_prefix="/results/<series>")
    theapp.register_blueprint(Xml,     url_prefix="/xml/<series>")
    theapp.pool = ThreadedConnectionPool(5, 10, host="127.0.0.1", dbname="scorekeeper", user=theapp.config['DBUSER'], password=theapp.config['DBPASS'], cursor_factory=DictCursor)
    theapp.jinja_env.filters['t3'] = t3


    if not theapp.debug:
        # If not using Flask debugger, log our exceptions
        theapp.register_error_handler(Exception, errorlog)

    # Configure our logging to use nwrsc.log with rotation
    handler = RotatingFileHandler('nwrsc.log', maxBytes=10000, backupCount=1)
    handler.setFormatter(logging.Formatter('%(asctime)s %(name)s %(levelname)s: %(message)s', '%m/%d/%Y %H:%M:%S'))
    handler.setLevel(logging.DEBUG)
    theapp.logger.addHandler(handler)

    DBSeriesWrapper(theapp)
    ResponseLogger(theapp)
    Compress(theapp)

    theapp.logger.info("NWRSC App created")
    theapp.logger.setLevel(logging.INFO)
    return theapp


class DBSeriesWrapper(object):
    """ Get a database connection from the pool and setup the schema path, teardown on request end """
    def __init__(self, app):
        self.app = app
        self.app.before_request(self.series_setup)
        self.app.teardown_request(self.teardown)

    def series_setup(self):
        """ Check if we have the series in the URL, set the schema path if available, return an error message otherwise """
        g.db =  self.app.pool.getconn() 
        if hasattr(g, 'series') and g.series:
            # Set up the schema path if we have a series
            if Series.exists(g.series):
                with g.db.cursor() as cur:
                    cur.execute("SET search_path=%s,'public'", (g.series,))
            else:
                return "%s is not a valid series" % g.series

    def teardown(self, exc=None):
        """ Make sure that the connection is commited so it doesn't sit in a locked state, put back in the pool """
        g.db.commit()
        self.app.pool.putconn(g.db) 


class ResponseLogger(object):
    """ Extra step so we can log the requests independant of the server used """
    def __init__(self, app):
        self.app = app
        self.app.after_request(self.log_response)

    def log_response(self, response):
        if response.content_encoding is not None:
            self.app.logger.info("%s %s?%s %s %s (%s)" % (request.method, request.path, request.query_string, response.status_code, response.content_length, response.content_encoding))
        else:
            self.app.logger.info("%s %s?%s %s %s" % (request.method, request.path, request.query_string, response.status_code, response.content_length))
        return response

