#!/usr/bin/env python

import sys
import os.path
import subprocess

PROCESSED = "processed.sql"

def call(cmd):
    print("Running " + " ".join(cmd))
    subprocess.call(cmd)

def processfile(name, replacements):
    ip = open(name, 'r')
    op = open(PROCESSED, 'w')
    for l in ip.readlines():
        for key in replacements:
            l = l.replace(key, replacements[key])
        op.write(l)
    ip.close()
    op.close()

if __name__ == '__main__':
    if len(sys.argv) < 4 or (sys.argv[2] == 'series' and len(sys.argv) != 5):
        print("Usage: {} <installbase> [init <wwwpassword> | series <name> <password>]".format(sys.argv[0]))
        sys.exit(-1)

    installbase = sys.argv[1]
    if installbase == "":
        psql    = "psql"
    else:
        bindir  = os.path.join(installbase, "postgresql-9.6.2/bin")
        dbdir   = os.path.join(installbase, "pgdb")
        logfile = os.path.join(installbase, "pgdb/postgresql.log")
        initdb  = os.path.join(bindir, "initdb")
        pg_ctl  = os.path.join(bindir, "pg_ctl")
        psql    = os.path.join(bindir, "psql")
    
    if sys.argv[2] == 'init':
        if installbase != "": # we did not install postgres, its locally running
            call([initdb, "-D", dbdir, "-U", "postgres"])
            call([pg_ctl, "-D", dbdir, "-l", logfile, "start"])
        call([psql, "-U", "postgres", "-c", "CREATE DATABASE scorekeeper"])
        processfile('public.sql', { '<wwwpassword>':sys.argv[3] }) 
        call([psql, "-U", "postgres", "-d", "scorekeeper", "-c", "\i public.sql"])
        os.remove(PROCESSED)

    elif sys.argv[2] == 'series':
        processfile('series.sql', { '<seriesname>':sys.argv[3], '<password>':sys.argv[4] }) 
        subprocess.call([psql, "-U", "postgres", "-d", "scorekeeper", "-c", "\i "+PROCESSED ])
        os.remove(PROCESSED)
