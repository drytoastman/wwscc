/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.util.UUID;

import org.wwscc.util.IdGenerator;

/**
 */
public class Challenge
{
	//private static final Logger log = Logger.getLogger("org.wwscc.storage.Challenge");

	protected UUID challengeid;
	protected int eventid;
	protected String name;
	protected int depth;

	public Challenge()
	{
		challengeid = IdGenerator.generateId();
		eventid = -1;
	}

	public Challenge(int inEvent, String inName, int inDepth)
	{
		challengeid = IdGenerator.generateId();
		eventid = inEvent;
		name = inName;
		depth = inDepth;
	}

	public UUID getChallengeId() { return challengeid; }
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
