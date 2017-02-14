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
import java.util.UUID;

/**
 *
 * @author bwilson
 */
public abstract class DataInterface
{	
	public abstract void close();
	
	public abstract String getSetting(String key);
	
	/** 
	 * @return a list of all events in the database 
	 */
	public abstract List<Event> getEvents();
	
	/** 
	 * update the run count for an event
	 * @param eventid the id of the event
	 * @param runs the number of runs to set to
	 * @return true if update succeeded
	 */
	public abstract boolean updateEventRuns(UUID eventid, int runs);

	/** 
	 * @param eventid TODO
	 * @return a list of all entrants participating in the current event  
	 */
	public abstract List<Entrant> getEntrantsByEvent(UUID eventid);
	
	/** 
	 * @param eventid TODO
	 * @return a list of all entrants registered for an event 
	 */
	public abstract List<Entrant> getRegisteredEntrants(UUID eventid);
	
	/** 
	 * @param driverid the driver id of the cars to search for
	 * @param eventid TODO
	 * @return a list of all registered car ids for a driver
	 */
	public abstract List<Car> getRegisteredCars(UUID driverid, UUID eventid);
	
	/* Entrants w/ runs */
	public abstract List<Entrant> getEntrantsByRunOrder(UUID eventid, int course, int rungroup); // get all entrants in a particular event/course/rungroup and loads their runs
	public abstract Entrant loadEntrant(UUID eventid, UUID carid, int course, boolean loadruns); // load an entrant by carid and all of the associated runs if desired

	public abstract List<UUID> getCarIdsForRunGroup(UUID eventid, int course, int rungroup); // get the carids based on the current run group
	public abstract Set<UUID> getCarIdsForCourse(UUID eventid, int course); // get the participating cardids based on the course
	public abstract void setRunOrder(UUID eventid, int course, int rungroup, List<UUID> carids); // set the run order of the current rungroup to carids

	//public abstract List<String> getRunGroupMapping(); // return the class codes assigned to the current run group

	public abstract void newDriver(Driver d) throws IOException; // create a new driver from data in d and set the id variable
	public abstract void updateDriver(Driver d) throws IOException; // update the driver values in the database
	public abstract void deleteDriver(Driver d) throws IOException;
	public abstract void deleteDrivers(Collection<Driver> d) throws IOException;
	public abstract Driver getDriver(UUID driverid);
	public abstract String getDriverNotes(UUID driverid);
	public abstract void setDriverNotes(UUID driverid, String notes);
	public abstract List<Driver> findDriverByMembership(String membership);

	public abstract List<Car> getCarsForDriver(UUID driverid); // get all cars for this driverid
	public abstract List<String> getCarAttributes(String attr); // get a unique list of possible 'attr' for the car
	
	/**
	 * Upon successful return, the provided car will be in the registered table for the current event.  If overwrite
	 * is true, then the paid value will overwrite the current value in the database if already present, otherwise, the
	 * value in the database already will stay.  If nothing is already present, overwrite is irrelevant.
	 * @param eventid TODO
	 * @param carid the carid to register in the current event
	 * @param paid true if the paid flag should be set
	 * @param overwrite true if we should overwrite a current registration entry (i.e. paid flag)
	 * @throws IOException
	 */
	public abstract void registerCar(UUID eventid, UUID carid, boolean paid, boolean overwrite) throws IOException;
	
	public abstract void unregisterCar(UUID eventid, UUID carid) throws IOException; // remove this car from the current event registration
	public abstract void newCar(Car c) throws IOException; // create a new car entry with this data, sets the id variable
	public abstract void updateCar(Car d) throws IOException; // update the car values in the database
	public abstract void deleteCar(Car d) throws IOException;
	public abstract void deleteCars(Collection<Car> d) throws IOException;
	public abstract boolean isRegistered(UUID eventid, UUID carid);
	public abstract MetaCar loadMetaCar(Car c, UUID eventid, int course);

	//public abstract boolean setEntrantRuns(Car newCar, Collection<Run> runs);
	//public abstract void insertRun(Run r); 
	public abstract void setRun(Run r);
	public abstract void deleteRun(UUID eventid, UUID carid, int course, int run);

	public abstract List<EventResult> getResultsForClass(UUID eventid, String classcode);
	public abstract AnnouncerData getAnnouncerDataForCar(UUID eventid, UUID carid);

	//** Challenge **/
	public abstract Set<UUID> getCarIdsByChallenge(UUID challengeid);
	public abstract void newChallenge(UUID eventid, String name, int size);
	public abstract void deleteChallenge(UUID challengeid);
	public abstract List<Challenge> getChallengesForEvent(UUID eventid); //int eventid);
	public abstract List<ChallengeRound> getRoundsForChallenge(UUID challengeid);
	public abstract List<ChallengeRun> getRunsForChallenge(UUID challengeid);
	public abstract Dialins loadDialins(UUID eventid); //int eventid);
	public abstract void updateChallenge(Challenge c);
	public abstract void updateChallengeRound(ChallengeRound r);
	public abstract void updateChallengeRounds(List<ChallengeRound> rounds);

	//** Cachable ??? ***/
	public abstract List<Driver> getDriversLike(String firstname, String lastname);
	public abstract Map<UUID, Driver> getAllDrivers();
	public abstract Map<UUID, Car> getAllCars();
	//public abstract Map<Integer, Run> getAllRuns();
	
	/**
	 * Uses currentEvent, currentCourse
	 * @param eventid TODO
	 * @param carid
	 * @param course TODO
	 * @return true if the carid is present in any rungroup for the current event/course
	 */
	public abstract boolean isInOrder(UUID eventid, UUID carid, int course);
	
	/**
	 * Uses currentEvent, currentCourse, currentRunGroup
	 * @param eventid TODO
	 * @param carid
	 * @param course TODO
	 * @param rungroup TODO
	 * @return true if the carid is present in the current event/course/rungroup
	 */
	public abstract boolean isInCurrentOrder(UUID eventid, UUID carid, int course, int rungroup);
	
	public abstract ClassData getClassData();
}

