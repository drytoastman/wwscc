/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.util.logging.Logger;

public class EventResult
{
	private static Logger log = Logger.getLogger("org.wwscc.storage.EventResults");

	protected int id;
	protected int carid;
	protected String firstname;
	protected String lastname;
	protected String indexcode;
	protected int position;
	protected int courses; /* How many courses in the calculation */
	protected double sum;
	protected double diff;
	protected double points;
	protected int ppoints;
	protected SADateTime updated;
	
	private double indexvalue;

	public String getFullName() { return firstname + " " + lastname; }
	public int getCarId() { return carid; }
	public double getSum() { return sum; }
	public double getDiff() { return diff; }
	public String getIndexCode() { return indexcode; }
	public double getIndex() { return indexvalue; }
	public int getPosition() { return position; }
	
	protected void setIndex(double value) { indexvalue = value; }
}

