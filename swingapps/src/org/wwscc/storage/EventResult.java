/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.util.UUID;

public class EventResult implements Comparable<EventResult>
{
	protected UUID eventid;
	protected UUID carid;
	protected String classcode;
	protected int position;
	protected int courses; /* How many courses in the calculation */
	protected double sum;
	protected double diff;
	protected double diffpoints;
	protected int pospoints;
	
	// privates not followed by AUTO load or table definition
	private String firstname;
	private String lastname;
	private String indexstr;
	private double indexvalue;

	public String getFullName() { return firstname + " " + lastname; }
	public UUID getCarId() { return carid; }
	public double getSum() { return sum; }
	public double getDiff() { return diff; }
	public String getIndexStr() { return indexstr; }
	public double getIndex() { return indexvalue; }
	public int getPosition() { return position; }
	public int getCourseCount() { return courses; }
	

	protected void setIndex(String str, double value) 
	{
		indexvalue = value;
		indexstr = str;
	}
	
	protected void setName(String first, String last)
	{
		firstname = first;
		lastname = last;
	}
	
	@Override
	public int compareTo(EventResult o) {
		if (o.courses == courses)
			return (int)(1000*(getSum() - o.getSum()));
		return o.courses - courses;
	}
}

