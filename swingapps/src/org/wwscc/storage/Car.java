/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.UUID;

import org.wwscc.util.IdGenerator;

public class Car extends AttrBase
{
	protected UUID carid;
	protected UUID driverid;
	protected int number;
	protected String classcode;
	protected String indexcode;
	protected boolean useclsmult;

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
		super();
		carid     = IdGenerator.generateId();
		driverid  = IdGenerator.nullid;
		number    = 0;
		classcode = "";
		indexcode = "";
	}
	
	public Car(Car other)
	{
		super(other.attr);
		carid      = other.carid;
		driverid   = other.driverid;
		classcode  = other.classcode;
		indexcode  = other.indexcode;
		number     = other.number;
		useclsmult = other.useclsmult;
	}
	
	public Car(ResultSet rs) throws SQLException
	{
		super(rs);
		carid      = (UUID)rs.getObject("carid");
		driverid   = (UUID)rs.getObject("driverid");
		classcode  = rs.getString("classcode");
		indexcode  = rs.getString("indexcode");
		number     = rs.getInt("number");
		useclsmult = rs.getBoolean("useclsmult");
	}

	public LinkedList<Object> getValues()
	{
		LinkedList<Object> ret = new LinkedList<Object>();
		ret.add(carid);
		ret.add(driverid);
		ret.add(classcode);
		ret.add(indexcode);
		ret.add(number);
		ret.add(useclsmult);
		attrCleanup();
		ret.add(attr);
		return ret;
	}
	
	public UUID getCarId()         { return carid; }
	public UUID getDriverId()      { return driverid; }
	public String getClassCode()   { return classcode; }
	public String getIndexCode()   { return indexcode; }
	public int getNumber()         { return number; }
	public boolean useClsMult()    { return useclsmult; }
	public String getYear()        { return getAttrS("year"); }
	public String getMake()        { return getAttrS("make"); }
	public String getModel()       { return getAttrS("model"); }
	public String getColor()       { return getAttrS("color"); }
	
	public void setCarId(UUID id)        { carid = id; }
	public void setDriverId(UUID id)     { driverid = id; }
	public void setClassCode(String s)   { classcode = s; }
	public void setIndexCode(String s)   { indexcode = s; }
	public void setNumber(int n)         { number = n; }
	public void setUseClsMult(Boolean b) { useclsmult = b; }
	
	public void setYear(String s)      { setAttrS("year", s); }
	public void setMake(String s)      { setAttrS("make", s); }
	public void setModel(String s)     { setAttrS("model", s); }
	public void setColor(String s)     { setAttrS("color", s); }
	
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
