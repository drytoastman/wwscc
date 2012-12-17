/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

public class EventResult implements Comparable<EventResult>
{
	protected int id;
	protected int eventid;
	protected int carid;
	protected String classcode;
	protected int position;
	protected int courses; /* How many courses in the calculation */
	protected double sum;
	protected double diff;
	protected double diffpoints;
	protected int pospoints;
	
	// privates not follwed by AUTO load or table definition
	private String firstname;
	private String lastname;
	private String indexstr;
	private double indexvalue;

	public String getFullName() { return firstname + " " + lastname; }
	public int getCarId() { return carid; }
	public double getSum() { return sum; }
	public double getDiff() { return diff; }
	public String getIndexStr() { return indexstr; }
	public double getIndex() { return indexvalue; }
	public int getPosition() { return position; }
	

	protected void setIndex(String code, boolean tireindexed, double value) 
	{
		if (code.equals("") && !tireindexed)
			indexstr = "";
		else
			indexstr = String.format("%s%s", code, tireindexed?"+T":"");
		indexvalue = value;
	}
	
	protected void setName(String first, String last)
	{
		firstname = first;
		lastname = last;
	}
	
	@Override
	public int compareTo(EventResult o) {
		return (int)(1000*(getSum() - o.getSum()));
	}
}

