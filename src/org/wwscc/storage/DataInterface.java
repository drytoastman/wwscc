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
	boolean trackRegChangesFlag = false;

	public boolean isTrackingRegChanges() { return trackRegChangesFlag; }
	public void trackRegChanges(boolean b) { trackRegChangesFlag = b; }

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


	public abstract void close(); //

	public abstract void clearChanges();
	public abstract List<Change> getChanges();
	public abstract String getSetting(String key);
	
	public abstract List<Event> getEvents(); // get a list of all events in the database
	public abstract boolean updateEvent(); // update the current event data in the database

	/* Entrants w/o runs */
	public abstract List<Entrant> getEntrantsByEvent(); // get all entrants participating in an event
	public abstract List<Entrant> getRegisteredEntrants(); // get all entrants registered for an event
	/* Entrants w/ runs */
	public abstract List<Entrant> getEntrantsByRunOrder(); // get all entrants in a particular event/course/rungroup and loads their runs
	public abstract Entrant loadEntrant(int carid, boolean loadruns); // load an entrant by carid and all of the associated runs if desired
	public abstract Entrant loadEntrantOpposite(int carid, boolean loadruns);

	public abstract List<Integer> getCarIdsForRunGroup(); // get the carids based on the current run group
	public abstract Set<Integer> getCarIdsForCourse(); // get the participating cardids based on the course
	public abstract void setRunOrder(List<Integer> carids); // set the run order of the current rungroup to carids

	public abstract void addToRunOrderOpposite(int carid);  // append carid to run order on opposite course
	public abstract void removeFromRunOrderOpposite(int carid); // remove carid from opposite course run order
	public abstract boolean hasRuns(int carid);  // return true if car has runs recorded
	public abstract boolean hasRunsOpposite(int carid); // return true if car has runs recorded opposite

	public abstract List<String> getRunGroupMapping(); // return the class codes assigned to the current run group

	public abstract void newDriver(Driver d) throws IOException; // create a new driver from data in d and set the id variable
	public abstract void updateDriver(Driver d) throws IOException; // update the driver values in the database
	public abstract void deleteDriver(Driver d) throws IOException;
	public abstract void deleteDrivers(Collection<Driver> d) throws IOException;

	public abstract List<Car> getCarsForDriver(int driverid); // get all cars for this driverid
	public abstract List<String> getCarAttributes(String attr); // get a unique list of possible 'attr' for the car
	public abstract void registerCar(int carid) throws IOException; // add this car to the current event registration
	public abstract void unregisterCar(int carid) throws IOException; // remove this car from the current event registration
	public abstract void newCar(Car c) throws IOException; // create a new car entry with this data, sets the id variable
	public abstract void updateCar(Car d) throws IOException; // update the car values in the database
	public abstract void deleteCar(Car d) throws IOException;
	public abstract void deleteCars(Collection<Car> d) throws IOException;
	public abstract boolean isRegistered(Car c);

	public abstract boolean setEntrantRuns(Car newCar, Collection<Run> runs);
	public abstract void insertRun(Run r); 
	public abstract void updateRun(Run r);
	
	public abstract List<EventResult> getResultsForClass(String classcode);

	//** Challenge **/
	public abstract Set<Integer> getCarIdsByChallenge(int challengeid);
	public abstract void newChallenge(String name, int size, boolean bonus);
	public abstract List<Challenge> getChallengesForEvent(); //int eventid);
	public abstract List<ChallengeRound> getRoundsForChallenge(int challengeid);
	public abstract List<ChallengeRun> getRunsForChallenge(int challengeid);
	public abstract Dialins loadDialins(); //int eventid);
	public abstract void updateChallengeRound(ChallengeRound r);

	//** Cachable ??? ***/
	public abstract List<Driver> getDriversLike(String firstname, String lastname);
	public abstract Map<Integer, Driver> getAllDrivers();
	public abstract Map<Integer, Car> getAllCars();
	public abstract Map<Integer, Run> getAllRuns();
	public abstract boolean isInOrder(int carid); //int eventid, int course, int carid);
	public abstract ClassData getClassData();
}

