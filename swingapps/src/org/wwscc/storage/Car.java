/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.wwscc.storage.SQLDataInterface.ResultRow;
import org.wwscc.util.IdGenerator;

public class Car implements Serializable
{
	private static final long serialVersionUID = -4356095380991113575L;
	
	protected UUID carid;
	protected UUID driverid;

	protected String year;
	protected String make;
	protected String model;
	protected String color;
	protected int number;
	protected String classcode;
	protected String indexcode;
	protected boolean tireindexed;
	
	// not part of car tables
	private String indexstr;

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
		carid = IdGenerator.generateId();
		driverid = IdGenerator.nullid;
		year = "";
		make = "";
		model = "";
		color = "";
		number = 0;
		classcode = "";
		indexcode = "";
		tireindexed = false;
		indexstr = null;
	}
	
	public Car(Car other)
	{
		carid = other.carid;
		driverid = other.driverid;
		year = other.year;
		make = other.make;
		model = other.model;
		color = other.color;
		number = other.number;
		classcode = other.classcode;
		indexcode = other.indexcode;
		tireindexed = other.tireindexed;
		indexstr = other.indexstr;
	}

	public Car(ResultRow rs) throws SQLException
	{
		carid       = rs.getUUID("carid");
		driverid    = rs.getUUID("driverid");
		classcode   = rs.getString("classcode");
		indexcode   = rs.getString("indexcode");
		number      = rs.getInt("number");
		
		JSONObject o = rs.getJSON("attr");
		year        = (String)o.get("year");
		make        = (String)o.get("make");
		model       = (String)o.get("model");
		color       = (String)o.get("color");
		tireindexed = ((Long)o.get("tireindexed")) > 0;		
	}

	public UUID getCarId() { return carid; }
	public UUID getDriverId() { return driverid; }
	public String getClassCode() { return classcode; }
	public String getIndexCode() { return indexcode; }
	public String getIndexStr() { return (indexstr != null) ? indexstr: indexcode; } 
	public int getNumber() { return number; }
	public String getYear() { return year; }
	public String getMake() { return make; }
	public String getModel() { return model; }
	public String getColor() { return color; }
	public boolean isTireIndexed() { return tireindexed;}
	
	public void setDriverId(UUID id) { driverid = id; }
	public void setNumber(int n) { number = n; }
	public void setYear(String s) { year = s; }
	public void setMake(String s) { make = s; }
	public void setModel(String s) { model = s; }
	public void setColor(String s) { color = s; }
	public void setClassCode(String s) { classcode = s; }
	public void setIndexCode(String s) { indexcode = s; }
	public void setTireIndexed(boolean b) { tireindexed = b; }
		
	protected void setIndexStr(String s) { indexstr = s; }
	
	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Car))
			return false;
		return ((Car)o).carid == carid;
	}

	@Override
	public int hashCode() {
		return carid.hashCode();
	}
}
