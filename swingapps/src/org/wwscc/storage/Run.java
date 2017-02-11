/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.util.Comparator;
import java.util.UUID;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wwscc.util.IdGenerator;


/**
 * This represents a single run in an event.  Note that for simplicity we represent a ProSolo
 * run as default (reaction, sixty) and then use it for regular runs as well
 */
@SuppressWarnings("unchecked")
public class Run implements Serial, Cloneable
{
	public static final int LEFT = 1;
	public static final int RIGHT = 2;

	protected UUID eventid, carid;
	protected int course, run; 
	protected int cones, gates;
	protected String status;
	protected double raw;
	protected JSONObject attr;

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
		Event e;
		public NetOrder(Event e)
		{
			this.e = e;
		}
		// Take cone/gate/status into account
		public int compare(Run o1, Run o2) 
		{
			if (!o1.isOK() && !o2.isOK()) return 0;
			if (!o2.isOK()) return -1;
			if (!o1.isOK()) return  1;
			return (int)((o1.raw+(e.conepen*o1.cones)+(e.gatepen*o1.gates))*1000 
						- (o2.raw+(e.conepen*o2.cones)+(e.gatepen*o2.gates))*1000);
		}
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

		this.carid   = IdGenerator.nullid;
		this.eventid = IdGenerator.nullid;
		this.course  = -1;
		this.run     = -1;
		this.attr    = new JSONObject();
	}


	public Run(double reaction, double sixty , double time, int cones, int gates, String status)
	{
		this(time, cones, gates, status);
		attr.put("reaction", reaction);
		attr.put("sixty", sixty);
	}

	@Override
	public Object clone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public int run() { return run; }
	public int course() { return course; }
	public UUID getCarId() { return carid; }
	public int getCones() { return cones; }
	public int getGates() { return gates; }
	public String getStatus() { return status; }
	public boolean validRun() { return !Double.isNaN(raw); }

	public double getReaction()     { return (double)attr.getOrDefault("reaction", -1.0);}
	public double getSixty()        { return (double)attr.getOrDefault("sixty", -1.0); }
	public double getSegment(int s) { return (double)attr.getOrDefault("seg"+s, -1.0); }
	public double getRaw()          { return raw; }

	public void setRunNumber(int r)         { run = r; }
	public void setCourse(int c)            { course = c; }
	public void setReaction(double d)       { attr.put("reaction", d); }
	public void setSixty(double d)          { attr.put("sixty", d); }
	public void setSegment(int s, double d) { attr.put("seg"+s, d); }
	public void setRaw(double d)            { raw = d; }
	public void setCones(int c)             { cones = c; }
	public void setGates(int g)             { gates = g; }
	public void setStatus(String s)         { status = s; }
	public void setCarId(UUID cid)          { carid = cid; }

	public void setId(UUID eventid, int course, int run)
	{
		this.eventid = eventid;
		this.course = course;
		this.run = run;
	}

	public void updateTo(UUID inEventid, UUID inCarid, int inCourse, int inRun)
	{
		this.eventid = inEventid;
		this.carid = inCarid;
		this.course = inCourse;
		this.run = inRun;
	}

	public boolean isOK()
	{
		if (status == null) return false;
		return status.equals("OK");
	}

	@Override
	public String toString()
	{
		return "<"+carid+","+eventid+","+course+","+run+","+raw+","+status+" ("+cones+","+gates+")>";
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Run)) return false;
		Run r = (Run)o;
		
		return ((r.course == course) && (r.run == run) && (r.status.equals(status)) &&
				(r.raw == raw) && (r.attr.equals(attr)));	
	}

	@Override
	public int hashCode() 
	{
		return course ^ run ^ new Double(raw).hashCode();
	}

	@Override
	public String encode()
	{
		JSONObject out = new JSONObject();
		out.put("course", course);
		out.put("run", run);
		out.put("status", status);
		out.put("raw", raw);
		out.put("attr", attr);
		return out.toJSONString();
	}

	@Override
	public void decode(String str) throws ParseException
	{
		JSONObject in = (JSONObject)(new JSONParser().parse(str));
		course = (Integer)in.getOrDefault("course", -1);
		run    = (Integer)in.getOrDefault("run", -1);
		raw    = (Double)in.getOrDefault("raw", -1);
		status = (String)in.getOrDefault("status", "OK");
		attr   = (JSONObject)in.getOrDefault("attr", new JSONObject());
	}
}
