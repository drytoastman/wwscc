/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.io.Serializable;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Represents a single event from the database.
 */
public class Event extends AttrBase implements Serializable
{
	private static final long serialVersionUID = 3721488283732959966L;

	protected int       eventid;
	protected String    name;
	protected Date      date;
	protected Timestamp regopened;
	protected Timestamp regclosed;
	protected int       courses;
	protected int       runs;
	protected int       countedruns;
	protected int       perlimit; // per person
	protected int       totlimit; // for whole event
	protected double    conepen;
	protected double    gatepen;
	protected boolean   ispro;
	protected boolean   ispractice;
		
	@Override
	public String toString()
	{
		return name;
	}

	public Event()
	{
	}
	
	public Event(ResultSet rs) throws SQLException
	{
		eventid     = rs.getInt("eventid");
		name        = rs.getString("name");
		date        = rs.getDate("date");
		regopened   = rs.getTimestamp("regopened");
		regclosed   = rs.getTimestamp("regclosed");
		courses     = rs.getInt("courses");
		runs		= rs.getInt("runs");
		countedruns = rs.getInt("countedruns");
		perlimit 	= rs.getInt("perlimit");
		totlimit 	= rs.getInt("totlimit");
		conepen     = rs.getDouble("conepen");
		gatepen 	= rs.getDouble("gatepen");
		ispro       = rs.getBoolean("ispro");
		ispractice  = rs.getBoolean("ispractice");
		loadAttr(rs);
	}

	public int getEventId() { return eventid; }
	public int getRuns() { return runs; }
	public int getCountedRuns()
	{
		if (countedruns <= 0)
			return Integer.MAX_VALUE;
		else
			return countedruns;
	}
	public int getCourses() { return courses; }
	public boolean isPro() { return ispro; }
	public double getConePenalty() { return conepen; }
	public double getGatePenalty() { return gatepen; }

	public void setRuns(int r) { runs = r; }
}

