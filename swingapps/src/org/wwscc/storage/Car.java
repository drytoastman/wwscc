/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.io.Serializable;
import java.util.Comparator;

public class Car implements Serializable
{
	protected int id;
	protected int driverid;

	protected String year;
	protected String make;
	protected String model;
	protected String color;
	protected int number;
	protected String classcode;
	protected String indexcode;

	/* meta, not added or retrieved from database */
	public boolean isInRunOrder = false;
	public boolean isRegistered = false;
	public int runcount = 0;

	static public class NumOrder implements Comparator<Car>
	{
	    public int compare(Car c1, Car c2)
		{
			return c1.number - c2.number;
		}
	}

	/*
	 * Create a blank car object.
	 */
	public Car()
	{
		id = -1;
		driverid = -1;
		year = "";
		make = "";
		model = "";
		color = "";
		number = 0;
		classcode = "";
		indexcode = "";
		isInRunOrder = false;
	}

	public int getId() { return id; }
	public int getDriverId() { return driverid; }
	public String getClassCode() { return classcode; }
	public String getIndexCode() { return indexcode; }
	public int getNumber() { return number; }
	public String getYear() { return year; }
	public String getMake() { return make; }
	public String getModel() { return model; }
	public String getColor() { return color; }

	public void setDriverId(int id) { driverid = id; }
	public void setNumber(int n) { number = n; }
	public void setYear(String s) { year = s; }
	public void setMake(String s) { make = s; }
	public void setModel(String s) { model = s; }
	public void setColor(String s) { color = s; }
	public void setClassCode(String s) { classcode = s; }
	public void setIndexCode(String s) { indexcode = s; }
	
	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Car))
			return false;
		return ((Car)o).id == id;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 59 * hash + this.id;
		return hash;
	}
}
