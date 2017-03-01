import os.path
import logging
from logging.handlers import RotatingFileHandler

from psycopg2.pool import ThreadedConnectionPool
from psycopg2.extras import DictCursor
from psycopg2.extensions import ISOLATION_LEVEL_AUTOCOMMIT
from werkzeug.debug.tbtools import get_current_traceback
from flask import Flask, request, g, current_app
from flask_compress import Compress

from nwrsc.controllers.results import Results

def create_app(config=None):
    """ Setup the application for the WSGI server """

    def errorlog(exception):
        """ We want to log exception information to file for later investigation """
        traceback = get_current_traceback(ignore_system_exceptions=True, show_hidden_frames=True)
        theapp.logger.error(traceback.plaintext)
        last = traceback.frames[-1]
        return "%s:%s %s" % (os.path.basename(last.filename), last.lineno, exception);

    def serieshook1(endpoint, values):
        """ Stop the requirement for blueprint functions to put series in their function definitions """
        if values is not None:
            g.series = values.pop('series', None)

    def serieshook2(endpoint, values):
        """ Make sure 'series' from the blueprint URLs is available for url_for relative calls """
        if 'series' in values or not g.series:
            return
        if current_app.url_map.is_endpoint_expecting(endpoint, 'series'):
            values['series'] = g.series

    def serieslist(blueprint=None):
        return "series list here %s" % blueprint

    # Setup the application
    theapp = Flask("nwrsc")
    theapp.config.update(DEBUG=True, LOGGER_HANDLER_POLICY="None")
    theapp.add_url_rule('/', 'serieslist', serieslist)
    theapp.add_url_rule('/<blueprint>/', 'serieslist', serieslist)
    theapp.register_blueprint(Results, url_prefix="/results/<series>")
    theapp.url_value_preprocessor(serieshook1)
    theapp.url_defaults(serieshook2)
    theapp.pool = ThreadedConnectionPool(5, 10, host="127.0.0.1", dbname="scorekeeper", user="wwwuser", password="wwwuser", cursor_factory=DictCursor)

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
            with g.db.cursor() as cur:
                cur.execute("select schema_name from information_schema.schemata where schema_name=%s", (g.series,))
                if cur.rowcount > 0:
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

