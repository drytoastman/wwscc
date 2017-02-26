/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.wwscc.util.IdGenerator;

/**
 *
 */
public class ChallengeRound
{
	private static Logger log = Logger.getLogger("org.wwscc.storage.ChallengeRound");

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
	
	public ChallengeRound(ResultSet rs) throws SQLException
	{
		challengeid  = rs.getInt("challengeid");
		round        = rs.getInt("round");
		car1         = new ChallengeRound.RoundEntrant();
		car1.carid   = (UUID)rs.getObject("car1id");
		car1.dial    = rs.getDouble("car1dial");
		car2         = new ChallengeRound.RoundEntrant();
		car2.carid   = (UUID)rs.getObject("car2id");
		car2.dial    = rs.getDouble("car2dial");
	}

	public int getChallengeId() { return challengeid; }
	public int getRound() { return round; }
	public RoundEntrant getTopCar() { return car1; }
	public RoundEntrant getBottomCar() { return car2; }
	public boolean isSwappedStart() { return swappedstart; }
	
	public void setSwappedStart(boolean swapped) { swappedstart = swapped; }

	public void applyRun(ChallengeRun r)
	{
		if ((car1.carid != null) && car1.carid.equals(r.carid))
			car1.applyRun(r);
		else if ((car2.carid != null) && car2.carid.equals(r.carid))
			car2.applyRun(r);
		else
			log.info("Throwing away run, doesn't belong to any of the round cars: " + r.carid);
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
		protected UUID carid;
		protected double dial;
		private ChallengeRun left, right;

		public RoundEntrant()
		{
			carid = IdGenerator.nullid;
			left = null;
			right = null;
			dial = 0.0;
		}
		
		public void setCar(UUID i) { carid = i; }
		public void setDial(double d) { dial = d; }
		
		public void reset()
		{
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
		
		public UUID getCarId() { return carid; }
		public double getDial() { return dial; }
		public ChallengeRun getLeft() { return left; }
		public ChallengeRun getRight() { return right; }

		public void setTo(UUID carid, double dial)
		{
			this.carid = carid;
			this.dial  = dial;
		}
	}
}