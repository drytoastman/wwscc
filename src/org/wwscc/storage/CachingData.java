/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides some caching of data if the data source is offsite or
 * of some other sort that could have long delays
 */
public class CachingData extends DataInterface
{
	DataInterface wrapped;

	protected List<Driver> foundDrivers;
	protected String savedSearchFirst;
	protected String savedSearchLast;

	protected HashMap<Integer, Boolean> inOrder;

	protected ClassData classData;

	public CachingData(DataInterface in)
	{
		wrapped = in;
	}

	@Override
	public void close()
	{
		wrapped.close();
	}
	
	@Override
	public void setCurrentEvent(Event e) 
	{
		wrapped.setCurrentEvent(e);
		inOrder.clear();
	}

	@Override
	public void setCurrentCourse(int course) 
	{
		wrapped.setCurrentCourse(course);
		inOrder.clear();
	}

	@Override
	public void setCurrentRunGroup(int rungroup) 
	{
		wrapped.setCurrentRunGroup(rungroup);
		inOrder.clear();
	}

	@Override
	public void setCurrentChallenge(int challengeid) { wrapped.setCurrentChallenge(challengeid); }

	@Override
	public String getCurrentSeries() {	return wrapped.getCurrentSeries(); }

	@Override
	public Event getCurrentEvent() { return wrapped.getCurrentEvent(); }

	@Override
	public int getCurrentCourse() { return wrapped.getCurrentCourse(); }

	@Override
	public int getCurrentRunGroup() { return wrapped.getCurrentRunGroup(); }

	@Override
	public int getCurrentChallenge() { return wrapped.getCurrentChallenge(); }

	@Override
	public List<Event> getEvents() { return wrapped.getEvents(); }

	@Override
	public void clearChanges() { wrapped.clearChanges(); }

	@Override
	public List<Change> getChanges() { return wrapped.getChanges(); }
	
	@Override
	public boolean updateEvent() { return wrapped.updateEvent(); }

	@Override
	public List<Entrant> getEntrantsByEvent() { return wrapped.getEntrantsByEvent(); }

	@Override
	public List<Entrant> getRegisteredEntrants() { return wrapped.getRegisteredEntrants(); }

	@Override
	public List<Entrant> getEntrantsByRunOrder() { return wrapped.getEntrantsByRunOrder(); }

	@Override
	public Entrant loadEntrant(int carid, boolean loadruns) { return wrapped.loadEntrant(carid, loadruns); }

	@Override
	public Entrant loadEntrantOpposite(int carid, boolean loadruns) { return wrapped.loadEntrantOpposite(carid, loadruns); }

	@Override
	public Set<Integer> getCarIdsForCourse() { return wrapped.getCarIdsForCourse(); }

	@Override
	public List<Integer> getCarIdsForRunGroup() { return wrapped.getCarIdsForRunGroup(); }

	@Override
	public void setRunOrder(List<Integer> carids) { wrapped.setRunOrder(carids); }

	@Override
	public void addToRunOrderOpposite(int carid) { wrapped.addToRunOrderOpposite(carid); }
	@Override
	public void removeFromRunOrderOpposite(int carid) { wrapped.removeFromRunOrderOpposite(carid); }
	/*
	@Override
	public void swapEntrant(int oldcarid, int newcarid) { wrapped.swapEntrant(oldcarid, newcarid); }
	@Override
	public void swapEntrantOpposite(int oldcarid, int newcarid) { wrapped.swapEntrantOpposite(oldcarid, newcarid); }
	 */
	@Override
	public boolean hasRuns(int carid) { return wrapped.hasRuns(carid); }
	@Override
	public boolean hasRunsOpposite(int carid) { return wrapped.hasRunsOpposite(carid); }
	@Override
	public Map<String, Integer> getClass2RunGroupMapping() { return wrapped.getClass2RunGroupMapping(); }
	@Override
	public void setClass2RunGroupMapping(Map<String, Integer> l) { wrapped.setClass2RunGroupMapping(l); }

	@Override
	public void newDriver(Driver d) throws IOException { wrapped.newDriver(d); }

	@Override
	public void updateDriver(Driver d) throws IOException { wrapped.updateDriver(d); }

	@Override
	public List<Car> getCarsForDriver(int driverid) { return wrapped.getCarsForDriver(driverid); }

	@Override
	public List<String> getCarAttributes(String attr) { return wrapped.getCarAttributes(attr); }

	@Override
	public void registerCar(int carid) throws IOException { wrapped.registerCar(carid); }

	@Override
	public void unregisterCar(int carid) throws IOException { wrapped.unregisterCar(carid); }

	@Override
	public void newCar(Car c) throws IOException { wrapped.newCar(c); }

	@Override
	public void insertRun(Run r) { wrapped.insertRun(r); }

	//@Override
	//public void deleteRun(Run r) { wrapped.deleteRun(r); }

	@Override
	public void updateRun(Run r) { wrapped.updateRun(r); }

	@Override
	public boolean setEntrantRuns(Car c, Collection<Run> runs) { return wrapped.setEntrantRuns(c, runs); }

	@Override
	public List<EventResult> getResultsForClass(String classcode) { return wrapped.getResultsForClass(classcode); }

	@Override
	public Set<Integer> getCarIdsByChallenge(int challengeid) { return wrapped.getCarIdsByChallenge(challengeid); }

	@Override
	public void newChallenge(String name, int size, boolean bonus) { wrapped.newChallenge(name, size, bonus); }

	@Override
	public List<Challenge> getChallengesForEvent() { return wrapped.getChallengesForEvent(); }

	@Override
	public List<ChallengeRound> getRoundsForChallenge(int challengeid) { return wrapped.getRoundsForChallenge(challengeid); }

	@Override
	public List<ChallengeRun> getRunsForChallenge(int challengeid) { return wrapped.getRunsForChallenge(challengeid); }

	@Override
	public Dialins loadDialins() { return wrapped.loadDialins(); }
	
	@Override
	public void updateChallengeRound(ChallengeRound r) { wrapped.updateChallengeRound(r); }


	/** Potentially cachable data **/

	@Override
	public Map<Integer, Driver> getAllDrivers() { return wrapped.getAllDrivers(); }

	@Override
	public Map<Integer, Car> getAllCars() { return wrapped.getAllCars(); }

	@Override
	public Map<Integer, Run> getAllRuns() { return wrapped.getAllRuns(); }

	@Override
	public List<Driver> getDriversLike(String firstname, String lastname)
	{
		if ((foundDrivers != null) && (savedSearchFirst != null) && (savedSearchLast != null) &&
				firstname.startsWith(savedSearchFirst) && lastname.startsWith(savedSearchLast))
		{
			/* Create a new sublist from the saved data */
			List<Driver> ret = new ArrayList<Driver>();
			for (Driver d : foundDrivers)
			{
				if (d.firstname.startsWith(firstname) && (d.lastname.startsWith(lastname)))
					ret.add(d);
			}
			return ret;
		}
			
		foundDrivers = wrapped.getDriversLike(firstname, lastname);
		savedSearchFirst = firstname;
		savedSearchLast = lastname;
		return foundDrivers;
	}

	
	@Override
	public boolean isInOrder(int carid)
	{
		Boolean ret = inOrder.get(carid);
		if (ret != null)
			return ret;
		ret = wrapped.isInOrder(carid);
		inOrder.put(carid, ret);
		return ret;
	}

	@Override
	public ClassData getClassData()
	{
		if (classData == null)
			classData = wrapped.getClassData();
		return classData;
	}

	@Override
	public void deleteDriver(Driver d) throws IOException { wrapped.deleteDriver(d); }

	@Override
	public void deleteDrivers(Collection<Driver> d) throws IOException { wrapped.deleteDrivers(d); }

	@Override
	public void updateCar(Car d) throws IOException { wrapped.updateCar(d); }

	@Override
	public void deleteCar(Car d) throws IOException { wrapped.deleteCar(d); }

	@Override
	public void deleteCars(Collection<Car> d) throws IOException { wrapped.deleteCars(d); }

	@Override
	public boolean isRegistered(Car c) { return wrapped.isRegistered(c); }
}
