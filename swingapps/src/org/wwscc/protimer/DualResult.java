/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */


package org.wwscc.protimer;

public class DualResult
{
	public static final int NONE = 0;
	public static final int LEFT = 1;
	public static final int RIGHT = 2;
	public static final int LEFT_DEFAULT = 3;
	public static final int RIGHT_DEFAULT = 4;

	public Result left;
	public Result right;
	public int win;
	public int lead;
	public int cwin;

	public DualResult()
	{
		left = new Result();
		right = new Result();
		win = NONE;
		lead = NONE;
		cwin = NONE;
	}

	/* info */
	public boolean hasLeftReaction()
	{
		return (!Double.isNaN(left.rt));
	}

	public boolean hasRightReaction()
	{
		return (!Double.isNaN(right.rt));
	}

	public boolean useDial()
	{
		if (Double.isNaN(left.dial) && Double.isNaN(right.dial)) return false;
		if ((left.dial == 0) && (right.dial == 0)) return false;
		if (Double.isNaN(left.dial) && (right.dial == 0)) return false;
		if ((left.dial == 0) && Double.isNaN(right.dial)) return false;
		return true;
	}

	public boolean useChallengeDial()
	{
		if (Double.isNaN(left.cdial) && Double.isNaN(right.cdial)) return false;
		if (Double.isNaN(left.cdial) && (right.cdial == 0)) return false;
		if ((left.cdial == 0) && (right.cdial == 0)) return false;
		if ((left.cdial == 0) && Double.isNaN(right.cdial)) return false;
		return true;
	}


	public boolean deleteLeftStart() { return left.deleteStart(); }
	public boolean deleteRightStart() { return right.deleteStart(); }
	public Result deleteLeftFinish() { return left.deleteFinish(); }
	public Result deleteRightFinish() { return right.deleteFinish(); }

	/* get */

	public ColorTime getLeftReaction() { return new ColorTime(left.rt, left.state); }
	public ColorTime getRightReaction() { return new ColorTime(right.rt, right.state); }

	public ColorTime getLeftSixty() { return new ColorTime(left.sixty, left.state); }
	public ColorTime getRightSixty() { return new ColorTime(right.sixty, right.state); }

	public ColorTime getLeftFinish() { return new ColorTime(left.finish, left.state); }
	public ColorTime getRightFinish() { return new ColorTime(right.finish, right.state); }

	public double getLeftDial() { return left.dial; }
	public double getRightDial() { return right.dial; }

	public double getLeftChallengeDial() { return left.cdial; }
	public double getRightChallengeDial() { return right.cdial; }

	public double getLeftOrigDial() { return left.origdial; }
	public double getRightOrigDial() { return right.origdial; }

	public int getLeftState() { return left.state; }
	public int getRightState() { return right.state; }

	public int getWin() { return win; }
	public int getLead() { return lead; }
	public int getChallengeWin() { return cwin; }

	/* set */

	public void setLeftWin() { win = LEFT; }
	public void setRightWin() { win = RIGHT; }
	public void setLeftLead() { lead = LEFT; }
	public void setRightLead() { lead = RIGHT; }
	public void setLeftChallengeWin() { cwin = LEFT; }
	public void setRightChallengeWin() { cwin = RIGHT; }

	public void setLeftReaction(ColorTime c)
	{
		left.rt = c.time;
		if (c.state != 0) left.state = c.state;
	}

	public void setRightReaction(ColorTime c)
	{
		right.rt = c.time;
		if (c.state != 0) right.state = c.state;
	}

	public void setLeftSixty(ColorTime c)
	{
		left.sixty = c.time;
		if (c.state != 0) left.state = c.state;
	}

	public void setRightSixty(ColorTime c)
	{
		right.sixty = c.time;
		if (c.state != 0) right.state = c.state;
	}

	public void setLeftFinish(ColorTime c, double dial)
	{
		left.finish = c.time;
		left.dial = dial;
		if (c.state != 0) left.state = c.state;
	}

	public void setRightFinish(ColorTime c, double dial)
	{
		right.finish = c.time;
		right.dial = dial;
		if (c.state != 0) right.state = c.state;
	}

	public void setLeftDial(double d) { left.origdial = d; }
	public void setRightDial(double d) { right.origdial = d; }

	public void setLeftChallengeDial(double d, double od)
	{
		left.cdial = d;
		left.origdial = od;
	}

	public void setRightChallengeDial(double d, double od)
	{
		right.cdial = d;
		right.origdial = od;
	}

}


