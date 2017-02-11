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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.wwscc.storage.SQLDataInterface.ResultRow;

/**
 * Represents a single event from the database.
 */
public class Event implements Serializable
{
	private static final long serialVersionUID = 3721488283732959966L;

	protected UUID      eventid;
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
	protected JSONObject attr;
	
	/*
	protected String segments;
	protected String location;
	protected String sponsor;
	protected String host;
	protected String designer;
	protected boolean doublespecial; // special double entry handling for registration
	protected int cost;
	protected String paypal;
	protected String snail;
	protected String notes;
	*/
	
	@Override
	public String toString()
	{
		return name;
	}

	public Event()
	{
	}
	
	public Event(ResultRow rs) throws SQLException
	{
		eventid     = rs.getUUID("eventid");
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
		attr        = rs.getJSON("attr");
	}

	public UUID getEventId() { return eventid; }
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

