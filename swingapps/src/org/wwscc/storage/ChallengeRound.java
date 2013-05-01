/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.util.logging.Level;
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
	protected boolean swappedstart;
	protected RoundEntrant car1;
	protected RoundEntrant car2;

	public ChallengeRound()
	{
		challengeid = -1;
		round = -1;
		swappedstart = false;
	}

	public ChallengeRound(int challenge, int rnd)
	{
		challengeid = challenge;
		round = rnd;
		swappedstart = false;
		car1 = new RoundEntrant();
		car2 = new RoundEntrant();
	}

	public int getId() { return id; }
	public int getChallengeId() { return challengeid; }
	public int getRound() { return round; }
	public RoundEntrant getTopCar() { return car1; }
	public RoundEntrant getBottomCar() { return car2; }
	public boolean isSwappedStart() { return swappedstart; }
	public void setSwappedStart(boolean swapped) { swappedstart = swapped; }

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
		if (car1.getLeft() != null && !Double.isNaN(car1.getLeft().getRaw())) val |= 0x08;
		if (car1.getRight() != null  && !Double.isNaN(car1.getRight().getRaw())) val |= 0x04;
		if (car2.getLeft() != null && !Double.isNaN(car2.getLeft().getRaw())) val |= 0x02;
		if (car2.getRight() != null && !Double.isNaN(car2.getRight().getRaw())) val |= 0x01;
		
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
	public static class RoundEntrant
	{
		protected int carid;
		protected double dial, result, newdial;
		private ChallengeRun left, right;

		public RoundEntrant()
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
		public void setNewDial(double d) { newdial = d; }
		
		public void reset()
		{
			newdial = dial;
			left = null;
			right = null;
		}

		public void applyRun(ChallengeRun r)
		{
			if (r.course == Run.LEFT)
				left = r;
			else if (r.course == Run.RIGHT)
				right = r;
			else
				log.log(Level.INFO, "Throwing away run as it isn''t left or right: {0}", r);
		}
		
		public void setTo(RoundEntrant re)
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
			
			if (newdial <= 0)
				newdial = 0.001;
		}

		public boolean breakout() { return Math.abs(newdial - dial) < .0001; }
				
		public int getCar() { return carid; }
		public double getDial() { return dial; }
		public double getResult() { return result; }
		public double getNewDial() { return newdial; }
		public ChallengeRun getLeft() { return left; }
		public ChallengeRun getRight() { return right; }
	}
}