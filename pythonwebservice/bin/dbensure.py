#!/usr/bin/env python3

import os
import sys

import nwrsc.lib.postgresql as p

westartedpostgres = False
basedir = os.path.abspath(os.path.join(os.path.dirname(sys.executable), "../.."))
connkeys = { 'host':'127.0.0.1', 'port':5432, 'user':'superu' }
argc = len(sys.argv)

if argc == 1:
    westartedpostgres = ensure_postgresql_running(basedir)
    p.ensure_database_created(connkeys)
    p.ensure_public_schema(connkeys)
elif argc == 3:
    westartedpostgres = ensure_postgresql_running(basedir)
    p.ensure_database_created(connkeys)
    p.ensure_series_schema(connkeys, sys.argv[1], sys.argv[2])
else:
    print("Usage: {} [<seriesname> <seriespassword>]")
    sys.exit(-1)

if westartedpostgres:
    p.ensure_postgresql_stopped(basedir)

