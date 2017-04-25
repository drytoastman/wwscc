#!/usr/bin/env python

import sys

class AttrWrapper(object):
    def __init__(self, tup, headers):
        for k,v in zip(headers, tup):
            setattr(self, k, v)

def convert(sourcefile, name, password):
    import sqlite3
    import json
    import uuid
    import psycopg2
    import psycopg2.extras

    remapdriver = dict()
    remapcar = dict()
    challengeruns = list()

    remapcar[-1] = None

    old = sqlite3.connect(sourcefile)
    old.row_factory = sqlite3.Row

    psycopg2.extras.register_uuid()
    new = psycopg2.connect(user='postgres', dbname='scorekeeper', cursor_factory=psycopg2.extras.DictCursor)
    cur = new.cursor()

    cur.execute("set search_path=%s,%s", (name, 'public'))

    #DRIVERS, add to global list and remap ids as necessary
    print("drivers")
    for r in old.execute('select * from drivers'):
        d = AttrWrapper(r, r.keys())

        cur.execute("select * from drivers where lower(firstname)=%s and lower(lastname)=%s and lower(email)=%s", 
                    (d.firstname.strip().lower(), d.lastname.strip().lower(), d.email.strip().lower()))
        if cur.rowcount > 0:
            match = cur.fetchone()
            remapdriver[d.id] = match['driverid']
            print('match %s %s %s' % (d.firstname, d.lastname, d.email))
        else:
            newd = dict()
            newd['driverid']   = uuid.uuid1()
            newd['firstname']  = d.firstname.strip()
            newd['lastname']   = d.lastname.strip()
            newd['email']      = d.email.strip()
            newd['username']   = newd['driverid']
            newd['password']   = ""
            newd['membership'] = d.membership and d.membership.strip() or ""
            newd['attr']       = dict()
            for a in ('alias', 'address', 'city', 'state', 'zip', 'phone', 'brag', 'sponsor', 'notes'):
                if hasattr(d, a) and getattr(d, a):
                    newd['attr'][a] = getattr(d, a).strip()
    
            cur.execute("insert into drivers values (%s, %s, %s, %s, %s, %s, %s, %s, now())", 
                (newd['driverid'], newd['firstname'], newd['lastname'], newd['email'], newd['username'], newd['password'], newd['membership'], json.dumps(newd['attr'])))
            remapdriver[d.id] = newd['driverid']


    #INDEXLIST (put into its own index group)
    print("indexes")
    cur.execute("insert into indexlist values ('', 'No Index', 1.000, now())")
    for r in old.execute("select * from indexlist"):
        i = AttrWrapper(r, r.keys())
        cur.execute("insert into indexlist values (%s, %s, %s, now())",     
                    (i.code, i.descrip, i.value))

    #CLASSLIST (map seriesid)
    print("classes")
    for r in old.execute("select * from classlist"):
        c = AttrWrapper(r, r.keys())
        c.usecarflag  = c.usecarflag and True or False
        c.carindexed  = c.carindexed and True or False
        c.eventtrophy = c.eventtrophy and True or False
        c.champtrophy = c.champtrophy and True or False
        c.secondruns  = c.code in ('TOPM', 'ITO2')
        cur.execute("insert into classlist values (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, now())", 
                    (c.code, c.descrip, c.classindex, c.caridxrestrict, c.classmultiplier, c.carindexed, c.usecarflag, c.eventtrophy, c.champtrophy, c.secondruns, c.countedruns))


    #CARS (all the same fields, need to map carid, driverid and seriesid)
    print("cars")
    for r in old.execute("select * from cars"):
        c = AttrWrapper(r, r.keys())
        if c.driverid < 0:
            continue
        newc = dict()
        newc['carid']      = uuid.uuid1()
        newc['driverid']   = remapdriver[c.driverid]
        newc['classcode']  = c.classcode
        newc['indexcode']  = c.indexcode or ''
        newc['number']     = c.number or 999
        newc['useclsmult'] = bool(getattr(c, 'tireindexed', False))
        newc['attr']       = dict()
        for a in ('year', 'make', 'model', 'color'):
            if hasattr(c, a) and getattr(c, a):
                newc['attr'][a] = getattr(c, a)

        cur.execute("insert into cars values (%s, %s, %s, %s, %s, %s, %s, now())", 
            (newc['carid'], newc['driverid'], newc['classcode'], newc['indexcode'], newc['number'], newc['useclsmult'], json.dumps(newc['attr'])))
        remapcar[c.id] = newc['carid']

        
    #EVENTS (all the same fields)
    maxeid = 1
    print("events")
    for r in old.execute("select * from events"):
        e = AttrWrapper(r, r.keys())
        if not e.segments.strip():
            segments = 0
        else:
            segments = len(e.segments.replace(" ", "").split(","))
        newe = dict()
        newe['eventid']     = e.id
        newe['name']        = e.name
        newe['date']        = e.date
        newe['regopened']   = e.regopened
        newe['regclosed']   = e.regclosed
        newe['courses']     = e.courses
        newe['runs']        = e.runs
        newe['countedruns'] = e.countedruns
        newe['segments']    = segments
        newe['perlimit']    = e.perlimit
        newe['sinlimit']    = e.totlimit
        newe['totlimit']    = e.totlimit
        newe['conepen']     = e.conepen
        newe['gatepen']     = e.gatepen
        newe['ispro']       = e.ispro and True or False
        newe['ispractice']  = e.practice and True or False
        newe['attr']        = dict()
        maxeid = max(maxeid, e.id)

        for a in ('location', 'sponsor', 'host', 'chair', 'designer', 'snail', 'cost', 'notes', 'doublespecial'):
            if hasattr(e, a) and getattr(e, a):
                newe['attr'][a] = getattr(e, a)

        cur.execute("insert into events values (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, now())", 
            (newe['eventid'], newe['name'], newe['date'], newe['regopened'], newe['regclosed'], newe['courses'], newe['runs'], newe['countedruns'], newe['segments'],
            newe['perlimit'], newe['sinlimit'], newe['totlimit'], newe['conepen'], newe['gatepen'], newe['ispro'], newe['ispractice'], json.dumps(newe['attr'])))
    # Make sure database sequence is on the same page as us
    cur.execute("ALTER SEQUENCE events_eventid_seq RESTART WITH %s", (maxeid+1,))

    #REGISTERED (map carid)
    print("registered")
    for r in old.execute("select * from registered"):
        oldr = AttrWrapper(r, r.keys())
        if oldr.eventid > 0x0FFFF:
            continue
        cur.execute("insert into registered values (%s, %s, %s, now())", (oldr.eventid, remapcar[oldr.carid], oldr.paid and True or False))


    #CLASSORDER
    print("classorder")
    for r in old.execute("select * from rungroups"):
        oldr = AttrWrapper(r, r.keys())
        cur.execute("insert into classorder values (%s, %s, %s, %s, now())", (oldr.eventid, oldr.classcode, oldr.rungroup, oldr.gorder))


    #RUNORDER 
    print("runorder")
    for r in old.execute("select * from runorder"):
        oldr = AttrWrapper(r, r.keys())
        cur.execute("insert into runorder values (%s, %s, %s, %s, %s, now())", (oldr.eventid, oldr.course, oldr.rungroup, oldr.row, remapcar[oldr.carid]))


    #RUNS (map eventid, carid)
    print("runs")
    for r in old.execute("select * from runs"):
        oldr = AttrWrapper(r, r.keys())
        if (oldr.eventid > 0x0FFFF):
            challengeruns.append(oldr)
            continue
            
        attr = dict()
        if olr.reaction: attr['reaction'] = oldr.reaction
        if old.sixty:    attr['sixty'] = oldr.sixty
        for ii in range(1,6):
            seg = getattr(oldr, 'seg%d'%ii)
            if seg is not None and seg > 0:
                attr['seg%d'%ii] = seg
        cur.execute("insert into runs values (%s, %s, %s, %s, %s, %s, %s, %s, %s, now())",
            (oldr.eventid, remapcar[oldr.carid], oldr.course, oldr.run, oldr.cones, oldr.gates, oldr.raw, oldr.status, json.dumps(attr)))


    #SETTINGS
    print("settings")
    settings = dict()
    for r in old.execute("select name,val from settings"):
        key = r['name']
        val = r['val']
        if key == 'useevents':
            key = 'dropevents'
        cur.execute("insert into settings values (%s, %s, now())", (key, val))

        
    #CHALLENGES (remap challengeid, eventid)
    print("challenges")
    for r in old.execute("select * from challenges"):
        c = AttrWrapper(r, r.keys())

        newc = dict()
        newc['challengeid'] = c.id
        newc['eventid']     = c.eventid
        newc['name']        = c.name
        newc['depth']       = c.depth

        cur.execute("insert into challenges values (%s, %s, %s, %s, now())", (newc['challengeid'], newc['eventid'], newc['name'], newc['depth']))


    #CHALLENGEROUNDS (remap roundid, challengeid, carid)
    print("challengerounds")
    check1 = set()
    maxcid = 1
    for rp in old.execute("select * from challengerounds"):
        r = AttrWrapper(rp, rp.keys())
        cid  = r.challengeid
        c1id = remapcar[r.car1id]
        c2id = remapcar[r.car2id]
        ss = r.swappedstart and True or False
        c1d = r.car1dial or 0.0
        c2d = r.car2dial or 0.0
        check1.add((cid, r.round))
        maxcid = max(maxcid, cid)
        cur.execute("insert into challengerounds values (%s, %s, %s, %s, %s, %s, %s, now())", (cid, r.round, ss, c1id, c1d, c2id, c2d))
    # Make sure database sequence is on the same page as us
    cur.execute("ALTER SEQUENCE challenges_challengeid_seq RESTART WITH %s", (maxcid+1,))


    #CHALLENGERUNS (now in ther own table)
    print("challengeruns")
    for r in challengeruns:
        chid  = r.eventid >> 16
        round = r.eventid & 0x0FFF
        caid  = remapcar[r.carid]
        if caid is not None and (chid, round) in check1:
            cur.execute("insert into challengeruns values (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, now())", (chid, round, caid,
                            r.course, r.reaction, r.sixty, r.raw, r.cones, r.gates, r.status))

    old.close()
    new.commit()
    new.close()

if __name__ == '__main__':
    if len(sys.argv) < 4:
        print("Usage: {} <old db file> <name> <password>".format(sys.argv[0]))
    else:
        convert(sys.argv[1], sys.argv[2], sys.argv[3])

