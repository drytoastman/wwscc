/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.io.Serializable;

/**
 * Represents a single event from the database.
 */
public class Event implements Serializable
{
	protected int id;
	protected boolean ispro;
	protected int courses;
	protected int runs;
	protected int countedruns;
	protected String segments;

	protected String password;
	protected String name;
	protected SADateTime.SADate date;
	protected String location;
	protected String sponsor;
	protected String host;
	protected String designer;

	protected SADateTime regopened;
	protected SADateTime regclosed;

	protected int perlimit; // per person
	protected int totlimit; // for whole event
	protected int cost;
	protected String paypal;
	protected String snail;
	protected String notes;

	@Override
	public String toString()
	{
		return name;
	}

	public Event()
	{
	}

	public int getId() { return id; }
	public int getRuns() { return runs; }
	public int getCountedRuns() { return countedruns; }
	public int getCourses() { return courses; }
	public boolean isPro() { return ispro; }

	public void setRuns(int r) { runs = r; }
}

