/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.util.logging.Logger;
import org.wwscc.challenge.Id;

/**
 * Represents the 'regular' run data for a single side run taken during
 * a challenge run
 */
public class ChallengeRun extends Run
{
	private static Logger log = Logger.getLogger("org.wwscc.storage.ChallengeRun");

	private int challengeid; // comes from eventid mux
	private int round;  // comes from eventid mux

	public ChallengeRun()
	{
		super();
		challengeid = 0;
		round = 0;
	}

	public ChallengeRun(Run r)
	{
		this();
		id = r.id;
		carid = r.carid;
		eventid = r.eventid;
		course = r.course;
		run = r.run;
		cones = r.cones;
		status = r.status;
		reaction = r.reaction;
		sixty = r.sixty;
		raw = r.raw;
		net = r.net;
		fixids();
		calc();
	}

	protected void fixids()
	{
		challengeid = (eventid >> 16) & 0x0FFFF;
		round = eventid & 0x0FFF;
	}
	
	public ChallengeRun(double reaction, double sixty , double time, int cones, int gates, String status)
	{
		super(reaction, sixty, time, cones, gates, status);
		challengeid = (eventid >> 16) & 0x0FFFF;
		round = eventid & 0x0FFF;
	}

	public void setChallengeRound(Id.Run id)
	{
		challengeid = id.challengeid;
		round = id.round;
		course = id.isLeft() ? Run.LEFT : Run.RIGHT;
		eventid = ((challengeid & 0x0FFFF) << 16) + (round & 0x0FFFF);
	}

	private void calc()
	{		
		if (!status.equals("OK"))
			this.net = 999.999;
		else
			this.net = raw + (2*cones);
	}

	@Override
	public void setRaw(double val)
	{
		raw = val;
		calc();
	}

	public void setCones(int inCones)
	{
		cones = inCones;
		calc();
	}
	
	@Override
	public void setStatus(String inStatus)
	{
		status = inStatus;
		calc();
	}
	
	public int getChallengeId() { return challengeid; }
	public int getRound() { return round; }
}