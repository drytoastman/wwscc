#!/usr/bin/env python3

import os
import sys

import nwrsc.lib.postgresql as p

westartedpostgres = False
basedir = os.path.abspath(os.path.join(os.path.dirname(sys.executable), "../.."))
argc = len(sys.argv)

if argc == 1:
    westartedpostgres = p.ensure_database_created(basedir)
    p.ensure_public_schema()
elif argc == 3:
    westartedpostgres = p.ensure_database_created(basedir)
    p.ensure_series_schema(sys.argv[1], sys.argv[2])
else:
    print("Usage: {} [<seriesname> <seriespassword>]")
    sys.exit(-1)

if westartedpostgres:
    p.ensure_postgresql_stopped(basedir)

