/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Entrant
{
	int driverid;
	String firstname;
	String lastname;
	Car car;
	Map<Integer, Run> runs;
	double index = 1.000;  // loaded/calculated when loading entrant

	public Entrant()
	{
		runs = new HashMap<Integer,Run>();
	}

	static public class NumOrder implements Comparator<Entrant>
	{
	    public int compare(Entrant e1, Entrant e2) { return e1.car.number - e2.car.number; }
	}

	public int getDriverId() { return driverid; }
	public String getName() { return firstname + " " + lastname; }
	public String getFirstName() { return firstname; }
	public String getLastName() { return lastname; }

	public int getCarId() { return car.id; }
	public String getCarModel() { return car.model; }
	public String getCarColor() { return car.color; }
	public String getCarDesc() { return car.year + " " + car.model + " " + car.color; }
	public String getClassCode() { return car.classcode; }
	public String getIndexStr() { return car.getIndexStr(); }
	public double getIndex() { return index; }
	public int getNumber() { return car.number; }


	public Run getRun(int num)
	{
		return runs.get(num);
	}

	/**
	 * Return an array of runs, each in their proper indexed locations, missing runs will be null
	 */
	public Run[] getRuns()
	{
		if (runs.size() <= 0)
			return new Run[0];

		Run result[] = new Run[Collections.max(runs.keySet())];
		for (int ii = 0; ii < result.length; ii++)
		{
			result[ii] = runs.get(ii+1);
		}

		return result;
	}

	/**
	 * Determine if this entrant has any runs entered at all
	 */
	public boolean hasRuns()
	{
		return (runs.size() > 0);
	}

	/**
	 * Return the number of actual recorded runs (not the max run number recorded)
	 */
	public int runCount()
	{
		return runs.size();
	}


	public Map<Integer, Run> removeRuns()
	{
		Map<Integer, Run> ret = new HashMap<Integer,Run>(runs);
		runs.clear();
		Database.d.setEntrantRuns(car, runs.values());
		return ret;
	}

	public void setRuns(Map<Integer,Run> newruns)
	{
		for (Run r : newruns.values())
		{
			r.updateTo(Database.d.currentEvent.id, Database.d.currentCourse, r.run, car.id, index);
			runs.put(r.run, r);
		}

		sortRuns(runs);  // just in case
		Database.d.setEntrantRuns(car, runs.values());
	}
	
	public void setRun(Run r, int pos)
	{
		// TODO, always set for global state?
		r.updateTo(Database.d.currentEvent.id, Database.d.currentCourse, pos, car.id, index);

		HashMap<Integer, Run> tempruns = new HashMap<Integer,Run>(runs);
		tempruns.put(pos, r);
		sortRuns(tempruns);
		if (Database.d.setEntrantRuns(car, tempruns.values()))
			runs = tempruns;
	}


	public void deleteRun(int num)
	{
		Run r = runs.get(num);
		if (r != null)
		{
			HashMap<Integer, Run> tempruns = new HashMap<Integer,Run>(runs);
			tempruns.remove(num);
			sortRuns(tempruns);
			if (Database.d.setEntrantRuns(car, tempruns.values()))
				runs = tempruns;
		}
	}

	private static Run.RawOrder rorder = new Run.RawOrder();
	private static Run.NetOrder norder = new Run.NetOrder();

	protected void sortRuns(Map<Integer, Run> theruns)
	{
		List<Run> list = new ArrayList<Run>(theruns.values());
		if (list.size() == 0) return;

		Collections.sort(list, rorder);
		for (int ii = 0; ii < list.size(); ii++)
		{
			list.get(ii).brorder = ii+1;
			list.get(ii).rorder = -1;
		}

		Collections.sort(list, norder);
		for (int ii = 0; ii < list.size(); ii++)
		{
			list.get(ii).bnorder = ii+1;
			list.get(ii).norder = -1;
		}

		// reduce list to first x runs
		int cnt = Math.min(Database.d.getCurrentEvent().getCountedRuns(), Database.d.getClassData().getClass(car.classcode).getCountedRuns());
		Iterator<Run> iter = list.iterator();
		while (iter.hasNext())
		{
			Run r = iter.next();
			if (r.run > cnt)
				iter.remove();
		}

		Collections.sort(list, rorder);
		for (int ii = 0; ii < list.size(); ii++)
			list.get(ii).rorder = ii+1;

		Collections.sort(list, norder);
		for (int ii = 0; ii < list.size(); ii++)
			list.get(ii).norder = ii+1;
	}
	

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 29 * hash + (car != null ? car.id : 0);
		return hash;
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
		if (car == null || car.id != other.car.id) {
			return false;
		}
		return true;
	}

}


