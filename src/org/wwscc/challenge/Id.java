/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.challenge;

/**
 *
 */
public class Id 
{
	/**
	 * Represents a pointer to a particular round in the tree
	 */ 
	public static class Round
	{
		public int challengeid;
		public int round;

		public Round(Round r)
		{
			challengeid = r.challengeid;
			round = r.round;
		}
		
		public Round(int c, int r)
		{
			challengeid = c;
			round = r;
		}

		public Entry advancesTo() 
		{
			if (round == 99) /* third place winner */
				return new Entry(challengeid, 0, Entry.Level.LOWER);

			Entry.Level level = (round%2 != 0) ? Entry.Level.UPPER : Entry.Level.LOWER;
			return new Entry(challengeid, round/2, level);
		}
		
		public Entry advanceThird()
		{
			Entry.Level level;
			if (round == 2)
				level = Entry.Level.LOWER;
			else if (round == 3)
				level = Entry.Level.UPPER;
			else
				return null;
			
			return new Entry(challengeid, 99, level);
		}

		/**
		 * Get the depth within the tree, 1 is the final round, 2 is semis, 3 is quarters, etc.
		 */
		public int getDepth()
		{
			if (round < 2) return 1;
			if (round < 4) return 2;
			if (round < 8) return 3;
			if (round < 16) return 4;
			if (round < 32) return 5;
			return 6;
		}
		
		public Entry makeLower() { return new Entry(this, Entry.Level.LOWER); }
		public Entry makeUpper() { return new Entry(this, Entry.Level.UPPER); }
		
		public Run makeUpperLeft() { return new Run(this, Entry.Level.UPPER, Run.RunType.LEFT); }
		public Run makeUpperRight() { return new Run(this, Entry.Level.UPPER, Run.RunType.RIGHT); }
		public Run makeLowerLeft() { return new Run(this, Entry.Level.LOWER, Run.RunType.LEFT); }
		public Run makeLowerRight() { return new Run(this, Entry.Level.LOWER, Run.RunType.RIGHT); }
		
		@Override
		public boolean equals(Object o)
		{
			if (o == null) return false;
			Round other = (Round)o;
			return ((other.challengeid == challengeid) && (other.round ==  round));
		}
		
		public String toString()
		{
			return String.format("Round %s", round);
		}
	}

	
	/**
	 * Represents a pointer to an entrant side in a specific round
	 */
	public static class Entry extends Round
	{
		public enum Level { UPPER, LOWER };
		Level level;
		
		public Entry(Entry e)
		{
			super(e);
			level = e.level;
		}
		
		public Entry(int c, int r, Level l)
		{
			super(c, r);
			level = l;
		}
		
		public Entry(Round from, Level l)
		{
			super(from);
			level = l;
		}
		
		public Level getLevel() { return level; }
		public boolean isUpper() { return (level == Level.UPPER); }
		public boolean isLower() { return (level == Level.LOWER); }
		
		public Run makeLeft() { return new Run(this, Run.RunType.LEFT); }
		public Run makeRight() { return new Run(this, Run.RunType.RIGHT); }

		public boolean equals(Object o)
		{
			if (o == null) return false;
			Entry e = (Entry)o;
			return ((e.level == level) && super.equals(o));
		}
		
		public String toString()
		{
			return String.format("Entry %s/%s", round, level);
		}
	}
	
	
	/**
	 * Represents a pointer to a single run for an entrant in a specific round
	 */
	public static class Run extends Entry
	{
		public enum RunType { LEFT, RIGHT };
		RunType runType;
		
		public Run(Round r, Entry.Level l, RunType t)
		{
			super(r, l);
			runType = t;
		}
		
		public Run(int c, int r, Entry.Level l, RunType t)
		{
			super(c, r, l);
			runType = t;
		}

		public Run(Entry from, RunType t)
		{
			super(from.challengeid, from.round, from.level);
			runType = t;
		}
				
		public RunType getRunType() { return runType; }
		public boolean isLeft() { return (runType == RunType.LEFT); }
		public boolean isRight() { return (runType == RunType.RIGHT); }
		
		@Override
		public boolean equals(Object o)
		{
			if (o == null) return false;
			Run r = (Run)o;
			return ((r.runType == runType) && super.equals(o));
		}
		
		public String toString()
		{
			return String.format("Run %s/%s/%s", round, level, runType);
		}
	}
}
