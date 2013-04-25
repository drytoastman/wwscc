/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author bwilson
 */
public abstract class DataInterface
{
	String host;
	String seriesName;
	Event currentEvent;
	int currentCourse;
	int currentRunGroup;
	int currentChallengeId;

	public void setCurrentEvent(Event e) { currentEvent = e; }
	public void setCurrentCourse(int course) { currentCourse = course; }
	public void setCurrentRunGroup(int rungroup) { currentRunGroup = rungroup; }
	public void setCurrentChallenge(int challengeid) { currentChallengeId = challengeid; }

	public String getCurrentHost() { return host; }
	public String getCurrentSeries() { return seriesName; }
	public Event getCurrentEvent() { return currentEvent; }
	public int getCurrentCourse() { return currentCourse; }
	public int getCurrentRunGroup() { return currentRunGroup; }
	public int getCurrentChallenge() { return currentChallengeId; }

	public abstract void close();

	public abstract boolean isTrackingRegChanges();
	public abstract void trackRegChanges(boolean track);
	public abstract void clearChanges();
	public abstract List<Change> getChanges();
	public abstract int countChanges();
	
	public abstract String getSetting(String key);
	public abstract boolean getBooleanSetting(String key);
	public abstract void putBooleanSetting(String key, boolean val);
	
	/** 
	 * @return a list of all events in the database 
	 */
	public abstract List<Event> getEvents();
	
	/** 
	 * send the current event data to the database 
	 * @return true if update succeeded
	 */
	public abstract boolean updateEvent();

	/** 
	 * @return a list of all entrants participating in the current event  
	 */
	public abstract List<Entrant> getEntrantsByEvent();
	
	/** 
	 * @return a list of all entrants registered for an event 
	 */
	public abstract List<Entrant> getRegisteredEntrants();
	
	/** 
	 * @param driverid the driver id of the cars to search for
	 * @return a list of all registered car ids for a driver
	 */
	public abstract List<Car> getRegisteredCars(int driverid);
	
	/* Entrants w/ runs */
	public abstract List<Entrant> getEntrantsByRunOrder(); // get all entrants in a particular event/course/rungroup and loads their runs
	public abstract Entrant loadEntrant(int carid, boolean loadruns); // load an entrant by carid and all of the associated runs if desired

	public abstract List<Integer> getCarIdsForRunGroup(); // get the carids based on the current run group
	public abstract Set<Integer> getCarIdsForCourse(); // get the participating cardids based on the course
	public abstract void setRunOrder(List<Integer> carids); // set the run order of the current rungroup to carids

	public abstract List<String> getRunGroupMapping(); // return the class codes assigned to the current run group

	public abstract void newDriver(Driver d) throws IOException; // create a new driver from data in d and set the id variable
	public abstract void updateDriver(Driver d) throws IOException; // update the driver values in the database
	public abstract void deleteDriver(Driver d) throws IOException;
	public abstract void deleteDrivers(Collection<Driver> d) throws IOException;
	public abstract List<DriverField> getDriverFields() throws IOException;
	public abstract Driver getDriver(int driverid);
	public abstract List<Driver> findDriverByMembership(String membership);

	public abstract List<Car> getCarsForDriver(int driverid); // get all cars for this driverid
	public abstract List<String> getCarAttributes(String attr); // get a unique list of possible 'attr' for the car
	
	/**
	 * Upon successful return, the provided car will be in the registered table for the current event.  If overwrite
	 * is true, then the paid value will overwrite the current value in the database if already present, otherwise, the
	 * value in the database already will stay.  If nothing is already present, overwrite is irrelevant.
	 * @param carid the carid to register in the current event
	 * @param paid true if the paid flag should be set
	 * @param overwrite true if we should overwrite a current registration entry (i.e. paid flag)
	 * @throws IOException
	 */
	public abstract void registerCar(int carid, boolean paid, boolean overwrite) throws IOException;
	
	public abstract void unregisterCar(int carid) throws IOException; // remove this car from the current event registration
	public abstract void newCar(Car c) throws IOException; // create a new car entry with this data, sets the id variable
	public abstract void updateCar(Car d) throws IOException; // update the car values in the database
	public abstract void deleteCar(Car d) throws IOException;
	public abstract void deleteCars(Collection<Car> d) throws IOException;
	public abstract boolean isRegistered(int carid);
	public abstract MetaCar loadMetaCar(Car c);

	public abstract boolean setEntrantRuns(Car newCar, Collection<Run> runs);
	public abstract void insertRun(Run r); 
	public abstract void updateRun(Run r);
	public abstract void deleteRun(int id);

	public abstract List<EventResult> getResultsForClass(String classcode);
	public abstract AnnouncerData getAnnouncerDataForCar(int carid);

	//** Challenge **/
	public abstract Set<Integer> getCarIdsByChallenge(int challengeid);
	public abstract void newChallenge(String name, int size);
	public abstract void deleteChallenge(int challengeid);
	public abstract List<Challenge> getChallengesForEvent(); //int eventid);
	public abstract List<ChallengeRound> getRoundsForChallenge(int challengeid);
	public abstract List<ChallengeRun> getRunsForChallenge(int challengeid);
	public abstract Dialins loadDialins(); //int eventid);
	public abstract void updateChallenge(Challenge c);
	public abstract void updateChallengeRound(ChallengeRound r);
	public abstract void updateChallengeRounds(List<ChallengeRound> rounds);

	//** Cachable ??? ***/
	public abstract List<Driver> getDriversLike(String firstname, String lastname);
	public abstract Map<Integer, Driver> getAllDrivers();
	public abstract Map<Integer, Car> getAllCars();
	public abstract Map<Integer, Run> getAllRuns();
	
	/**
	 * Uses currentEvent, currentCourse
	 * @param carid
	 * @return true if the carid is present in any rungroup for the current event/course
	 */
	public abstract boolean isInOrder(int carid);
	
	/**
	 * Uses currentEvent, currentCourse, currentRunGroup
	 * @param carid
	 * @return true if the carid is present in the current event/course/rungroup
	 */
	public abstract boolean isInCurrentOrder(int carid);
	
	public abstract ClassData getClassData();
}

