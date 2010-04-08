/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.util.logging.Logger;

/**
 *
 */
public class ChallengeRound
{
	private static Logger log = Logger.getLogger("org.wwscc.storage.ChallengeRound");

	protected int id;
	protected int challengeid;
	protected int round;
	protected RoundEntry car1;
	protected RoundEntry car2;

	public ChallengeRound()
	{
		challengeid = -1;
		round = -1;
	}

	public ChallengeRound(int challenge, int rnd)
	{
		challengeid = challenge;
		round = rnd;
		car1 = new RoundEntry();
		car2 = new RoundEntry();
	}

	public int getId() { return id; }
	public int getChallengeId() { return challengeid; }
	public int getRound() { return round; }
	public RoundEntry getCar1() { return car1; }
	public RoundEntry getCar2() { return car2; }

	public void applyRun(ChallengeRun r)
	{
		if (car1.carid == r.carid)
			car1.applyRun(r);
		else if (car2.carid == r.carid)
			car2.applyRun(r);
		else
			log.info("Throwing away run, doesn't belong to any of the round cars: " + r);
	}

	public enum RoundState { NONE, PARTIAL1, HALFNORMAL, HALFINVERSE, PARTIAL2, DONE, INVALID };
	public RoundState getState()
	{
		int val = 0;
		if (car1.getLeft() != null) val |= 0x08;
		if (car1.getRight() != null) val |= 0x04;
		if (car2.getLeft() != null) val |= 0x02;
		if (car2.getRight() != null) val |= 0x01;
		
		switch (val)
		{
			case 0:
				return RoundState.NONE;
			
			case 1:
			case 2:
			case 4:
			case 8:
				return RoundState.PARTIAL1;
				
			case 6:
				return RoundState.HALFINVERSE;
				
			case 9:
				return RoundState.HALFNORMAL;
				
			case 7:
			case 11:
			case 13:
			case 14:
				return RoundState.PARTIAL2;
				
			case 15:
				return RoundState.DONE;
				
			default:
				return RoundState.INVALID;
		}		
	}
	
	/**
	 * Organization class to combine the elements of a single car in a challenge round.
	 */
	public static class RoundEntry
	{
		protected int carid;
		protected double dial, result, newdial;
		private ChallengeRun left, right;

		public RoundEntry()
		{
			carid = -1;
			left = null;
			right = null;
			dial = 0.0;
			result = 0.0;
			newdial = 0.0;
		}
		
		public void setCar(int i) { carid = i; }
		public void setDial(double d) { dial = d; }

		public void applyRun(ChallengeRun r)
		{
			if (r.course == Run.LEFT)
				left = r;
			else if (r.course == Run.RIGHT)
				right = r;
			else
				log.info("Throwing away run as it isn't left or right: " + r);
		}
		
		public void setTo(RoundEntry re)
		{
			carid = re.carid;
			if (re.newdial == 0.0)
				dial = re.dial;
			else
				dial = re.newdial;
		}
		
		public void setResultByNet(double d)
		{
			result = d - (2*dial);
			if (Double.isNaN(d))
				return;
			
			double hresult = d/2;
			if (hresult < dial)
				newdial = dial - ((dial - hresult)*1.5);
			else
				newdial = dial;
		}

		public boolean breakout() { return newdial != dial; }
				
		public int getCar() { return carid; }
		public double getDial() { return dial; }
		public double getResult() { return result; }
		public double getNewDial() { return newdial; }
		public ChallengeRun getLeft() { return left; }
		public ChallengeRun getRight() { return right; }
	}
}