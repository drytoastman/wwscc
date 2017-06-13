#!/usr/bin/env python3

import sys
import time
import os.path
import pkg_resources
import psycopg2
import subprocess

SUPERU  = "superuser"

HBA = """
#TYPE     DATABASE  USER                 ADDRESS         METHOD
host      all       superuser,localuser  127.0.0.1/32    trust    # No password for local access
host      all       superuser,localuser  0.0.0.0/0       reject   # no super or local user off site
hostnossl all       all                  0.0.0.0/0       reject   # force SSL for non-localhost connections
host      all       +driversaccess       0.0.0.0/0       password # allow any other logins with password
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

def ensure_postgresql_running(basedir):
    bindir  = os.path.join(basedir, "postgresql", "bin")
    dbdir   = os.path.join(basedir, "database")
    hbaconf = os.path.join(dbdir,   "pg_hba.conf")
    pgconf  = os.path.join(dbdir,   "postgresql.conf")
    initdb  = os.path.join(bindir,  "initdb")
    pg_ctl  = os.path.join(bindir,  "pg_ctl")

    if not os.path.exists(pgconf):
        print("Initializing database directory")
        subprocess.call([initdb, "-D", dbdir, "-U", SUPERU])
        with open(hbaconf, 'w') as hba, open(pgconf, 'a') as conf:
            hba.write(HBA)
            conf.write(CONFADDON)

    if subprocess.call([pg_ctl, "-D", dbdir, "status"]):
        print("Starting database")
        subprocess.call([pg_ctl, "-D", dbdir, "-w", "start"])
        return True # We started the database

    return False # Someone else started the database


def ensure_postgresql_stopped(basedir):
    dbdir   = os.path.join(basedir, "database")
    pg_ctl  = os.path.join(basedir, "postgresql",  "bin", "pg_ctl")
    if not subprocess.call([pg_ctl, "-D", dbdir, "status"]):
        print("Stopping database")
        subprocess.call([pg_ctl, "-D", dbdir, "-w", "stop"])


#############################


def check_password(user, password):
    try:
        pg = psycopg2.connect(host='127.0.0.1', port=54329, user=user, password=password, dbname='scorekeeper')
        pg.close()
        return True
    except Exception:
        return False


def ensure_database_created(basedir):
    ret = ensure_postgresql_running(basedir)
    pg = psycopg2.connect(host='127.0.0.1', port=54329, user=SUPERU, dbname='postgres')
    pg.autocommit = True
    pgc = pg.cursor()
    pgc.execute("SELECT datname FROM pg_database WHERE datname='scorekeeper'")
    if pgc.rowcount == 0:
        print("Creating scorekeeper database")
        pgc.execute("CREATE DATABASE scorekeeper")
        pg.commit()
    pg.close()
    return ret


def ensure_public_schema():
    db = psycopg2.connect(host='127.0.0.1', port=54329, user=SUPERU, dbname='scorekeeper')
    dbc = db.cursor()
    dbc.execute("SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename='drivers'")
    if dbc.rowcount == 0:
        print("Creating top level drivers table")
        dbc.execute(processsqlfile('public.sql', {}))
        db.commit()
    db.close()


def ensure_series_schema(seriesname, seriespass):
    ensure_public_schema()
    db = psycopg2.connect(host='127.0.0.1', port=54329, user=SUPERU, dbname='scorekeeper')
    dbc = db.cursor()
    dbc.execute("SELECT tablename FROM pg_tables WHERE schemaname=%s AND tablename='runs'", (seriesname,))
    if dbc.rowcount == 0:
        print("Creating series tables")
        dbc.execute(processsqlfile('series.sql', { '<seriesname>':seriesname, '<password>':seriespass }))
        db.commit()
    db.close()


def processsqlfile(name, replacements):
    ret = []
    with pkg_resources.resource_stream('nwrsc', 'model/'+name) as ip:
        for l in ip.readlines():
            l = l.decode('utf-8')
            for key in replacements:
                l = l.replace(key, replacements[key])
            ret.append(l)
    return '\n'.join(ret)

