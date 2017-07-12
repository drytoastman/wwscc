#!/usr/bin/env python3

import datetime
import json
import logging
import socket
import sys
import threading
import time
import uuid

import psycopg2
import psycopg2.extras
#from zeroconf import ServiceBrowser, ServiceInfo, Zeroconf

from nwrsc.model import Series
import nwrsc.merge.model as m

log  = logging.getLogger(__name__)
TYPE = "_scorekeeperdb._tcp.local."

class IgnoreThisEvent(Exception):
    pass

class MergeProcess(threading.Thread):

    def __init__(self, daemon=True, localport=54329):
        threading.Thread.__init__(self, daemon=daemon)
        self.localport = localport

    def shutdown(self):
        self.zeroconf.unregister_all_services()
        self.zeroconf.close()

    def run(self):
        args = {
            'cursor_factory':psycopg2.extras.DictCursor,
            'host':"127.0.0.1", 
            'port':self.localport,
            'dbname':"scorekeeper",
            'application_name':"mergetool",
            'user':"localuser"
        }
        psycopg2.extras.register_uuid()
        self.localdb = psycopg2.connect(**args)

        m.clearactive(self.localdb)
        me = m.getlocalhost(self.localdb)
        if me is None:
            me = {'serverid':uuid.uuid1(), 'active':False, 'lastmerge':m.LOCALTIME, 'attr':{}}
            m.newserver(self.localdb, me)

        me['attr']['serverid'] = str(me['serverid'])
        me['attr']['name'] = socket.gethostname()
        me['attr']['ip']   = socket.gethostbyname(socket.gethostname())
        me['attr']['port'] = self.localport 
        me['attr']['db']   = ','.join(Series.activeindb(self.localdb))

        m.updateserver(self.localdb, me)
        self.localdb.commit()

        ip = socket.inet_aton(me['attr']['ip'])
        fullname = "{}.{}".format(me['attr']['serverid'], TYPE)
        myinfo = ServiceInfo(TYPE, fullname, ip, self.localport, 0, 0, json.dumps(me['attr']).encode('utf-8'))
        log.info("I am {}: {}".format(fullname, me))

        self.zeroconf = Zeroconf()
        self.zeroconf.register_service(myinfo)
        ServiceBrowser(self.zeroconf, TYPE, self)

        while True:
            for remote in m.activeservers(self.localdb):
                # If we have the greater serverid, we should be doing the work
                # Still fall back and try a merge ourselves if the other person is delayed
                waittime = me['serverid'] > remote['serverid'] and 5 or 2
                print("{}:{} waittime {}".format(remote['attr']['ip'], remote['attr']['port'], waittime))
                if remote['lastmerge'] + datetime.timedelta(minutes=waittime) < datetime.datetime.now():
                    print("merge")
                    args = {
                        'cursor_factory':psycopg2.extras.DictCursor,
                        'host':remote['attr']['ip'],
                        'port':remote['attr']['port'],
                        'dbname':"scorekeeper",
                        'application_name':"mergetool",
                        'user':"localuser"
                    }

                    conn = psycopg2.connect(**args)
                    # do merge here
                    # if connect timeout, mark as inactive?

                    mirror = m.getserver(conn, me['attr']['serverid'])
                    mirror['lastmerge'] = datetime.datetime.now()
                    m.updateserver(conn, mirror)

                    remote['lastmerge'] = datetime.datetime.now()
                    m.updateserver(self.localdb, remote)

                    conn.commit()
                    conn.close()

                    self.localdb.commit()

            time.sleep(5)


    def extractinfo(self, type, name):
        info = self.zeroconf.get_service_info(type, name)
        if info is None:
            raise IgnoreThisEvent()
        attr = json.loads(info.text)
        if 'serverid' not in attr:
            log.error("The discovered info has no serverid field ({})".format(attr))
            raise IgnoreThisEvent()
        remote = m.getserver(self.localdb, attr['serverid'])
        if remote is not None and remote['lastmerge'] >= m.LOCALTIME: # Ignore discovery of myself
            raise IgnoreThisEvent()
        return remote, attr

    def remove_service(self, z, type, name):
        try:
            remote,attr = self.extractinfo(type, name)
            if remote is None:
                return
            log.info("Removing database {}: {}".format(name, remote))
            remote['active'] = False
            m.updateserver(self.localdb, remote)
            self.localdb.commit()
        except IgnoreThisEvent:
            pass

    def add_service(self, z, type, name):
        try:
            remote,attr = self.extractinfo(type, name)
            if remote is None:
                remote = {'serverid':attr['serverid'], 'active':False, 'lastmerge':'epoch', 'attr':{}}
                m.newserver(self.localdb, remote)
                log.info("New server discovered: {}".format(name))
            remote['active'] = True
            remote['attr'] = attr.copy()
            m.updateserver(self.localdb, remote)
            self.localdb.commit()
            log.info("Discovered database {}: {}".format(name, remote))
        except IgnoreThisEvent:
            pass

if __name__ == '__main__':
    port = int(sys.argv[1])
    logging.basicConfig(level=0, format='%(asctime)s %(name)s %(levelname)s: %(message)s', datefmt='%m/%d/%Y %H:%M:%S')
    mp = MergeProcess(localport=port)
    mp.start()
    input('press any key to exit')
    mp.shutdown()


