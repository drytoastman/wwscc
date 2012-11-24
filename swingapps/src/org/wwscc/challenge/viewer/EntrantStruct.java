/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */
package org.wwscc.challenge.viewer;

import org.wwscc.challenge.ChallengeModel;
import org.wwscc.challenge.Id;

/**
 * Simple collection of pieces used to display each entrant in a round viewer
 */
class EntrantStruct
{
	EntrantDisplay display;
	RunDisplay leftRun, rightRun;
	
	public EntrantStruct(ChallengeModel m, Id.Entry eid)
	{
		display = new EntrantDisplay(m, eid);
		leftRun = new RunDisplay(m, eid.makeLeft());
		rightRun = new RunDisplay(m, eid.makeRight());
	}

	public String getName()
	{
		return display.name;
	}

	public void updateRun()
	{
		leftRun.updateRun();
		rightRun.updateRun();
	}

	public void updateColors()
	{
		leftRun.updateColor();
		rightRun.updateColor();
	}

	public double getDiff()
	{
		double d1 = leftRun.diff;
		double d2 = rightRun.diff;
		if (Double.isNaN(d1) && Double.isNaN(d2))
			return 0.0;
		else if (Double.isNaN(d1))
			return d2;
		else if (Double.isNaN(d2))
			return d1;
		else
			return d1 + d2;
	}
}
