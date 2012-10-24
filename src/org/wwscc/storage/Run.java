/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.util.Comparator;
import java.util.logging.Logger;


/**
 * This represents a single run in an event.  Note that for simplicity we represent a ProSolo
 * run as default (reaction, sixty) and then use it for regular runs as well
 */
public class Run implements Serial, Cloneable
{
	private static Logger log = Logger.getLogger(Run.class.getCanonicalName());
	public static final int LEFT = 1;
	public static final int RIGHT = 2;
	public static final int SEGMENTS = 5;

	protected int id;
	protected int carid, eventid, course, run; 
	protected int cones, gates;
	protected String status;
	protected int rorder, norder, brorder, bnorder;
	protected double reaction, sixty, seg1, seg2, seg3, seg4, seg5, raw, net;

	public static class RawOrder implements Comparator<Run>
	{
		public int compare(Run a, Run b)
		{
			if (!a.isOK() && !b.isOK())
				return 0;
			if (!a.isOK())
				return 1;
			if (!b.isOK())
				return -1;
			return (int)(a.raw*1000 - b.raw*1000);
		}
	}
	public static class NetOrder implements Comparator<Run>
	{
		// net time already incorporates non-OK status 
		public int compare(Run o1, Run o2) { return (int)(o1.net*1000 - o2.net*1000);}
	}

	public Run()
	{
		this(0,0,0,"NA");
	}

	public Run(double raw)
	{
		this(raw, 0, 0, "OK");
	}
	

	public Run(double raw, int cones, int gates, String status)
	{
		if (Double.isNaN(raw))
			this.raw = 999.999;
		else
			this.raw = raw;

		this.cones	= cones;
		this.gates	= gates;
		this.status	= status;

		this.carid = -1;
		this.eventid = -1;
		this.course = -1;
		this.run = -1;
		this.rorder = -1;
		this.norder = -1;
		this.brorder = -1;
		this.bnorder = -1;

		/* Just assume 1.000 index for now */
		compute(1.0);
	}


	public Run(double reaction, double sixty , double time, int cones, int gates, String status)
	{
		this(time, cones, gates, status);
		this.reaction = reaction;
		this.sixty = sixty;
	}

	@Override
	public Object clone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public int getId() { return id; }
	public int run() { return run; }
	public int course() { return course; }
	public int getCarId() { return carid; }
	public int getCones() { return cones; }
	public int getGates() { return gates; }
	public String getStatus() { return status; }
	public boolean validRun() { return !Double.isNaN(raw); }

	public double getReaction() { return reaction; }
	public double getSixty() { return sixty; }
	public double getRaw() { return raw; }
	public double getNet() { return net; }

	public int getNetOrder() { return norder; }
	public int getRawOrder() { return rorder; }
	public int getBestRawOrder() { return brorder; }
	public int getBestNetOrder() { return bnorder; }

	public void setCourse(int c) { course = c; }
	
	public void setReaction(double d) { reaction = d; }
	public void setSixty(double d) { sixty = d; }
	public void setRaw(double d) { raw = d; compute(1.0); }
	public void setCones(int c) { cones = c; compute(1.0); }
	public void setGates(int g) { gates = g; compute(1.0); }
	public void setStatus(String s) { status = s; compute(1.0); }

	public double getSegment(int s)
	{
		switch (s)
		{
			case 1: return seg1;
			case 2: return seg2;
			case 3: return seg3;
			case 4: return seg4;
			case 5: return seg5;
		}
		return Double.NaN;
	}

	public void setSegment(int s, double d)
	{
		switch (s)
		{
			case 1: seg1 = d; break;
			case 2: seg2 = d; break;
			case 3: seg3 = d; break;
			case 4: seg4 = d; break;
			case 5: seg5 = d; break;
			default: log.warning("Invalid segmen to set: " + s); break;
		}
	}

	public void setCarId(int carid)
	{
		this.carid = carid;
	}


	public void setId(int eventid, int course, int run)
	{
		this.eventid = eventid;
		this.course = course;
		this.run = run;
	}

	public void updateTo(int eventid, int course, int run, int carid, double index)
	{
		this.eventid = eventid;
		this.course = course;
		this.run = run;
		this.carid = carid;
		compute(index);
	}

	public boolean isOK()
	{
		if (status == null) return false;
		return status.equals("OK");
	}

	public void compute(double index)
	{
		if (status.equals("OK"))		
		{
			if (Database.d.getBooleanSetting("indexafterpenalties"))
				net = (raw + (Database.d.currentEvent.conepen * cones) + (Database.d.currentEvent.gatepen * gates)) * index;
			else
				net = (raw * index) + (Database.d.currentEvent.conepen * cones) + (Database.d.currentEvent.gatepen * gates);
		}
		else if (status.equals("RL") || status.equals("NS"))
			net = 1999.999;
		else
			net = 999.999;
	}

	@Override
	public String toString()
	{
		return "<"+carid+","+eventid+","+course+","+run+","+raw+","+status+" ("+cones+","+gates+") N:"+net+">";
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Run)) return false;
		Run r = (Run)o;
		
		return ((r.course == course) && (r.run == run) && (r.status.equals(status)) &&
			(r.raw == raw) && (r.reaction == reaction) && (r.sixty == sixty) && (r.seg1 == seg1) &&
			(r.seg2 == seg2) && (r.seg3 == seg3) && (r.seg4 == seg4) && (r.seg5 == seg5));
	}

	@Override
	public String encode()
	{
		return String.format("%d %d %s %.3f %.3f %.3f %.3f %.3f %.3f %.3f %.3f",
			course, run, status, raw, reaction, sixty, seg1, seg2, seg3, seg4, seg5);
	}

	@Override
	public void decode(String in)
	{
		String s[] = in.trim().split("\\s+");

		int ii = 0;
		course = Integer.parseInt(s[ii++]);
		run = Integer.parseInt(s[ii++]);
		status = s[ii++];
		raw = Double.parseDouble(s[ii++]);
		reaction = Double.parseDouble(s[ii++]);
		sixty = Double.parseDouble(s[ii++]);
		seg1 = Double.parseDouble(s[ii++]);
		seg2 = Double.parseDouble(s[ii++]);
		seg3 = Double.parseDouble(s[ii++]);
		seg4 = Double.parseDouble(s[ii++]);
		seg5 = Double.parseDouble(s[ii++]);
	}
}
