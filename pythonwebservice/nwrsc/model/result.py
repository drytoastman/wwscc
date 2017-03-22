import logging
from flask import g
from math import ceil
from copy import copy
from collections import defaultdict
from operator import attrgetter
from datetime import date

from .base import AttrBase, BaseEncoder, Entrant
from .classlist import ClassData
from .series import Series
from .settings import Settings
from .simple import Challenge, Event , Run

log = logging.getLogger(__name__)


class PosPoints(object):
    def __init__(self, settingsvalue):
        self.ppoints = list(map(int, settingsvalue.split(',')))
    def get(self, position):
        idx = position - 1
        if idx >= len(self.ppoints):
            return self.ppoints[-1]
        else:
            return self.ppoints[idx]


class Result(object):
    """ 
        Interface into the results table for cached results.  This is the primary source of information
        for the results, json and xml controllers as the data is present even if the series has been
        archived.  If the series is active and the data in the regular tables is more up to date, we 
        regenerate the values in the results table.
    """

    @classmethod
    def getSeriesInfo(cls):
        name = "info"
        if cls._needUpdate(('classlist', 'indexlist', 'events', 'settings'), name):
            cls._updateSeriesInfo(name)
        return SeriesInfo(cls._loadResults(name))

    @classmethod
    def getEventResults(cls, eventid):
        name = "e%d"%eventid
        if cls._needUpdate(('classlist', 'indexlist', 'events', 'cars', 'runs'), name):
            cls._updateEventResults(name, eventid)
        return cls._loadResults(name)

    @classmethod
    def getChallengeResults(cls, challengeid):
        name = "c%d"%challengeid
        if cls._needUpdate(('challengerounds', 'challengeruns'), name):
            cls._updateChallengeResults(name, challengeid)
        ret = dict() # Have to convert back to dict as JSON can't store using ints as keys
        for rnd in cls._loadResults(name):
            ret[rnd['round']] = rnd
        return ret

    @classmethod
    def getChampResults(cls):
        """ returns a ChampClass list object """
        name = "champ"
        if cls._needUpdate(('classlist', 'indexlist', 'events', 'cars', 'runs'), name):
            cls._updateChampResults(name)
        res = cls._loadResults(name)
        for k, v in res.items():
            res[k] = ChampClass(v) # Rewrap the list with ChampClass for template function
        return res

    @classmethod
    def getTopTimesTable(cls, classdata, results, *keys):
        """ Get top times.  Pass in results from outside as in some cases, they are already loaded """
        return cls._loadTopTimesTable(classdata, results, *keys)

    @classmethod
    def applyAnnouncerDetails(cls, settings, eventresults, carid):
        cls._applyAnnouncerDetails(settings, eventresults, carid)


    #####################  Everything below here is for internal use, use the API above ##############

    #### Helpers for basic results operations

    @classmethod
    def _needUpdate(cls, tables, name):
        # check if we can/need to update based on table changes
        if g.seriestype != Series.ACTIVE:
            return False
        with g.db.cursor() as cur:
            cur.execute("select " +
                "(select max(time) from serieslog where tablen in %s) >" +
                "(select modified from results where series=%s and name=%s)", (tables, g.series, name))
            mod = cur.fetchone()[0]
            if mod is None or mod: 
                return True
        return False

    @classmethod
    def _loadResults(cls, name):
        with g.db.cursor() as cur:
            cur.execute("select data from results where series=%s and name=%s", (g.series, name))
            res = cur.fetchone()
            if res is not None:
                return res['data']
            else:
                return dict()

    @classmethod
    def _insertResults(cls, name, data):
        # Get access for modifying series rows, check if we need to insert a default first.
        # Don't upsert as we have to specify LARGE json object twice.
        with g.db.cursor() as cur:
            cur.execute("set role %s", (g.series,))
            cur.execute("insert into results values (%s, %s, '{}', now()) ON CONFLICT (series, name) DO NOTHING", (g.series, name))
            cur.execute("update results set data=%s, modified=now() where series=%s and name=%s", (BaseEncoder().encode(data), g.series, name))
            cur.execute("reset role")
            g.db.commit()


    ### Here is where the actual data generation is done

    @classmethod
    def _updateSeriesInfo(cls, name):
        classdata = ClassData.get()
        data = {
                'events': Event.byDate(),
                'challenges': Challenge.getAll(),
                'classes': list(classdata.classlist.values()),
                'indexes': list(classdata.indexlist.values()),
                'settings': Settings.get()
            }
        cls._insertResults(name, data)


    @classmethod
    def _updateEventResults(cls, name, eventid):
        """
            Creating the cached event result data for the given event.
            The event result data is {<classcode>, [<Entrant>]}.
            Each Entrant is a json object of attributes and a list of lists of Run objects ([course#][run#])
            Each Run object is regular run data with attributes like bestraw, bestnet assigned.
        """
        results = defaultdict(list)
        cptrs   = {}
    
        event     = Event.get(eventid)
        classdata = ClassData.get()
        settings  = Settings.get()
        ppoints   = PosPoints(settings.pospointlist)
    
        with g.db.cursor() as cur:
            # Fetch all of the entrants (driver/car combo), place in class lists, save pointers for quicker access
            cur.execute("select distinct d.firstname,d.lastname,d.membership,c.*,r.rungroup from drivers d " + 
                        "join cars c on c.driverid=d.driverid join runorder r on r.carid=c.carid " +
                        "where r.eventid=%s", (eventid,))
            ii = 0
            for e in [Entrant(**x) for x in cur.fetchall()]:
                e.indexstr = classdata.getIndexStr(e)
                e.indexval = classdata.getEffectiveIndex(e)
                e.runs = [[Run(raw=999.999,pen=999.999,net=999.999,status='DNS') for x in range(event.runs)] for x in range(event.courses)]
                results[e.classcode].append(e)
                ii = ii + 1
                cptrs[e.carid] = e

            # Fetch all of the runs, calc net and assign to the correct entrant
            cur.execute("select * from runs where eventid=%s", (eventid,))
            lastcourse = 0
            for r in [Run(**x) for x in cur.fetchall()]:
                match = cptrs[r.carid]
                match.runs[r.course-1][r.run - 1] = r
                penalty = (r.cones * event.conepen) + (r.gates * event.gatepen)
                if r.status != "OK":
                    r.pen = 999.999
                    r.net = 999.999
                elif settings.indexafterpenalties:
                    r.pen = r.raw + penalty
                    r.net = r.pen * match.indexval
                else:
                    r.pen = r.raw + penalty
                    r.net = (r.raw*match.indexval) + penalty
        
            # For every entrant, calculate their best runs (raw,net,allraw,allnet) and event sum(net)
            for e in cptrs.values():
                e.net = 0      # Best counted net overall time
                e.pen = 0      # Best counted unindexed overall time (includes penalties)
                e.netall = 0   # Best net of all runs (same as net when counted not active)
                e.penall = 0   # Best unindexed of all runs (same as pen when counted not active)
                if event.ispro:
                    e.dialraw = 0  # Best raw times (OK status) used for dialin calculations
                counted = min(classdata.getCountedRuns(e.classcode), event.getCountedRuns())

                # Creates an attribute for each entry in the list with the value of index+1 """
                def marklist(lst, label):
                    for ii, entry in enumerate(lst):
                        setattr(entry, label, ii+1)

                # When sorting raw, we need to ignore non-OK status runs
                def rawgetter(obj):
                    if obj.status == "OK":
                        return obj.raw
                    return 999.999
        
                for course in range(event.courses):
                    bestrawall = sorted(e.runs[course], key=rawgetter)
                    bestnetall = sorted(e.runs[course], key=attrgetter('net'))
                    bestraw    = sorted(e.runs[course][0:counted], key=rawgetter)
                    bestnet    = sorted(e.runs[course][0:counted], key=attrgetter('net'))
                    marklist (bestrawall, 'arorder')
                    marklist (bestnetall, 'anorder')
                    marklist (bestraw, 'rorder')
                    marklist (bestnet, 'norder')
                    e.netall += bestnetall[0].net
                    e.penall += bestnetall[0].pen
                    e.net += bestnet[0].net
                    e.pen += bestnet[0].pen
                    if event.ispro:
                        e.dialraw += bestraw[0].raw

            # Now for each class we can sort and update position, trophy, points(both types)
            for clas in results:
                res = results[clas]
                res.sort(key=attrgetter('net'))
                trophydepth = ceil(len(res) / 3.0)
                eventtrophy = classdata.classlist[clas].eventtrophy
                for ii, e in enumerate(res):
                    e.position   = ii+1
                    e.trophy     = eventtrophy and (ii < trophydepth)
                    e.pospoints  = ppoints.get(e.position)
                    e.diffpoints = res[0].net*100/e.net;
                    e.points     = settings.usepospoints and e.pospoints or e.diffpoints

                    # Dialins for pros
                    if event.ispro:
                        e.bonusdial = e.dialraw / 2.0
                        if ii == 0:
                            e.prodiff = len(res) > 1 and e.net - res[1].net or 0.0
                            e.prodial = e.bonusdial
                        else:
                            e.prodiff = e.net - res[0].net
                            e.prodial = res[0].dialraw * res[0].indexval / e.indexval / 2.0

    
        cls._insertResults(name, results)


    @classmethod
    def _applyAnnouncerDetails(cls, settings, eventresults, carid):
        # Calculate things for the announcer/info displays
        ppoints = PosPoints(settings.pospointlist)
        for clscode, entrants in eventresults.items():
            for e in entrants:
                if e['carid'] != carid:
                    continue

                lastcourse = 1
                lastrun = 4
                lastentry = e['runs'][lastcourse-1][lastrun-1] 
                ann = dict()

                def norderrun(n):
                    # Find the run with norder == n 
                    return next((x for x in e['runs'][lastcourse-1] if x['norder'] == n), None)

                if lastentry['norder'] == 1:
                    # New best net, note net improvement, mark the old info with sum - lastrun + prevrun
                    prevbest = norderrun(2) 
                    if prevbest:
                        ann['netimp'] = lastentry['net'] - prevbest['net']
                        ann['oldnet'] = e['net'] - lastentry['net'] + prevbest['net']

                if lastentry['rorder'] == 1:
                    # New best raw, note raw improvement over previous run
                    # This can be n=2 for overall improvement, or n=1 if only raw, not net improvement
                    prevbest = norderrun(lastentry['norder'] == 1 and 2 or 1)
                    if prevbest:
                        ann['rawimp'] = lastentry['raw'] - prevbest['raw']

                if lastentry['cones'] != 0 or lastentry['gates'] != 0:
                    # add data for run without penalties
                    ann['potnet'] = e['net'] - lastentry['net'] + (lastentry['raw'] * e['indexval'])

                # Figure out points changes
                sumlist = [x['net'] for x in entrants]
                sumlist.remove(e['net'])

                if 'oldnet' in ann:
                    sumlist.append(ann['oldnet'])
                    sumlist.sort()
                    position = sumlist.index(ann['oldnet'])+1
                    ann['oldpospoints']  = ppoints.get(position)
                    ann['olddiffpoints'] = sumlist[0]*100/ann['oldnet']
                    sumlist.remove(ann['oldnet'])

                if 'potnet' in ann:
                    sumlist.append(ann['potnet'])
                    sumlist.sort()
                    position = sumlist.index(ann['potnet'])+1
                    ann['potentialpospoints']  = ppoints.get(position)
                    ann['potentialdiffpoints'] = sumlist[0]*100/ann['potnet']

                e['ann'] = ann
                return # we are done, break out and return from here


    @classmethod
    def _updateChallengeResults(cls, name, challengeid):
        rounds = dict()
        with g.db.cursor() as cur:
            getrounds = "SELECT x.*, " \
                    "d1.firstname as e1fn, d1.lastname as e1ln, c1.classcode as e1cc, c1.indexcode as e1ic, " \
                    "d2.firstname as e2fn, d2.lastname as e2ln, c2.classcode as e2cc, c2.indexcode as e2ic  " \
                    "FROM challengerounds x " \
                    "LEFT JOIN cars c1 ON x.car1id=c1.carid LEFT JOIN drivers d1 ON c1.driverid=d1.driverid " \
                    "LEFT JOIN cars c2 ON x.car2id=c2.carid LEFT JOIN drivers d2 ON c2.driverid=d2.driverid " \
                    "WHERE challengeid=%s "
    
            getruns = "select * from challengeruns where challengeid=%s "
    
            cur.execute(getrounds, (challengeid,))
            for obj in [AttrBase(**x) for x in cur.fetchall()]:
                # We organize ChallengeRound in a topological structure so we do custom setting here
                rnd = AttrBase()
                rnd.challengeid  = obj.challengeid
                rnd.round        = obj.round
                rnd.winner       = 0 
                rnd.detail       = ""
                rnd.e1           = AttrBase()
                rnd.e1.carid     = obj.car1id
                rnd.e1.dial      = obj.car1dial
                rnd.e1.newdial   = obj.car1dial
                rnd.e1.firstname = obj.e1fn or ""
                rnd.e1.lastname  = obj.e1ln or ""
                rnd.e1.classcode = obj.e1cc
                rnd.e1.indexcode = obj.e1ic
                rnd.e1.left      = None
                rnd.e1.right     = None
                rnd.e2           = AttrBase()
                rnd.e2.carid     = obj.car2id
                rnd.e2.dial      = obj.car2dial
                rnd.e2.newdial   = obj.car2dial
                rnd.e2.firstname = obj.e2fn or ""
                rnd.e2.lastname  = obj.e2ln or ""
                rnd.e2.classcode = obj.e2cc
                rnd.e2.indexcode = obj.e2ic
                rnd.e2.left      = None
                rnd.e2.right     = None
                rounds[rnd.round] = rnd
    
            cur.execute(getruns, (challengeid,))
            for run in [AttrBase(**x) for x in cur.fetchall()]:
                rnd = rounds[run.round]
                run.net = run.status == "OK" and run.raw + (run.cones * 2) + (run.gates * 10) or 999.999
                if   rnd.e1.carid == run.carid:
                    setattr(rnd.e1, run.course==1 and 'left' or 'right', run)
                elif rnd.e2.carid == run.carid:
                    setattr(rnd.e2, run.course==1 and 'left' or 'right', run)

            for rnd in rounds.values():
                #(rnd.winner, rnd.detail) = rnd.compute()
                tl = rnd.e1.left
                tr = rnd.e1.right
                bl = rnd.e2.left
                br = rnd.e2.right

                # Missing an entrant or no run data
                if rnd.e1.carid == 0 or rnd.e2.carid == 0:
                    rnd.detail = 'No matchup yet'
                    continue
                if tl is None and tr is None:
                    rnd.detail = 'No runs taken'
                    continue

                # Some runs taken but there was non-OK status creating a default win
                if tl and tl.status != "OK":  rnd.winner = 2; rnd.detail = rnd.e2.firstname+" wins by default"; continue
                if br and br.status != "OK":  rnd.winner = 1; rnd.detail = rnd.e1.firstname+" wins by default"; continue
                if tr and tr.status != "OK":  rnd.winner = 2; rnd.detail = rnd.e2.firstname+" wins by default"; continue
                if bl and bl.status != "OK":  rnd.winner = 1; rnd.detail = rnd.e1.firstname+" wins by default"; continue

                # Some runs so present a half way status
                if not tl or not tr: 
                    if tl and br:   hr = (tl.net - rnd.e1.dial) - (br.net - rnd.e2.dial)
                    elif tr and bl: hr = (tr.net - rnd.e1.dial) - (bl.net - rnd.e2.dial)
                    else:           hr = 0

                    if hr > 0:   rnd.detail = '%s leads by %0.3f' % (rnd.e2.firstname, hr)
                    elif hr < 0: rnd.detail = '%s leads by %0.3f' % (rnd.e1.firstname, hr)
                    else:        rnd.detail = 'Tied at the Half'

                    continue

                # We have all the data, calculate who won
                rnd.e1.result = rnd.e1.left.net + rnd.e1.right.net - (2*rnd.e1.dial)
                rnd.e2.result = rnd.e2.left.net + rnd.e2.right.net - (2*rnd.e2.dial)
                if rnd.e1.result < 0: rnd.e1.newdial = rnd.e1.dial + (rnd.e1.result/2 * 1.5)
                if rnd.e2.result < 0: rnd.e2.newdial = rnd.e2.dial + (rnd.e2.result/2 * 1.5)

                if rnd.e1.result < rnd.e2.result: 
                    rnd.winner = 1
                    rnd.detail = "%s wins by %0.3f" % (rnd.e1.firstname, (rnd.e2.result - rnd.e1.result))
                elif rnd.e2.result < rnd.e1.result:
                    rnd.winner = 2
                    rnd.detail = "%s wins by %0.3f" % (rnd.e2.firstname, (rnd.e1.result - rnd.e2.result))
                else:
                    rnd.detail = 'Tied?'
    
        cls._insertResults(name, list(rounds.values()))


    @classmethod
    def _updateChampResults(cls, name):
        """
            Create the cached result for champ results.  
            If justeventid is None, we load all event results and create the champ results.
            If justeventid is not None, we use the previous champ results and just update the event
                (saves loading/parsing all events again)
            Returns a dict of ChampClass objects
        """
        settings  = Settings.get()
        classdata = ClassData.get()
        events = Event.byDate()

        completed = 0

        # Interm storage while we distribute result data by driverid
        store = defaultdict(lambda : defaultdict(ChampEntrant))
        for event in events:
            if event.ispractice: continue
            if date.today() >= event.date:
                completed += 1

            eventresults = cls.getEventResults(event.eventid)
            for classcode in eventresults:
                if not classdata.classlist[classcode].champtrophy:  # class doesn't get champ trophies, ignore
                    continue
                classmap = store[classcode]
                for entrant in eventresults[classcode]:
                    classmap[entrant['driverid']].addResults(event, entrant)

        todrop   = settings.dropevents
        bestof   = max(todrop, completed - todrop)
        sortkeys = ['points']
        sortkeys.extend([x for x in map(str.strip, settings.champsorting.split(',')) if x in ChampEntrant.AvailableSubKeys])

        # Final storage where results are an ordered list rather than map
        ret = defaultdict(ChampClass)
        for classcode, classmap in store.items():
            for entrant in classmap.values():
                entrant.points.calc(bestof)
                ret[classcode].append(entrant)
            ret[classcode].sort(key=attrgetter(*sortkeys), reverse=True)
            ii = 1
            for e in ret[classcode]:
                if e.eventcount < settings.minevents:
                    e.position = ''
                else:
                    e.position = ii
                    ii += 1

        cls._insertResults(name, ret)

             
    @classmethod
    def _loadTopTimesTable(cls, classdata, results, *keys):
        """
            Generate lists on demand as there are many iterations.  Returns a TopTimesTable class
            that wraps all the TopTimesLists together.
            For each key passed in, the following values may be set:
                indexed = True for indexed times, False for penalized but raw times
                counted = True for to only included 'counted' runs and non-second run classes
                course  = 0 for combined course total, >0 for specific course
               Extra fields that have standard defaults we stick with:
                title   = A string to override the list title with
                col     = A list of column names for the table
                fields  = The fields to match to each column
        """
        lists = list()
        for key in keys:
            indexed = key.get('indexed', True)
            counted = key.get('counted', True)
            course  = key.get('course', 0)
            title   = key.get('title', None)
            cols    = key.get('cols', None)
            fields  = key.get('fields', None)

            if title is None:
                title  = "Top {}Times ({} Runs)".format(indexed and "Indexed " or "", counted and "Counted" or "All")
                if course > 0: title += " Course {}".format(course)

            if cols is None:   cols   = ['Name', 'Class',     'Index',    '',         'Time']
            if fields is None: fields = ['name', 'classcode', 'indexstr', 'indexval', 'time']

            ttl = TopTimesList(title, cols, fields)
            for cls in results:
                if classdata.classlist[cls].secondruns and counted:
                    continue

                for e in results[cls]:
                    if course > 0:
                        for r in e['runs'][course-1]:
                            if (counted and r['norder'] == 1) or (not counted and r['anorder'] == 1):
                                time = indexed and r['net'] or r['pen']
                                break
                    else:
                        if counted:
                            time = indexed and e['net'] or e['pen']
                        else:
                            time = indexed and e['netall'] or e['penall']

                    ttl.append(TopTimeEntry(fields,
                        name="{} {}".format(e['firstname'], e['lastname']),
                        classcode = e['classcode'],
                        indexstr  = e['indexstr'],
                        indexval  = e['indexval'],
                        time      = time
                    ))

            # Sort and set 'pos' attribute, then add to the mass table
            ttl.sort(key=attrgetter('time'))
            lists.append(ttl)

        return TopTimesTable(*lists)


###################################################################################


class SeriesInfo(dict):
    """
        We wrap the returned JSON series info data in this for easier access 
        and returning of core model objects rather than a raw dict
    """
    def __init__(self, obj):
        self.update(obj)

    def getClassData(self):
        return ClassData(self['classes'], self['indexes'])

    def getSettings(self):
        return Settings(self['settings'])

    def getEvent(self, eventid):
        for e in self['events']:
            if e['eventid'] == eventid:
                return Event(**e)
        return None

    def getChallengesForEvent(self, eventid):
        return [Challenge(**c) for c in self['challenges'] if c['eventid'] == eventid]

    def getChallenge(self, challengeid):
        for c in self['challenges']:
            if c['challengeid'] == challengeid:
                return Challenge(**c)
        return None


class TopTimesList(list):
    """ A list of top times along with the title, column and field info """
    def __init__(self, title, cols, fields):
        self.title = title
        self.cols = cols
        self.fields = fields
        

class TopTimeEntry(object):
    """ A row entry in the TopTimesList """
    def __init__(self, fields, **kwargs):
        self._fields = fields
        self.__dict__.update(kwargs)

    def __iter__(self):
        """ return a set of attributes as determined by original fields """
        for f in self._fields:
            yield getattr(self, f, "missing")

    def __repr__(self):
        return "{}, {}".format(self._fields, self.__dict__)


class TopTimesRow(list):
    pass

class TopTimesTable(object):
    """ We need to zip our lists together ourselves (can't do it in template anymore) so we create our Table and Rows here """
    def __init__(self, *lists):
        self.titles   = list()
        self.colcount = list()
        self.cols     = list()
        self.fields   = list()
        self.rows     = list()

        for ttl in lists:
            self.addList(ttl)

    def addList(self, ttl):
        if len(ttl.cols) != len(ttl.fields):
            raise Exception('Top times columns and field arrays are not equals in size ({}, {})'.format(len(ttl.cols), len(ttl.fields)))

        self.titles.append(ttl.title)
        self.colcount.append(len(ttl.cols))
        self.cols.append(ttl.cols)
        self.fields.append(ttl.fields)

        if len(self.rows) < len(ttl):
            self.rows.extend([TopTimesRow() for x in range(len(ttl) - len(self.rows))])

        for ii in range(len(ttl)):
            self.rows[ii].append(ttl[ii])


class PointStorage(AttrBase):

    def __init__(self):
        self.events = {}
        self.total = 0
        self.drop = []
        self.usingbest = 0
        AttrBase.__init__(self)

    def feedFilter(self, k, v):
        if k == 'usingbest': return None
        return v

    def get(self, eventid):
        return self.events.get(eventid, None)

    def set(self, eventid, points):
        self.events[eventid] = points

    def theory(self, eventid, points):
        save = self.events[eventid]
        self.events[eventid] = points
        self.calc(self.usingbest)
        ret = self.total
        self.events[eventid] = save
        self.calc(self.usingbest)
        return ret
        
    def calc(self, bestof):
        self.total = 0
        self.drop = []
        self.usingbest = bestof
        for ii, points in enumerate(sorted(self.events.items(), key=lambda x:x[1], reverse=True)):
            if ii < bestof:
                self.total += points[1]  # Add to total points
            else:
                self.drop.append(points[0])  # Otherwise this is a drop event, mark eventid

    # provides the comparison for sorting
    def __eq__(self, other):
        return self.total == other.total

    def __lt__(self, other):
        return self.total < other.total


class ChampEntrant(AttrBase):

    AvailableSubKeys = ['firsts', 'seconds', 'thirds', 'fourths', 'attended']

    def __init__(self):
        self.points       = PointStorage()
        self.tiebreakers  = [0]*4
        self.eventcount   = 0
        AttrBase.__init__(self)

    def __getattr__(self, key):
        """ Implement getattr to match firsts, seconds, thirds, etc. """
        try:
            # 100 - turns ordering into reverse
            if key == 'attended': return 100 - self.eventcount
            return 100 - self.tiebreakers[{ 'firsts':0, 'seconds':1, 'thirds':2, 'fourths':3 }[key]]
        except:
            raise AttributeError("No known key %s" % key)

    def addResults(self, event, entry): 
        self.firstname = entry['firstname']
        self.lastname  = entry['lastname']
        self.driverid  = entry['driverid']
        idx = entry['position']-1
        if idx < len(self.tiebreakers):
            self.tiebreakers[idx] += 1
        self.eventcount += 1
        self.points.set("d-%s"%event.date, entry['points'])

    def __repr__(self):
        return "%s %s: %s" % (self.firstname, self.lastname, self.points.total)

class ChampClass(list):
    @property
    def entries(self):
        return sum([e['eventcount'] for e in self])

