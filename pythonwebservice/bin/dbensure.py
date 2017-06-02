#!/usr/bin/env python3

import sys
import time
import os.path
import subprocess

PROCESSED = "processed.sql"

HBA = """
# TYPE    DATABASE  USER        ADDRESS         METHOD
host      all       all         127.0.0.1/32    trust
hostnossl all       all         0.0.0.0/0       reject   # force SSL for non-localhost connections
host      all       postgres    0.0.0.0/0       reject   # no super user off site
host      all       +baseaccess 0.0.0.0/0       password
"""

CONFADDON = """
listen_addresses = '*'
port = 54329
authentication_timeout = 30s
#ssl = on
ssl_cert_file = 'server.crt'
ssl_key_file = 'server.key'
ssl_ca_file = 'scorekeeperca.crt'
ssl_crl_file = ''
"""

def call(cmd):
    print("Running " + " ".join(cmd))
    subprocess.call(cmd)

def processsqlfile(name, replacements):
    with open(name, 'r') as ip, open(PROCESSED, 'w') as op:
        for l in ip.readlines():
            for key in replacements:
                l = l.replace(key, replacements[key])
            op.write(l)

if __name__ == '__main__':
    print(os.path.dirname(__file__))
    if len(sys.argv) < 4 or (sys.argv[2] == 'series' and len(sys.argv) != 5):
        print("Usage: {} <installbase> [init <wwwpassword> | series <name> <password>]".format(sys.argv[0]))
        sys.exit(-1)

    installbase = sys.argv[1]
    if installbase == "": # Postgresql is installed by the system
        psql    = "psql"
    else:
        bindir  = os.path.join(installbase, "postgresql", "bin")
        dbdir   = os.path.join(installbase, "pgdb")
        logfile = os.path.join(installbase, "pgdb", "postgresql.log")
        hbaconf = os.path.join(installbase, "pgdb", "pg_hba.conf")
        pgconf  = os.path.join(installbase, "pgdb", "postgresql.conf")
        logfile = os.path.join(installbase, "pgdb", "postgresql.log")
        initdb  = os.path.join(bindir, "initdb")
        pg_ctl  = os.path.join(bindir, "pg_ctl")
        psql    = os.path.join(bindir, "psql")
    
    if sys.argv[2] == 'init':
        if installbase != "": # we did not install postgres, its locally running
            call([initdb, "-D", dbdir, "-U", "postgres"])
            with open(hbaconf, 'w') as hba, open(pgconf, 'a') as conf:
                hba.write(HBA)
                conf.write(CONFADDON)
            call([pg_ctl, "-D", dbdir, "-l", logfile, "start"])
            print("waiting for database to start")
            time.sleep(5)
        call([psql, "-U", "postgres", "-c", "CREATE DATABASE scorekeeper"])
        processsqlfile('public.sql', { '<wwwpassword>':sys.argv[3] }) 
        call([psql, "-U", "postgres", "-d", "scorekeeper", "-c", "\i public.sql"])
        os.remove(PROCESSED)

    elif sys.argv[2] == 'series':
        processsqlfile('series.sql', { '<seriesname>':sys.argv[3], '<password>':sys.argv[4] }) 
        subprocess.call([psql, "-U", "postgres", "-d", "scorekeeper", "-c", "\i "+PROCESSED ])
        os.remove(PROCESSED)
