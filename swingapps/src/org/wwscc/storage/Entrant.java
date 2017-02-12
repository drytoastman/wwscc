/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2017 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Entrant
{
	UUID driverid;
	String firstname;
	String lastname;
	Car car;
	Map<Integer, Run> runs;
	boolean paid = false;

	public Entrant()
	{
		runs = new HashMap<Integer,Run>();
	}

	static public class NumOrder implements Comparator<Entrant>
	{
	    public int compare(Entrant e1, Entrant e2) { return e1.car.number - e2.car.number; }
	}

	public UUID getDriverId() { return driverid; }
	public String getName() { return firstname + " " + lastname; }
	public String getFirstName() { return firstname; }
	public String getLastName() { return lastname; }

	public UUID getCarId() { return car.carid; }
	public String getCarModel() { return car.model; }
	public String getCarColor() { return car.color; }
	public String getCarDesc() { return car.year + " " + car.model + " " + car.color; }
	public String getClassCode() { return car.classcode; }
	public String getIndexStr() { return car.getIndexStr(); }
	public int getNumber() { return car.number; }
	public boolean isPaid() { return paid; }


	/*
	 * @return Get a run based on its run number
	 */
	public Run getRun(int num)
	{
		return runs.get(num);
	}

	/*
	 * @return a collection of the runs for this entrant
	 */
	public Collection<Run> getRuns()
	{
		return runs.values();
	}

	/**
	 * @return true if this entrant has any runs entered at all
	 */
	public boolean hasRuns()
	{
		return (runs.size() > 0);
	}

	/**
	 * @return the number of actual recorded runs (not the max run number recorded)
	 */
	public int runCount()
	{
		return runs.size();
	}

	public void setRun(Run r)
	{
		runs.put(r.run, r);
	}
	
	public void clearRuns()
	{
		runs.clear();
	}

	public void deleteRun(int num)
	{
		runs.remove(num);
	}

	@Override
	public int hashCode() {
		return car.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Entrant other = (Entrant) obj;
		if (car == null || car.carid != other.car.carid) {
			return false;
		}
		return true;
	}
}


