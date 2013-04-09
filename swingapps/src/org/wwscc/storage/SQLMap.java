/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2009 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author bwilson
 */
public class SQLMap
{
	static final private HashMap<String,String> sql = new HashMap<String,String>();
	static public String get(String key) { return sql.get(key); }
	static public Set<String> keys() { return sql.keySet(); }
	
	static
	{
		sql.put("DELETEANNOUNCERDATA", "delete from announcer where eventid=? and carid=?");
		sql.put("DELETECAR", "delete from cars where id=?");
		sql.put("DELETECLASSRESULTS", "delete from eventresults where classcode=? and eventid=?");
		sql.put("DELETECHALLENGE", "delete from challenges where id=?");
		sql.put("DELETEDRIVER", "delete from drivers where id=?");
		sql.put("DELETEEXTRA", "delete from driverextra where driverid=?");
		sql.put("DELETERUN", "delete from runs where id=?");
		sql.put("DELETECLASSGROUPMAPPING", "delete from rungroups where eventid=?");
		sql.put("DELETERUNORDER", "delete from runorder where eventid=? and course=? and rungroup=?");
		sql.put("DELETERUNSBYCOURSE", "delete from runs where carid=? AND course=? AND eventid=?");

		sql.put("GETALLDRIVERS", "select * from drivers");
		sql.put("GETALLEXTRA", "select * from driverextra");
		sql.put("GETALLFIELDS", "select * from driverfields");
		sql.put("GETALLCARS", "select * from cars");
		sql.put("GETALLRUNS", "select * from runs");
		sql.put("GETANNOUNCERDATABYENTRANT", "select * from announcer where eventid=? and carid=?");
		sql.put("GETANYRUNS", "select count(id) as count from runs where carid=?");
		sql.put("GETCARIDSBYCHALLENGE", "select car1id,car2id from challengerounds where challengeid=?");
		sql.put("GETCARIDSFORCOURSE", "select carid from runorder where eventid=? AND course=?");
		sql.put("GETCARIDSFORGROUP", "select carid from runorder where eventid=? AND course=? AND rungroup=? order by row");
		sql.put("GETCHALLENGESFOREVENT", "select * from challenges where eventid = ?");
		sql.put("GETCHANGES", "select * from changes");
		sql.put("GETCLASSES", "select * from classlist");
		sql.put("GETCLASSRESULTS", "select r.carid as carid,SUM(r.net) as sum, COUNT(r.net) as coursecnt from runs as r, cars as c " +
					"where r.norder=1 and r.carid=c.id and c.classcode=? and r.eventid=? " +
					"group by r.carid order by coursecnt DESC,sum");
		sql.put("GETDIALINS", "select d.firstname as firstname, d.lastname as lastname, d.alias as alias, c.classcode as classcode, " +
					"c.indexcode as indexcode, c.tireindexed as tireindexed, c.id as carid, SUM(r.raw) as myraw, f.position as position, f.sum as mynet " +
					"from runs as r, cars as c, drivers as d, eventresults as f " +
					"where r.carid=c.id and c.driverid=d.id and r.eventid=? and r.rorder=1 and f.eventid=? and f.carid=c.id " +
					"group by d.id order by position");
		sql.put("GETDRIVER", "select * from drivers where id=?");
		sql.put("GETDRIVERSBY", "select * from drivers where firstname like ? and lastname like ? order by firstname,lastname");
		sql.put("GETDRIVERBYMEMBERSHIP", "select * from drivers where membership like ?");
		sql.put("GETDRIVERSBYFIRST", "select * from drivers where firstname like ? order by firstname,lastname");
		sql.put("GETDRIVERSBYLAST", "select * from drivers where lastname like ? order by firstname,lastname");
		sql.put("GETEVENTENTRANTS", "select distinct d.firstname as firstname,d.lastname as lastname,c.* from runs as r, cars as c, drivers as d " +
						"where r.carid=c.id AND c.driverid=d.id and r.eventid=?");
		sql.put("GETEVENTRESULTSBYCLASS", "select d.firstname,d.lastname,c.indexcode,c.tireindexed,r.* " +
					"from eventresults as r, cars as c, drivers as d " +
					"where r.carid=c.id and c.driverid=d.id and r.classcode=? and r.eventid=? " +
					"order by position");
		sql.put("GETEVENTS", "select * from events order by date");
		sql.put("GETEXTRA", "select name,value from driverextra where driverid=?");
		sql.put("GETINDEXES", "select * from indexlist");
		sql.put("GETREGISTEREDENTRANTS", "select distinct d.firstname as firstname,d.lastname as lastname,c.* from registered as x, cars as c, drivers as d " +
						"where x.carid=c.id AND c.driverid=d.id and x.eventid=?");
		sql.put("GETREGISTEREDCARS", "select c.* from registered as x, cars as c, drivers as d " +
						"where x.carid=c.id AND c.driverid=d.id and x.eventid=? and d.id=?");
		sql.put("GETROUNDSFORCHALLENGE", "select * from challengerounds where challengeid=?");
		sql.put("GETRUNORDERENTRANTS", "select d.firstname,d.lastname,c.* from runorder as r, cars as c, drivers as d " +
						"where r.carid=c.id AND c.driverid=d.id and r.eventid=? AND r.course=? AND r.rungroup=? " +
						"order by r.row");

		sql.put("GETRUNORDERROWS", "select row from runorder where eventid=? AND course=? AND carid=?");
		sql.put("GETRUNORDERROWSCURRENT", "select row from runorder where eventid=? AND course=? AND rungroup=? AND carid=?");
		sql.put("GETRUNCOUNT", "select count(run) as count from runs where carid=? and eventid=? and course=?");
		sql.put("GETRUNGROUPMAPPING", "select classcode from rungroups where eventid=? and rungroup=? order by gorder");
		sql.put("GETRUNSBYCARID", "select * from runs where carid=? and eventid=? and course=?");
		sql.put("GETRUNSBYGROUP", "select * from runs where eventid=? and course=? and carid in " +
						"(select carid from runorder where eventid=? AND course=? AND rungroup=?)");
		sql.put("GETRUNSFORCHALLENGE", "select * from runs where (eventid>>16)=?");
		sql.put("GETSETTING", "select val from settings where name=?");

		sql.put("INSERTBLANKCHALLENGEROUND", "insert into challengerounds (challengeid,round,swappedstart,car1id,car2id) values (?,?,?,?,?)");
		sql.put("INSERTCAR", "insert or ignore into cars ("+AUTO.getCarVarStr()+") values ("+AUTO.getCarArgStr()+")");
		sql.put("INSERTCHALLENGE", "insert into challenges (eventid, name, depth) values (?,?,?)");
		sql.put("INSERTCLASSRESULTS", "insert into eventresults ("+AUTO.getEventResultVarStr()+") values ("+AUTO.getEventResultArgStr()+")");
		sql.put("INSERTDRIVER", "insert or ignore into drivers ("+AUTO.getDriverVarStr()+") values (" +AUTO.getDriverArgStr()+")");
		sql.put("INSERTEXTRA", "insert into driverextra (driverid, name, value) values (?,?,?)");
		sql.put("INSERTRUN", "insert into runs ("+AUTO.getRunVarStr()+") values ("+AUTO.getRunArgStr()+")");
		sql.put("INSERTCLASSGROUPMAPPING", "insert into rungroups (eventid, classcode, rungroup) values (?,?,?)");
		sql.put("INSERTRUNORDER", "insert into runorder values (NULL, ?,?,?,?,?)");
		sql.put("ISREGISTERED", "select id from registered where carid=? and eventid=?");

		sql.put("LOADDRIVERCARS", "select * from cars where driverid = ? order by classcode, number");
		sql.put("LOADENTRANT", "select d.firstname,d.lastname,c.* from cars as c, drivers as d " +
						"where c.driverid=d.id and c.id=?");
		sql.put("LOADRUNORDER", "select carid from runorder where eventid=? and course=? and rungroup=? order by row");

		sql.put("REGISTERCAR", "insert or ignore into registered (eventid, carid) values (?,?)");
		sql.put("REPLACEANNOUNCERDATA", "insert or replace into announcer ("+AUTO.getAnnouncerDataVarStr()+") values ("+AUTO.getAnnouncerDataArgStr()+")");

		sql.put("SWAPRUNORDER", "update runorder set carid=? where eventid=? and course=? and rungroup=? and carid=?");
		sql.put("SWAPRUNS", "update runs set carid=? where eventid=? and course=? and carid=?");

		sql.put("TRACK", "insert into changes (type, args) values (?,?)");
		sql.put("TRACKCLEAR", "delete from changes");
		sql.put("TRACKCOUNT", "select count(*) from changes");
		
		sql.put("UNREGISTERCAR", "delete from registered where eventid=? and carid=?");
		sql.put("UPDATEBOOLEANSETTING", "update settings set val=? where name=?");
		sql.put("UPDATECAR", "update cars set "+ AUTO.getCarSetStr() + " where id=?");
		sql.put("UPDATECHALLENGE", "update challenges set "+ AUTO.getChallengeSetStr() + " where id=?");
		sql.put("UPDATECHALLENGEROUND", "update challengerounds set challengeid=?,round=?,swappedstart=?,"+
					"car1id=?,car1dial=?,car1result=?,car1newdial=?,"+
					"car2id=?,car2dial=?,car2result=?,car2newdial=? "+
					"where id=?");
		sql.put("UPDATEDRIVER", "update drivers set "+ AUTO.getDriverSetStr() + " where id=?");
		sql.put("UPDATEEVENTS", "update events set "+AUTO.getEventSetStr()+" where id=?");
		sql.put("UPDATERUN", "update runs set "+ AUTO.getRunSetStr() + " where id=?");
	}

	/**
	 * Write python version of file.
	 * @param args the command line args, ignored
	 * @throws IOException 
	 */
	public static void main(String args[]) throws IOException
	{
		PrintStream out = new PrintStream(new FileOutputStream("../pythonwebservice/nwrsc/model/sqlmap.py"));
		out.println("### AUTOGENERATED CODE, DO NOT MODIFY ###");
		out.println("sqlmap = {");
		String keys[] = SQLMap.keys().toArray(new String[0]);
		Arrays.sort(keys);
		for (String k : keys)
		{
			StringBuilder fixed = new StringBuilder();
			int holderid = 1;
			for (char c : SQLMap.get(k).toCharArray())
			{
				if (c == '?')
					fixed.append(":h"+(holderid++));
				else
					fixed.append(c);
			}
			out.println(String.format("'%s':'%s',", k, fixed.toString()));
		}
		out.println("}");
		out.close();
	}
}
