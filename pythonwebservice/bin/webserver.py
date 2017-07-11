#!/usr/bin/env python3

import signal
import os, sys
from cheroot import wsgi
from cheroot.workers import threadpool
from nwrsc.app import create_app

server = None

def removepid(signum, frame):
    global server
    if server:
        server.stop()
    os.remove(pidfile)
    sys.exit(0)

def patchinit(orig):
    def newinit(self, server):
        orig(self, server)
        self.daemon = True
    return newinit

def justdie(self, timeout=None):
    return

if __name__ == '__main__':
    pidfile = os.path.expanduser('~/nwrscwebserver.pid')
    theapp = create_app()
    port = theapp.config['PORT']

    signal.signal(signal.SIGABRT, removepid)
    signal.signal(signal.SIGINT,  removepid)
    signal.signal(signal.SIGTERM, removepid)
    with open(pidfile, 'w') as fp:
        fp.write(str(os.getpid()))

    if theapp.debug:
        theapp.run(host='0.0.0.0', port=port, threaded=True)
    else:
        threadpool.WorkerThread.__init__ = patchinit(threadpool.WorkerThread.__init__)
        threadpool.ThreadPool.stop = justdie 
        server = wsgi.Server(('0.0.0.0', port), theapp, numthreads=60, shutdown_timeout=1, server_name="Scorekeeper 2.0")
        server.start()

    removepid(0, 0) # just in case we get here somehow

