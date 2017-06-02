#!/usr/bin/env python3

import os
import sys

import nwrsc.lib.postgresql as p

basedir = os.path.abspath(os.path.join(os.path.dirname(sys.executable), "../.."))
argc    = len(sys.argv)

if argc == 2:
    p.ensure_database_running(basedir)
    p.ensure_public_schema(sys.argv[1])
elif argc == 3:
    p.ensure_database_running(basedir)
    p.ensure_series_schema(sys.argv[1], sys.argv[2])
else:
    print("Usage: {} (<wwwpassword> | <seriesname> <seriespassword>)")
    sys.exit(-1)

