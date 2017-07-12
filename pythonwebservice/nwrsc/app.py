import sys
import os
import datetime
import logging
import threading
from logging import StreamHandler
from logging.handlers import RotatingFileHandler
from operator import attrgetter

from flask import Flask, request, abort, g, current_app, render_template, send_from_directory
from flask_compress import Compress
from flask_assets import Environment, Bundle
from flask_bcrypt import Bcrypt
from itsdangerous import URLSafeTimedSerializer
from psycopg2 import OperationalError, DatabaseError
from psycopg2.pool import ThreadedConnectionPool
from psycopg2.extras import DictCursor, register_uuid
from werkzeug.debug.tbtools import get_current_traceback
from werkzeug.contrib.profiler import ProfilerMiddleware

from nwrsc.controllers.admin import Admin
from nwrsc.controllers.dynamic import Announcer, Timer
from nwrsc.controllers.feed import Xml, Json
from nwrsc.controllers.register import Register
from nwrsc.controllers.results import Results
from nwrsc.lib.encoding import to_json
from nwrsc.lib.postgresql import ensure_database_created, ensure_public_schema
from nwrsc.model import Series
from nwrsc.merge.process import MergeProcess

log = logging.getLogger(__name__)


class FlaskWithPool(Flask):
    """ Add some PGPool operations so we can reset from a request context when the DB is restarted on us """
    def __init__(self, name):
        Flask.__init__(self, name)
        self.resetlock = threading.Lock()
        self.pool = None

    def db_prepare(self):
        """ Check if we have the series in the URL, set the schema path if available, return an error message otherwise """
        if hasattr(g, 'db'):
            raise EnvironmentError('Database has already been prepared.  Preparing again is an error.')

        g.db = self._get_from_pool()
        if hasattr(g, 'series') and g.series:
            # Set up the schema path if we have a series
            g.seriestype = Series.type(g.series)
            if g.seriestype == Series.INVALID:
                abort(404, "%s is not a valid series" % g.series)
            with g.db.cursor() as cur:
                cur.execute("SET search_path=%s,'public'; commit", (g.series,))
        else:
            g.seriestype = Series.UNKNOWN

    def db_return(self):
        """ Return a connection to the pool and clear the attribute """
        if hasattr(g, 'db'):
            self.pool.putconn(g.db) 
            del g.db # Removes 'db' from g dictionary

    def _reset_pool(self):
        """ First person here gets to reset it, others can continue on and try again """
        if self.resetlock.acquire(False):
            try:
                if self.pool and not self.pool.closed:
                    self.pool.closeall()
                # Make sure the basic database is present and create a PG connection pool
                connkeys = { 'host':self.config['DBHOST'], 'port':self.config['DBPORT'], 'user':'postgres' }
                ensure_database_created(connkeys)
                ensure_public_schema(connkeys)
                # Create a new pool of connections.  Server should support 100, leave 10 for applications
                self.pool = ThreadedConnectionPool(5, 80, cursor_factory=DictCursor, application_name="webserver", dbname="scorekeeper",
                                                         host=self.config['DBHOST'], port=self.config['DBPORT'], user=self.config['DBUSER'])
            except Exception as e:
                log.error("Error in pool create/reset: %s", str(e))
            finally:
                self.resetlock.release()

    def _get_from_pool(self):
        """ Get a database connection from the pool and make sure its connected, attempt reset once if needed """
        try:
            ret = self.pool.getconn() 
            with ret.cursor() as cur:
                cur.execute("select 1")
        except (DatabaseError, OperationalError) as e:
            log.warning("Possible database restart.  Reseting pool and trying again!")
            try: 
                self._reset_pool()
                ret = self.pool.getconn() 
                with ret.cursor() as cur:
                    cur.execute("select 1")
            except (DatabaseError, OperationalError) as e:
                raise EnvironmentError("Errors with postgresql connection pool.  Bailing")

        #log.debug("{} setup: {} connections used".format(threading.current_thread(), len(self.pool._used)))
        return ret


def create_app(config=None):
    """ Setup the application for the WSGI server """

    def errorlog(exception):
        """ We want to log exception information to file for later investigation """
        traceback = get_current_traceback(ignore_system_exceptions=True, show_hidden_frames=True)
        log.error(traceback.plaintext)
        last = traceback.frames[-1]
        now = datetime.datetime.now().replace(microsecond=0)
        return render_template("error.html", now=now, name=os.path.basename(last.filename), line=last.lineno, exception=exception)

    def preprocessor(endpoint, values):
        """ Remove the requirement for blueprint functions to put series/eventid in their function definitions """
        if values is not None:
            g.series = values.pop('series', None)
            g.eventid = values.pop('eventid', None)

    def urldefaults(endpoint, values):
        """ Make sure series,eventid from the subapp URLs are available for url_for relative calls """
        for u in ('series', 'eventid'):
            if u not in values and getattr(g, u) and current_app.url_map.is_endpoint_expecting(endpoint, u):
                values[u] = getattr(g, u)

    def t3(val, sign=False):
        """ Wrapper to safely print floats as XXX.123 format """
        if val is None: return ""
        if type(val) is not float: return str(val)
        try:
            return (sign and "%+0.3f" or "%0.3f") % (val,)
        except:
            return str(val)

    def msort(val, *attr):
        """ Filter to sort on multiple attributes """
        ret = list(val)
        ret.sort(key=attrgetter(*attr))
        return ret

    # setup uuid for postgresql
    register_uuid()

    # Setup the application with default configuration
    theapp = FlaskWithPool("nwrsc")
    theapp.config.update({
        "PORT":                    int(os.environ.get('NWRSC_PORT',     80)),
        "DEBUG":                  bool(os.environ.get('NWRSC_DEBUG',    False)),
        "PROFILE":                bool(os.environ.get('NWRSC_PROFILE',  False)),
        "DBHOST":                      os.environ.get('NWRSC_DBHOST',   '192.168.24.3'),
        "DBPORT":                  int(os.environ.get('NWRSC_DBPORT',   5432)),
        "DBUSER":                      os.environ.get('NWRSC_DBUSER',   'localuser'),
        "SHOWLIVE":               bool(os.environ.get('NWRSC_SHOWLIVE', True)),
        "LOG_LEVEL":                   os.environ.get('NWRSC_LOGLEVEL', 'INFO'),
        "SECRET_KEY":                  os.environ.get('NWRSC_SECRET',   'secret stuff here'),
        "ASSETS_DEBUG":           False,
        "LOGGER_HANDLER_POLICY":  "None",
    })

    theapp.config['TEMPLATES_AUTO_RELOAD'] = theapp.config['DEBUG']
    theapp.config['LOG_STDERR']            = theapp.config['DEBUG']
    #"RUN_MERGER":

    # Setup basic top level URL handling followed by Blueprints for the various sections
    theapp.url_value_preprocessor(preprocessor)
    theapp.url_defaults(urldefaults)
    theapp.add_url_rule('/',             'toresults', redirect_to='/results')
    theapp.register_blueprint(Admin,     url_prefix="/admin/<series>")
    theapp.register_blueprint(Announcer, url_prefix="/announcer/<series>")
    theapp.register_blueprint(Json,      url_prefix="/json/<series>")
    theapp.register_blueprint(Register,  url_prefix="/register")
    theapp.register_blueprint(Results,   url_prefix="/results/<series>")
    theapp.register_blueprint(Timer,     url_prefix="/timer")
    theapp.register_blueprint(Xml,       url_prefix="/xml/<series>")

    # Some static things that need to show up at the root level
    @theapp.route('/favicon.ico')
    def favicon(): return send_from_directory('static/images', 'cone.png')
    @theapp.route('/robots.txt')
    def robots(): return send_from_directory('static', 'robots.txt')
    @theapp.route('/<subapp>/')
    def serieslist(subapp): return render_template('serieslist.html', subapp=subapp, serieslist=Series.list())
    @theapp.before_request
    def onrequest(): current_app.db_prepare()
    @theapp.teardown_request
    def teardown(exc=None): current_app.db_return()
    @theapp.after_request
    def logrequest(response):
        log.info("%s %s?%s %s %s (%s)" % (request.method, request.path, request.query_string, response.status_code, response.content_length, response.content_encoding))
        return response

    theapp._reset_pool()

    # extra Jinja bits
    theapp.jinja_env.filters['t3'] = t3
    theapp.jinja_env.filters['msort'] = msort
    theapp.jinja_env.filters['to_json'] = to_json

    # Configure our logging to use webserver.log with rotation and optionally stderr
    if not theapp.debug:
        theapp.register_error_handler(Exception, errorlog)

    level = getattr(logging, theapp.config['LOG_LEVEL'], logging.INFO)
    fmt  = logging.Formatter('%(asctime)s %(name)s %(levelname)s: %(message)s', '%m/%d/%Y %H:%M:%S')
    root = logging.getLogger()
    root.setLevel(level)
    root.handlers = []

    fhandler = RotatingFileHandler(os.path.expanduser('~/nwrscwebserver.log'), maxBytes=1000000, backupCount=10)
    fhandler.setFormatter(fmt)
    fhandler.setLevel(level)
    root.addHandler(fhandler)
    logging.getLogger('werkzeug').setLevel(logging.WARN)

    if theapp.config.get('LOG_STDERR', False):
        shandler = StreamHandler()
        shandler.setFormatter(fmt)
        shandler.setLevel(level)
        root.addHandler(shandler)

    # Setting up WebAssets, crypto stuff, compression and profiling
    Environment(theapp)
    Compress(theapp)
    if theapp.config.get('PROFILE', False):
        theapp.wsgi_app = ProfilerMiddleware(theapp.wsgi_app, restrictions=[30])
    theapp.hasher = Bcrypt(theapp)
    theapp.usts = URLSafeTimedSerializer(theapp.config["SECRET_KEY"])

    if theapp.config.get('RUN_MERGER', False):
        MergeProcess().start()

    log.info("Scorekeeper App created")
    return theapp

