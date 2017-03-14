import sys
import os.path
import logging
import threading
from logging.handlers import RotatingFileHandler

from psycopg2 import OperationalError
from psycopg2.pool import ThreadedConnectionPool
from psycopg2.extras import DictCursor
from psycopg2.extensions import ISOLATION_LEVEL_AUTOCOMMIT
from werkzeug.debug.tbtools import get_current_traceback
from werkzeug.contrib.profiler import ProfilerMiddleware
from flask import Flask, request, g, current_app, render_template, send_from_directory
from flask_compress import Compress

from nwrsc.controllers.results import Results
from nwrsc.controllers.feed import Xml, Json
from nwrsc.model import Series


class FlaskWithPool(Flask):
    """ Add some PGPool operations so we can reset from a request context when the DB is restarted on us """
    def __init__(self, name):
        Flask.__init__(self, name)
        self.resetlock = threading.Lock()

    def create_pool(self):
        self.pool = ThreadedConnectionPool(5, 10, cursor_factory=DictCursor, host="127.0.0.1", dbname="scorekeeper",
                                             user=self.config['DBUSER'], password=self.config['DBPASS'])

    def reset_pool(self):
        """ First person here gets to reset it, others can continue on and try again """
        if self.resetlock.acquire(False):
            try:
                if not self.pool.closed:
                    self.pool.closeall()
                self.create_pool()
            finally:
                self.resetlock.release()


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
        """ Render a list of series available in the current database """
        return render_template('serieslist.html', serieslist=Series.list())

    def favicon():
        """ Return our cone icon as the favicon """
        return send_from_directory('static', 'cone.png')

    def t3(val):
        """ Wrapper to safely print floats as XXX.123 format """
        if val is None: return ""
        if type(val) is not float: return str(val)
        try:
            return "%0.3f" % (val)
        except:
            return str(val)


    # Figure out where we are
    installroot = os.path.abspath(os.path.join(sys.executable, "../../../"))

    # Setup the application with default configuration
    theapp = FlaskWithPool("nwrsc")
    theapp.config.update({
        "PORT": 80,
        "DEBUG": False,
        "PROFILE": False,
        "LOGGER_HANDLER_POLICY":"None",
        "DBUSER":"wwwuser",
        "DBPASS":"wwwuser",
        "SHOWLIVE":True,
        "SHOWGRID":True,
        "INSTALLROOT": installroot
    })

    # Let the site config override what it wants
    theapp.config.from_json(os.path.join(installroot, 'siteconfig.json'))

    # If not using Flask debugging webserver, log our exceptions to the regular errorlog
    if not theapp.debug:
        theapp.register_error_handler(Exception, errorlog)

    # Setup basic top level URL handling followed by Blueprints for the various sections
    theapp.url_value_preprocessor(preprocessor)
    theapp.url_defaults(urldefaults)
    theapp.add_url_rule('/',            'serieslist', redirect_to='/results')
    theapp.add_url_rule('/favicon.ico', 'favicon',    favicon)
    theapp.add_url_rule('/<subapp>/',   'serieslist', serieslist)
    theapp.register_blueprint(Results, url_prefix="/results/<series>")
    theapp.register_blueprint(Xml,     url_prefix="/xml/<series>")
    theapp.register_blueprint(Json,    url_prefix="/json/<series>")

    # Create a PG connection pool and extra Jinja bits
    theapp.create_pool()
    theapp.jinja_env.filters['t3'] = t3
    theapp.jinja_env.globals['zip'] = zip

    # Configure our logging to use webserver.log with rotation
    path = os.path.join(installroot, 'logs/webserver.log')
    handler = RotatingFileHandler(path, maxBytes=1000000, backupCount=10)
    handler.setFormatter(logging.Formatter('%(asctime)s %(name)s %(levelname)s: %(message)s', '%m/%d/%Y %H:%M:%S'))
    handler.setLevel(logging.DEBUG)
    theapp.logger.addHandler(handler)
    theapp.logger.setLevel(logging.INFO)

    # Create some wrapping pieces for setting up PG connection, response logging, compression and profiling
    DBSeriesWrapper(theapp)
    ResponseLogger(theapp)
    Compress(theapp)
    if theapp.config.get('PROFILE', False):
        theapp.wsgi_app = ProfilerMiddleware(theapp.wsgi_app, restrictions=[30])

    # Good to go
    theapp.logger.info("Scorekeeper App created")
    return theapp


class DBSeriesWrapper(object):
    """ Get a database connection from the pool and setup the schema path, teardown on request end """
    def __init__(self, app):
        self.app = app
        self.app.before_request(self.onrequest)
        self.app.teardown_request(self.teardown)

    def onrequest(self):
        try:
            print("series setup starting ...")
            self.series_setup()
            print("... done")
        except OperationalError as e:
            print(".. exception ", e)
            self.app.logger.warning("Possible database restart.  Reseting pool and trying again!")
            self.app.reset_pool()
            self.series_setup()

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

