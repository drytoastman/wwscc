#!/usr/bin/env python

import sys
import os.path
import subprocess


def call(cmd):
    print("Running " + " ".join(cmd))
    subprocess.call(cmd)

if __name__ == '__main__':
    if len(sys.argv) < 3 or (sys.argv[2] == 'series' and len(sys.argv) != 4):
        print("Usage: {} <installbase> [init | series <name>]")
        sys.exit(-1)

    installbase = sys.argv[1]

    bindir  = os.path.join(installbase, "postgresql-9.6.2/bin")
    dbdir   = os.path.join(installbase, "pgdb")
    logfile = os.path.join(installbase, "pgdb/postgresql.log")

    initdb  = os.path.join(bindir, "initdb")
    pg_ctl  = os.path.join(bindir, "pg_ctl")
    psql    = os.path.join(bindir, "psql")
    

    if sys.argv[2] == 'init':
        call([initdb, "-D", dbdir, "-U", "postgres"])
        call([pg_ctl, "-D", dbdir, "-l", logfile, "start"])
        call([psql, "-U", "postgres", "-c", "\i init.sql"])
        call([psql, "-U", "postgres", "-d", "scorekeeper", "-c", "\i public.sql"])

    if sys.argv[2] == 'series':
        name = sys.argv[3]
        fp = open('series.sql', 'r')
        op = open('_tmp.sql', 'w')
        for l in fp.readlines():
            ln = l.replace('<seriesname>', name)
            op.write(ln)
        fp.close()
        op.close()

        subprocess.call([psql, "-U", "postgres", "-d", "scorekeeper", "-c", "\i _tmp.sql"])
        os.remove('_tmp.sql')
