/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** */
public interface DataInterface
{	
	/**
	 * Open a new series.  Closes the previous series if still open.
	 * @param series the name of the series to open
	 */
	public void open(String series, String password);
	
	/**
	 * Closes the currently open series connection if open.
	 */
	public void close();
	
	/**
	 * @param key the setting to lookup
	 * @return the string value of the setting
	 */
	public String getSetting(String key);
	
	/** 
	 * @return a list of all events in the current series 
	 */
	public List<Event> getEvents();
	
	/** 
	 * update the run count for an event
	 * @param eventid the id of the event
	 * @param runs the number of runs to set to
	 * @return true if update succeeded
	 */
	public boolean updateEventRuns(int eventid, int runs);

	/** 
	 * @param eventid TODO
	 * @return a list of all entrants participating in the current event  
	 */
	public List<Entrant> getEntrantsByEvent(int eventid);
	
	/** 
	 * @param eventid TODO
	 * @return a list of all entrants registered for an event 
	 */
	public List<Entrant> getRegisteredEntrants(int eventid);
	
	/** 
	 * @param driverid the driver id of the cars to search for
	 * @param eventid TODO
	 * @return a list of all registered car ids for a driver
	 */
	public List<Car> getRegisteredCars(UUID driverid, int eventid);
	
	/* Entrants w/ runs */
	public List<Entrant> getEntrantsByRunOrder(int eventid, int course, int rungroup); // get all entrants in a particular event/course/rungroup and loads their runs
	public Entrant loadEntrant(int eventid, UUID carid, int course, boolean loadruns); // load an entrant by carid and all of the associated runs if desired

	public List<UUID> getCarIdsForRunGroup(int eventid, int course, int rungroup); // get the carids based on the current run group
	public Set<UUID> getCarIdsForCourse(int eventid, int course); // get the participating cardids based on the course
	public void setRunOrder(int eventid, int course, int rungroup, List<UUID> carids); // set the run order of the current rungroup to carids

	public void newDriver(Driver d) throws SQLException; // create a new driver from data in d and set the id variable
	public void updateDriver(Driver d) throws SQLException; // update the driver values in the database
	public void deleteDriver(Driver d) throws SQLException;
	public void deleteDrivers(Collection<Driver> d) throws SQLException;
	public Driver getDriver(UUID driverid);
	public List<Driver> findDriverByMembership(String membership);
	public List<Driver> getDriversLike(String firstname, String lastname);

	public List<Car> getCarsForDriver(UUID driverid); // get all cars for this driverid
	public Map<String, Set<String>> getCarAttributes(); // get a unique list of possible 'attr' for the car
	
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
	public void registerCar(int eventid, UUID carid, boolean paid, boolean overwrite) throws SQLException;
	
	public void unregisterCar(int eventid, UUID carid) throws SQLException; // remove this car from the current event registration
	public void newCar(Car c) throws SQLException; // create a new car entry with this data, sets the id variable
	public void updateCar(Car d) throws SQLException; // update the car values in the database
	public void deleteCar(Car d) throws SQLException;
	public void deleteCars(Collection<Car> d) throws SQLException;
	public boolean isRegistered(UUID eventid, UUID carid);
	public MetaCar loadMetaCar(Car c, int eventid, int course);

	public void setRun(Run r);
	public void deleteRun(int eventid, UUID carid, int course, int run);

	/* Challenge */
	public Set<UUID> getCarIdsByChallenge(int challengeid);
	public void newChallenge(UUID eventid, String name, int size);
	public void deleteChallenge(int challengeid);
	public List<Challenge> getChallengesForEvent(int eventid);
	public List<ChallengeRound> getRoundsForChallenge(int challengeid);
	public List<ChallengeRun> getRunsForChallenge(int challengeid);
	public Dialins loadDialins(int eventid);
	public void updateChallenge(Challenge c);
	public void updateChallengeRound(ChallengeRound r);
	public void updateChallengeRounds(List<ChallengeRound> rounds);

	
	/**
	 * Uses currentEvent, currentCourse
	 * @param eventid TODO
	 * @param carid
	 * @param course TODO
	 * @return true if the carid is present in any rungroup for the current event/course
	 */
	public boolean isInOrder(int eventid, UUID carid, int course);
	
	/**
	 * Uses currentEvent, currentCourse, currentRunGroup
	 * @param eventid TODO
	 * @param carid
	 * @param course TODO
	 * @param rungroup TODO
	 * @return true if the carid is present in the current event/course/rungroup
	 */
	public boolean isInCurrentOrder(int eventid, UUID carid, int course, int rungroup);
	
	public ClassData getClassData();
}

