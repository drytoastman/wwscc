/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;


/**
 */
public class Challenge
{
	//private static final Logger log = Logger.getLogger("org.wwscc.storage.Challenge");

	protected int id;
	protected int eventid;
	protected String name;
	protected int depth;

	public Challenge()
	{
		id = -1;
		eventid = -1;
	}

	public Challenge(int inEvent, String inName, int inDepth)
	{
		id = -1;
		eventid = inEvent;
		name = inName;
		depth = inDepth;
	}

	public int getId() { return id; }
	public int getEventId() { return eventid; }
	public String getName() { return name; }
	public int getDepth() { return depth; }

	public void setName(String s) { name = s; }
	
	@Override
	public String toString()
	{
		return name;
	}
}
