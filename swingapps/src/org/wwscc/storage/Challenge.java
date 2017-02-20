/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.util.LinkedList;

/**
 */
public class Challenge
{
	//private static final Logger log = Logger.getLogger("org.wwscc.storage.Challenge");

	protected int challengeid;
	protected int eventid;
	protected String name;
	protected int depth;

	public Challenge()
	{
		challengeid = -1;
		eventid = -1;
	}

	public Challenge(int inEvent, String inName, int inDepth)
	{
		challengeid = -1;
		eventid = inEvent;
		name = inName;
		depth = inDepth;
	}
	
	public LinkedList<Object> getValues()
	{
		LinkedList<Object> ret = new LinkedList<Object>();
		ret.add(challengeid);
		ret.add(eventid);
		ret.add(name);
		ret.add(depth);
		return ret;
	}

	public int getChallengeId() { return challengeid; }
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
