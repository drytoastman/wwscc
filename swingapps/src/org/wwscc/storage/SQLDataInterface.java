/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2017 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.wwscc.util.IdGenerator;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;

/** */
public abstract class SQLDataInterface implements DataInterface
{
	private static Logger log = Logger.getLogger(SQLDataInterface.class.getCanonicalName());

	ClassData classCache = null;
	long classCacheTimestamp = 0;
	
	public abstract void start() throws SQLException;
	public abstract void commit() throws SQLException;
	public abstract void rollback();
	public abstract Object executeUpdate(String sql, List<Object> args) throws SQLException;
	public abstract void executeGroupUpdate(String sql, List<List<Object>> args) throws SQLException;
	public abstract ResultSet executeSelect(String sql, List<Object> args) throws SQLException;
	public abstract void closeLeftOvers();
	public abstract <T> List<T> executeSelect(String key, List<Object> args, Constructor<T> objc) throws SQLException;

	/**
	 * Utility function to create a list for passing args
	 * @param args list of objects to add to initially
	 * @return the new List
	 */
	static List<Object> newList(Object... args)
	{
		List<Object> l = new ArrayList<Object>();
		for (Object o : args)
			l.add(o);
		return l;
	}

	static void logError(String f, Exception e)
	{
		log.log(Level.SEVERE, f + " failed: " + e.getMessage() + "\nRefresh screen and try again", e);
	}

		
	@Override
	public String getSetting(String key)
	{
		try
		{
			ResultSet setting = executeSelect("select val from settings where name=?", newList(key));
			if (setting.next()) {
				return setting.getString("val");
			} else {
				return "";
			}
		} catch (SQLException ioe) {
			logError("getSetting", ioe);
			return "";
		}
	}

	@Override
	public List<Event> getEvents()
	{
		try
		{
			return executeSelect("select * from events order by date", null, Event.class.getConstructor(ResultSet.class));
		}
		catch (Exception ioe)
		{
			logError("getEvents", ioe);
			return null;
		}
	}

	@Override
	public boolean updateEventRuns(int eventid, int runs)
	{
		try
		{
			executeUpdate("update events set runs=? where id=?", newList(runs, eventid));
			return true;
		}
		catch (Exception ioe)
		{
			logError("updateEventRuns", ioe);
			return false;
		}
	}

	/**
	 * Utility function for methods that loads entrants from driver/car
	 * data as well as placing runs if they match.
	 * @param d result data containing entrant info
	 * @param r result data containing run info or null
	 * @return a list of entrants
	 * @throws SQLException 
	 */
	List<Entrant> loadEntrants(ResultSet d, ResultSet r) throws SQLException
	{
		List<Entrant> ret = new ArrayList<Entrant>();
		List<Run> runs = null;

		if (r != null)
		{
			runs = new ArrayList<Run>();
			while (r.next())
				runs.add(new Run(r));
		}

		while (d.next())
		{
			Entrant e = new Entrant(d);
			if (runs != null)
			{
				for (Run rx : runs)
				{
					if (rx.getCarId().equals(e.getCarId()))
						e.runs.put(rx.run, rx);
				}
			}
			
			ret.add(e);
		}

		return ret;
	}
		
	
	@Override
	public List<Entrant> getEntrantsByEvent(int eventid)
	{
		try
		{
			return loadEntrants(executeSelect("select distinct d.firstname as firstname,d.lastname as lastname,c.* from runs as r, cars as c, drivers as d " +
						"where r.carid=c.carid AND c.driverid=d.driverid and r.eventid=?", newList(eventid)), null);
		}
		catch (Exception ioe)
		{
			logError("getEntrantsByEvent", ioe);
			return null;
		}
	}


	@Override
	public List<Entrant> getRegisteredEntrants(int eventid)
	{
		try
		{
			return loadEntrants(executeSelect("select distinct d.firstname as firstname,d.lastname as lastname,c.*,x.paid from registered as x, cars as c, drivers as d " +
						"where x.carid=c.carid AND c.driverid=d.driverid and x.eventid=?", newList(eventid)), null);
		}
		catch (Exception ioe)
		{
			logError("getRegisteredEntrants", ioe);
			return null;
		}
	}
	
	
	@Override
	public List<Car> getRegisteredCars(UUID driverid, int eventid)
	{
		try
		{
			return executeSelect("select c.* from registered as x, cars as c, drivers as d " +
						"where x.carid=c.carid AND c.driverid=d.driverid and x.eventid=? and d.driverid=?", 
						newList(eventid, driverid), Car.class.getConstructor(ResultSet.class));
		}
		catch (Exception ioe)
		{
			logError("getRegisteredCars", ioe);
			return null;
		}
	}

	
	/**
	 * Gets all the entrants and their runs based on the current run order.  Ends up
	 * being a lot faster (particular over a network) to load all of the runs for the run
	 * group as one and then filter them to each entrant locally.
	 * @return the list of entrants in the current run order
	 */
	@Override
	public List<Entrant> getEntrantsByRunOrder(int eventid, int course, int rungroup)
	{
		try
		{
			ResultSet d = executeSelect("select d.firstname,d.lastname,c.*,reg.paid from drivers d " +
						"JOIN cars c ON c.driverid=d.driverid JOIN runorder r ON r.carid=c.carid LEFT JOIN registered as reg ON reg.carid=c.carid and reg.eventid=r.eventid  " +
						"where r.eventid=? AND r.course=? AND r.rungroup=? order by r.row", newList(eventid, course, rungroup));
			if (d == null)
				return new ArrayList<Entrant>();
			ResultSet runs = executeSelect("select * from runs where eventid=? and course=? and carid in " +
						"(select carid from runorder where eventid=? AND course=? AND rungroup=?)", newList(eventid, course, eventid, course, rungroup));
			List<Entrant> ret = loadEntrants(d, runs);
			closeLeftOvers();
			return ret;
		}
		catch (Exception ioe)
		{
			logError("getEntrantsByRunOrder", ioe);
			return null;
		}
	}

	
	@Override
	public Entrant loadEntrant(int eventid, UUID carid, int course, boolean loadruns)
	{
		try
		{
			ResultSet d = executeSelect("select d.firstname,d.lastname,c.*,r.paid from drivers as d, cars as c LEFT JOIN registered as r on r.carid=c.carid and r.eventid=?  " +
						"where c.driverid=d.driverid and c.carid=?", newList(eventid, carid));
			ResultSet runs = null;
			if (loadruns)
				runs = executeSelect("select * from runs where carid=? and eventid=? and course=?", newList(carid, eventid, course));
			List<Entrant> e = loadEntrants(d, runs);
			closeLeftOvers();
			if (e.size() > 0)
				return e.get(0);
			return null;
		}
		catch (Exception ioe)
		{
			logError("loadEntrant", ioe);
			return null;
		}
	}


	@Override
	public Set<UUID> getCarIdsForCourse(int eventid, int course)
	{
		try
		{
			ResultSet d = executeSelect("select carid from runorder where eventid=? AND course=?", newList(eventid, course));
			HashSet<UUID> ret = new HashSet<UUID>();
			while (d.next())
				ret.add((UUID)d.getObject("carid"));
			closeLeftOvers();
			return ret;
		}
		catch (Exception ioe)
		{
			logError("getCarIdsForCourse", ioe);
			return null;
		}
	}

	@Override
	public List<UUID> getCarIdsForRunGroup(int eventid, int course, int rungroup)
	{
		try
		{
			ResultSet d = executeSelect("select carid from runorder where eventid=? AND course=? AND rungroup=? order by row", newList(eventid, course, rungroup));
			List<UUID> ret = new ArrayList<UUID>();
			while (d.next())
				ret.add((UUID)d.getObject("carid"));
			return ret;
		}
		catch (Exception ioe)
		{
			logError("getCarIdsForRunGroup", ioe);
			return null;
		}
	}

	@Override
	public void setRunOrder(int eventid, int course, int rungroup, List<UUID> carids) 
	{
		try
		{
			if (rungroup <= 0) return; // Shouldn't be doing this if rungroup isn't valid

			/* Start transaction */
			start();

			List<List<Object>> lists = new ArrayList<List<Object>>(carids.size());

			int row = 0;
			for (UUID carid : carids)
			{
				row++;
				List<Object> items = new ArrayList<Object>(6);
				items.add(eventid);
				items.add(course);
				items.add(rungroup);
				items.add(row);
				items.add(carid);
				items.add(carid);
				lists.add(items);
			}
			
			// update our ids
			executeGroupUpdate("INSERT INTO runorder VALUES (?,?,?,?,?,now()) " + 
								"ON CONFLICT (eventid, course, rungroup, row) DO UPDATE " + 
								"SET carid=?,modified=now()", lists);
			
			// clear out any leftovers from previous values
			executeUpdate("DELETE FROM runorder where eventid=? and course=? and rungroup=? and row>?", newList(eventid, course, rungroup, row));
			commit();
		}
		catch (Exception ioe)
		{
			rollback();
			logError("setRunOrder", ioe);
		}
	}

	//****************************************************/

	protected boolean hasRuns(UUID eventid, int carid, int course)
	{
		try
		{
			boolean ret = false;
			List<Object> vals = newList(carid, eventid, course);
			ResultSet d = executeSelect("select count(run) as count from runs where carid=? and eventid=? and course=?", vals);
			if (d.next())
				ret = d.getInt("count") > 0;
			closeLeftOvers();
			return ret;
		}
		catch (SQLException ioe)
		{
			logError("hasRuns", ioe);
			return false;
		}
	}


	@Override
	public MetaCar loadMetaCar(Car c, int eventid, int course)
	{
		try
		{
			MetaCar mc = new MetaCar(c);
			ResultSet cr = executeSelect("select paid from registered where carid=? and eventid=?", newList(c.getCarId(), eventid));
			mc.isPaid = false;
			mc.isRegistered = cr.next();
			if (mc.isRegistered)
				mc.isPaid = cr.getBoolean("paid");
			
			ResultSet ar = executeSelect("select raw from runs where carid=? limit 1", newList(c.getCarId()));
			mc.hasActivity = ar.next();
			
			ResultSet rr = executeSelect("select row from runorder where carid=? and eventid=? and course=? limit 1", newList(c.getCarId(), eventid, course));
			mc.isInRunOrder = rr.next();
			
			closeLeftOvers();
			return mc;
		}
		catch (Exception ioe)
		{
			logError("loadMetaCar", ioe);
			return null;
		}
	}

	@Override
	public void newDriver(Driver d) throws SQLException
	{
		executeUpdate("insert into drivers values (?,?,?,?,?,?,?)", d.getValues());
	}

	@Override
	public void updateDriver(Driver d) throws SQLException
	{
		LinkedList<Object> vals = d.getValues();
		vals.add(vals.pop());
		executeUpdate("update drivers set firstname=?,lastname=?,email=?,password=?,membership=?,attr=?,modified=now() where driverid=?", vals);
	}

	@Override
	public void deleteDriver(Driver d) throws SQLException
	{
		executeUpdate("delete from drivers where driverid=?", newList(d.driverid));
	}

	@Override
	public void deleteDrivers(Collection<Driver> list) throws SQLException
	{
		try
		{
			start();
			for (Driver d : list)
				executeUpdate("delete from drivers where id=?", newList(d.driverid));
			commit();
		}
		catch (SQLException sql)
		{
			rollback();
			throw sql;
		}
	}
	
	
	@Override
	public Driver getDriver(UUID driverid)
	{
		try
		{
			return executeSelect("select * from drivers where driverid=?", newList(driverid), 
								Driver.class.getConstructor(ResultSet.class)).get(0);
		}
		catch (Exception ioe)
		{
			logError("getDriver", ioe);
			return null;
		}		
	}
	
	@Override
	public List<Driver> findDriverByMembership(String membership)
	{
		List<Driver> ret = new ArrayList<Driver>();
		try
		{
			return executeSelect("select * from drivers where membership like ?", newList(membership), 
					Driver.class.getConstructor(ResultSet.class));
		}
		catch (Exception ioe)
		{
			logError("findDriverByMembership", ioe);
		}
		return ret;
	}
	
	@Override
	public List<Car> getCarsForDriver(UUID driverid)
	{
		try
		{
			return executeSelect("select * from cars where driverid = ? order by classcode, number", 
							newList(driverid), Car.class.getConstructor(ResultSet.class));
		}
		catch (Exception ioe)
		{
			logError("getCarsForDriver", ioe);
			return null;
		}
	}


	@Override
	public Map<String, Set<String>> getCarAttributes() 
	{
		try
		{
			Map<String, Set<String>> ret = new HashMap<String, Set<String>>();
			HashSet<String> make  = new HashSet<String>();
			HashSet<String> model = new HashSet<String>();
			HashSet<String> color = new HashSet<String>();
			
			ResultSet rs = executeSelect("select attr from cars", null);
			while (rs.next())
			{
				JSONObject attr = (JSONObject)new JSONParser().parse(rs.getString("attr"));
				if (attr.containsKey("make"))  make.add((String)attr.get("make"));
				if (attr.containsKey("model")) model.add((String)attr.get("model"));
				if (attr.containsKey("color")) color.add((String)attr.get("color"));
			}
			
			ret.put("make",  make);
			ret.put("model", model);
			ret.put("color", color);
			return ret;
		}
		catch (Exception ioe)
		{
			logError("getCarAttributes", ioe);
			return null;
		}
	}

	@Override
	public void registerCar(int eventid, UUID carid, boolean paid, boolean overwrite) throws SQLException
	{
		List<Object> vals = newList(eventid, carid, paid);
		String sql = "INSERT INTO REGISTERED (eventid, carid, paid) VALUES (?, ?, ?) ON CONFLICT (eventid, carid) DO ";
		String upd = "UPDATE SET paid=?,modified=now()";
		String ign = "NOTHING"; 
		if (overwrite)
		{
			vals.add(paid);
			executeUpdate(sql+upd, vals);
		}
		else
		{
			executeUpdate(sql+ign, vals);
		}
	}

	@Override
	public void unregisterCar(int eventid, UUID carid) throws SQLException
	{
		List<Object> vals = newList(eventid, carid);
		executeUpdate("delete from registered where eventid=? and carid=?", vals);
	}

	@Override
	public void newCar(Car c) throws SQLException
	{
		executeUpdate("insert into cars values (?,?,?,?,?,?)", c.getValues());
		Messenger.sendEvent(MT.CAR_CREATED, c);
	}

	@Override
	public void updateCar(Car c) throws SQLException
	{
		LinkedList<Object> vals = c.getValues();
		vals.add(vals.pop());
		executeUpdate("update cars set driverid=?,classcode=?,indexcode=?,number=?,attr=?,modified=now() where carid=?", vals);
	}

	@Override
	public void deleteCar(Car c) throws SQLException
	{
		executeUpdate("delete from cars where carid=?", newList(c.carid));
	}

	@Override
	public void deleteCars(Collection<Car> list) throws SQLException
	{
		try
		{
			start();
			for (Car c : list)
				executeUpdate("delete from cars where carid=?", newList(c.carid));
			commit();
		}
		catch (SQLException ioe)
		{
			rollback();
			throw ioe;
		}
	}

	
	@Override
	public boolean isRegistered(UUID eventid, UUID carid)
	{
		try
		{
			ResultSet cr = executeSelect("select paid from registered where carid=? and eventid=?", newList(carid, eventid));
			boolean ret = cr.next();
			closeLeftOvers();
			return ret;
		}
		catch (Exception ioe)
		{
			logError("isRegistered", ioe);
			return false;
		}
	}
	
	@Override
	public void setRun(Run r)
	{
		try{
			executeUpdate("INSERT INTO runs (eventid, carid, course, run, cones, gates, raw, status, attr, modified) " +
						"values (?,?,?,?,?,?,?,?,?,now()) ON CONFLICT (eventid, carid, course, run) DO UPDATE " +
						"SET cones=?,gates=?,raw=?,status=?,attr=?,modified=now()", 
						newList(r.eventid, r.carid, r.course, r.run, r.cones, r.gates, r.raw, r.status, r.attr,
								r.cones, r.gates, r.raw, r.status, r.attr));
		} catch (Exception ioe){
			logError("setRun", ioe);
		}
	}

	@Override
	public void deleteRun(int eventid, UUID carid, int course, int run)
	{
		try {
			executeUpdate("DELETE FROM runs WHERE eventid=? AND carid=? AND course=? AND run=?", newList(eventid, carid, course, run));
		} catch (Exception ioe){
			logError("deleteRun", ioe);
		}
	}
	

	@Override
	public Set<UUID> getCarIdsByChallenge(int challengeid)
	{
		try
		{
			
			ResultSet rs = executeSelect("select car1id,car2id from challengerounds where challengeid=?", newList(challengeid));
			HashSet<UUID> ret = new HashSet<UUID>();
			while (rs.next())
			{
				ret.add((UUID)rs.getObject("car1id"));
				ret.add((UUID)rs.getObject("car2id"));
			}
			return ret;
		}
		catch (Exception ioe)
		{
			logError("getCarIdsByChallenge", ioe);
			return null;
		}
	}

	@Override
	public int newChallenge(int eventid, String name, int size)
	{
		int newid = -1;
		try
		{
			int rounds = size - 1;
			int depth = (int)(Math.log(size)/Math.log(2));
			start();

			newid = (Integer)executeUpdate("insert into challenges (eventid, name, depth) values (?,?,?)", newList(eventid, name, depth));

			String sql = "insert into challengerounds (challengeid,round,swappedstart) values (?,?,?)";
			List<Object> rargs = newList(newid, 0, false);
			for (int ii = 0; ii <= rounds; ii++)
			{
				rargs.set(1, ii);
				executeUpdate(sql, rargs);
			}
			rargs.set(1, 99);
			executeUpdate(sql, rargs);

			commit();
		}
		catch (Exception ioe)
		{
			logError("newChallenge", ioe);
			rollback();
		}
		
		return newid;
	}

	@Override
	public void deleteChallenge(int challengeid)
	{
		try
		{   // This delete cascades to challengrounds and challengeruns
			executeUpdate("DELETE FROM challenges WHERE challengeid=?", newList(challengeid));
		}
		catch (Exception ioe)
		{
			logError("deleteChallenge", ioe);
		}
	}
	
	
	@Override
	public List<Challenge> getChallengesForEvent(int eventid)
	{
		try
		{
			return executeSelect("select * from challenges where eventid = ?", newList(eventid), Challenge.class.getConstructor(ResultSet.class));
		}
		catch (Exception ioe)
		{
			logError("getChallengesForEvent", ioe);
			return null;
		}
	}

	@Override
	public List<ChallengeRound> getRoundsForChallenge(int challengeid) 
	{
		try
		{
			return executeSelect("select * from challengerounds where challengeid=?", newList(challengeid), 
								ChallengeRound.class.getConstructor(ResultSet.class));
		}
		catch (Exception ioe)
		{
			logError("getRoundsForChallenge", ioe);
			return null;
		}
	}

	@Override
	public List<ChallengeRun> getRunsForChallenge(int challengeid)
	{
		try
		{
			return executeSelect("select * from challengeruns where challengeid=?", newList(challengeid), ChallengeRun.class.getConstructor(ResultSet.class));
		}
		catch (Exception ioe)
		{
			logError("getRunsForChallenge", ioe);
			return null;
		}
	}

	final static class Leader {  // I miss python
		UUID Xcarid; double basis; double net;
		Leader(UUID i, double b, double n) { Xcarid = i; basis = b; net = n; }
	}
	
	@Override
	public Dialins loadDialins(int eventid) 
	{		
		String sql = "SELECT c.classcode,c.indexcode,c.attr->'tireindexed' as tireindexed,r.* "
				+ "FROM runs AS r JOIN cars AS c ON c.carid=r.carid WHERE r.eventid=? ORDER BY r.carid,r.course";

		try
		{
			Event e      = executeSelect("select * from events where eventid=?", newList(eventid), Event.class.getConstructor(ResultSet.class)).get(0);
			ResultSet rs = executeSelect(sql, newList(eventid));

			Dialins ret         = new Dialins();
			UUID currentid      = IdGenerator.nullid;
			String classcode    = "";
			String indexcode    = "";
			double index        = 1.0;
			double bestraw[]    = new double[2];
			double bestnet[]    = new double[2];
			
			// 1. load all runs
			// 2. calculate best raw/net/sum for each carid
			// 3. set personal dialin
			while (rs.next())
			{
				Run r = new Run(rs);
				double net = 999.999;
				
				if (!currentid.equals(r.getCarId()))  // next car, process previous
				{
					ret.setEntrant(currentid, classcode, bestraw[0]+bestraw[1], bestnet[0]+bestnet[1], index);
					currentid = r.getCarId();
					bestraw[0] = bestraw[1] = bestnet[0] = bestnet[1] = 999.999;
				}
				
				classcode    = rs.getString("classcode");
				indexcode    = rs.getString("indexcode");
				index        = getClassData().getEffectiveIndex(classcode, indexcode, rs.getBoolean("tireindexed"));
				
				if (r.isOK()) // we ignore non-OK runs
				{				
					int idx = r.course() - 1;
					if (r.raw < bestraw[idx])
						bestraw[idx] = r.raw;
					
					net = (r.getRaw() + (e.getConePenalty() * r.getCones()) + (e.getGatePenalty() * r.getGates())) * index;
					if (net < bestnet[idx])
						bestnet[idx] = net;
				}
			}
				
			// 4. order and set class dialins
			ret.finalize();
			closeLeftOvers();
			return ret;
		}
		catch (Exception ioe)
		{
			logError("loadDialins", ioe);
			return null;
		}
	}

	@Override
	public void updateChallenge(Challenge c)
	{
		try
		{
			LinkedList<Object> vals = c.getValues();
			vals.add(vals.pop());
			executeUpdate("update challenges set eventid=?,name=?,depth=? where challengeid=?", vals);
		}
		catch (SQLException ioe)
		{
			logError("updateChallenge", ioe);
		}
	}


	protected void _updateChallengeRound(ChallengeRound r) throws SQLException
	{
		List<Object> list = newList();
		list.add(r.swappedstart);
		list.add(r.car1.carid);
		list.add(r.car1.dial);
		list.add(r.car2.carid);
		list.add(r.car2.dial);
		list.add(r.challengeid);
		list.add(r.round);
		executeUpdate("update challengerounds set swappedstart=?,car1id=?,car1dial=?,car2id=?,car2dial=? where challengeid=? and round=?", list);
	}
	
	
	@Override
	public void updateChallengeRound(ChallengeRound r) 
	{
		try
		{
			_updateChallengeRound(r);
		}
		catch (Exception ioe)
		{
			logError("updateChallengeRound", ioe);
		}
	}
	
	@Override
	public void updateChallengeRounds(List<ChallengeRound> rounds) 
	{
		try
		{
			start();
			for (ChallengeRound r : rounds)
				_updateChallengeRound(r);
			commit();
		}
		catch (Exception ioe)
		{
			rollback();
			logError("updateChallengeRounds", ioe);
		}
	}
	
	@Override
	public void setChallengeRun(ChallengeRun r)
	{
		try {
			executeUpdate("INSERT INTO challengeruns (challengeid, round, carid, course, reaction, sixty, raw, cones, gates, status, modified) " +
						"values (?,?,?,?,?,?,?,?,?,?,now()) ON CONFLICT (challengeid, round, carid, course) DO UPDATE " +
						"SET reaction=?,sixty=?,raw=?,cones=?,gates=?,status=?,modified=now()", 
						newList(r.challengeid, r.round, r.carid, r.course, r.reaction, r.sixty, r.raw, r.cones, r.gates, r.status, 
								r.reaction, r.sixty, r.raw, r.cones, r.gates, r.status));
		} catch (Exception ioe){
			logError("setChallengeRun", ioe);
		}
	}

	@Override
	public void deleteChallengeRun(ChallengeRun r)
	{
		if (r == null)
			return;
		try {
			executeUpdate("DELETE FROM challengeruns where challengeid=? AND round=? AND carid=? AND course=?", newList(r.challengeid, r.round, r.carid, r.course)); 
		} catch (Exception ioe){
			logError("deleteChallengeRun", ioe);
		}
	}
	
	
	@Override
	public List<Driver> getDriversLike(String first, String last)
	{
		if ((first == null) && (last == null))
			return new ArrayList<Driver>();
		try
		{
			Constructor<Driver> cc = Driver.class.getConstructor(ResultSet.class);
			if (first == null)
				return executeSelect("select * from drivers where lower(lastname) like ? order by firstname,lastname", newList(last.toLowerCase()+"%"), cc);
			else if (last == null)
				return executeSelect("select * from drivers where lower(firstname) like ? order by firstname,lastname", newList(first.toLowerCase()+"%"), cc);
			else
				return executeSelect("select * from drivers where lower(firstname) like ? and lower(lastname) like ? order by firstname,lastname", newList(first.toLowerCase()+"%", last.toLowerCase()+"%"), cc);
		}
		catch (Exception ioe)
		{
			logError("getDriversLike", ioe);
			return null;
		}
	}


	@Override
	public boolean isInOrder(int eventid, UUID carid, int course) 
	{
		try
		{
			ResultSet rs = executeSelect("select row from runorder where eventid=? AND course=? AND carid=?", newList(eventid, course, carid));
			return rs.next();
		}
		catch (Exception ioe)
		{
			logError("isInOrder", ioe);
			return false;
		}
		finally
		{
			closeLeftOvers();
		}
	}
	
	@Override
	public boolean isInCurrentOrder(int eventid, UUID carid, int course, int rungroup) 
	{
		try
		{
			ResultSet rs = executeSelect("select row from runorder where eventid=? AND course=? AND rungroup=? AND carid=?", newList(eventid, course, rungroup, carid));
			return rs.next();
		}
		catch (Exception ioe)
		{
			logError("isInCurrentOrder", ioe);
			return false;
		}
		finally
		{
			closeLeftOvers();
		}
	}

	@Override
	public ClassData getClassData()
	{
		try
		{
			if ((classCache != null) && (classCacheTimestamp < System.currentTimeMillis() + 2000))
				return classCache;
				
			ClassData ret = new ClassData();
			for (ClassData.Class cls : executeSelect("select * from classlist", null, ClassData.Class.class.getConstructor(ResultSet.class)))
				ret.add(cls);
			for (ClassData.Index idx : executeSelect("select * from indexlist", null, ClassData.Index.class.getConstructor(ResultSet.class)))
				ret.add(idx);
			classCache = ret; // save for quick lookups when doing multiple calls within a couple seconds
			classCacheTimestamp = System.currentTimeMillis();
			return classCache;
		}
		catch (Exception ioe)
		{
			logError("getClassData", ioe);
			return null;
		}
	}

	@Override
	public String getEffectiveIndexStr(Car c)
	{
		return getClassData().getIndexStr(c.classcode, c.indexcode, c.isTireIndexed());
	}	
}
