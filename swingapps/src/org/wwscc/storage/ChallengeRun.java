/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2010 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.util.UUID;

import org.wwscc.challenge.Id;
import org.wwscc.util.IdGenerator;

/**
 * Represents the 'regular' run data for a single side run taken during
 * a challenge run
 */
public class ChallengeRun
{
	protected UUID   challengeid, carid; 
	protected double reaction, sixty, raw;
	protected int    round, course, cones, gates;
	protected String status;

	public ChallengeRun()
	{
		challengeid = IdGenerator.nullid;
		carid = IdGenerator.nullid;
		reaction = -1;
		sixty = -1;
		raw = -1;
		round = -1;
		course = -1;
		cones = 0;
		gates = 0;
		status = "";
	}

	public ChallengeRun(Run r)
	{
		this();
		carid    = r.carid;
		reaction = r.getReaction();
		sixty    = r.getSixty();
		raw      = r.getRaw();
		course   = r.course;
		cones    = r.cones;
		gates    = r.gates;
		status   = r.status;
	}

	public ChallengeRun(double reaction, double sixty , double time, int cones, int gates, String status)
	{
		this();
		this.reaction = reaction;
		this.sixty    = sixty;
		this.raw      = time;
		this.cones    = cones;
		this.gates    = gates;
		this.status   = status;
	}


	public void setChallengeRound(Id.Run id)
	{
		challengeid = id.challengeid;
		round = id.round;
		course = id.isLeft() ? Run.LEFT : Run.RIGHT;
	}
	
	public UUID getChallengeId() { return challengeid; }
	public int getRound() { return round; }
	public int getCones() { return cones; }
	public int getGates() { return gates; }
	public double getRaw() { return raw; }
	public double getReaction() { return reaction; }
	public double getSixty() { return sixty; }
	public String getStatus() { return status; }
	public double getNet() { return Double.NaN; } // FINISH ME
	
	public boolean isOK()
	{
		if (status == null) return false;
		return status.equals("OK");
	}

	public int statusLevel()
	{
		if (status.equals("RL") || status.equals("NS")) return 2;
		if (status.endsWith("DNF")) return 1;
		return 0;
	}	
}
