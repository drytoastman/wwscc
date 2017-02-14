#!/usr/bin/env python

import sys
import os.path
import subprocess

# Cygwin python uses cygwin paths
cygbase = "/home/bwilson/root"
bindir  = os.path.join(cygbase, "postgresql-9.6.2/bin")
initdb  = os.path.join(bindir, "initdb")
pg_ctl  = os.path.join(bindir, "pg_ctl")
psql    = os.path.join(bindir, "psql")

# postgres commands use Windows paths
winbase = "/cygwin64/home/bwilson/root"
dbdir   = os.path.join(winbase, "pgdb")
logfile = os.path.join(winbase, "pgdb/postgresql.log")

if __name__ == '__main__':
    if len(sys.argv) < 2 or (sys.argv[1] == 'series' and len(sys.argv) != 3):
        print "Usage: {} [init | series <name>]"
        sys.exit(-1)

    if sys.argv[1] == 'init':
        subprocess.call([initdb, "-D", dbdir, "-U", "postgres"])
        subprocess.call([pg_ctl, "-D", dbdir, "-l", logfile, "start"])
        subprocess.call([psql, "-U", "postgres", "-c", "CREATE USER scorekeeper PASSWORD 'scorekeeper'"])
        subprocess.call([psql, "-U", "postgres", "-c", "CREATE USER wwwuser PASSWORD 'wwwuser'"])
        subprocess.call([psql, "-U", "postgres", "-c", "GRANT  scorekeeper TO wwwuser"])
        subprocess.call([psql, "-U", "postgres", "-c", "CREATE DATABASE scorekeeper WITH OWNER scorekeeper"])
        subprocess.call([psql, "-U", "postgres", "-d", "scorekeeper", "-c", "\i public.sql"])

    if sys.argv[1] == 'series':
        name = sys.argv[2]
        fp = open('series.sql', 'r')
        op = open('_tmp.sql', 'w')
        for l in fp.readlines():
            ln = l.replace('<seriesname>', name)
            op.write(ln)
        fp.close()
        op.close()

        subprocess.call([psql, "-U", "postgres", "-d", "scorekeeper", "-c", "\i _tmp.sql"])
        os.remove('_tmp.sql')
