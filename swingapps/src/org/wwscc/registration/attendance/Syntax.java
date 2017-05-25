/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2013 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.registration.attendance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PunchCard = Series(nwr,pro); championships=0; MinYear(2011); maxyearcount<=4;
 * ISTClass = totalevents<=9; avgyearcount<=3
 */

public class Syntax
{
	private static final Logger log = Logger.getLogger(Syntax.class.getCanonicalName());
	
	static Pattern filter = Pattern.compile("(\\w+)\\((.*?)\\)");
	static Pattern comparison = Pattern.compile("(\\w+)([<>=!]*)(\\d+)");
			 
	public static List<AttendanceCalculation> scanAll(String multiline)
	{
		List<AttendanceCalculation> ret = new ArrayList<AttendanceCalculation>();
		for (String s : multiline.split("\n")) {
			try {
				AttendanceCalculation c = scan(s);
				if (c != null) ret.add(c);
			} catch (Exception e) {
				log.severe("Failed to parse attedance setting line  (" + s + "): " + e);
			}
		}
		
		return ret;
	}
	
	public static AttendanceCalculation scan(String s) throws Exception // all sorts of parsing and reflection stuff
	{		
		String a[] = s.replace(" ",  "").split("=", 2);
		if (a.length < 2)
			return null;
		
		AttendanceCalculation ret = new AttendanceCalculation(a[0]);
		
		for (String action : a[1].split(";"))
		{
			Matcher m = filter.matcher(action);
			if (m.find()) {
				ret.add(filterMap.get(m.group(1)).getConstructor(String.class).newInstance(m.group(2)));
				continue;
			}

			m = comparison.matcher(action);
			if (m.find()) {
				ret.add(new Comparison(m.group(1), m.group(2), m.group(3))); 
			}	
		}
		
		return ret;
	}
	
	
	public interface Filter
	{
		public boolean filter(AttendanceEntry in);
		public String getName();
	}
	
	static class MinYearFilter implements Filter
	{
		int minyear;
		public MinYearFilter(String arg) { minyear = Integer.parseInt(arg); }
		public boolean filter(AttendanceEntry in) { return (in.year >= minyear); }
		public String getName() { return String.format("MinYear(%s)", minyear); }
	}
	
	static class SeriesFilter implements Filter
	{
		List<String> matchseries;
		public SeriesFilter(String arg) { matchseries = Arrays.asList(arg.split(",")); }
		public boolean filter(AttendanceEntry in) { return (matchseries.contains(in.series.toLowerCase())); }
		public String getName() { throw new UnsupportedOperationException("Need to reimplement getAttendance if anyone uses it anymore"); }
	}
	
	public static final Map<String, Class<? extends Filter>> filterMap;
	static {
		filterMap = new HashMap<String, Class<? extends Filter>>();
		filterMap.put("Series", SeriesFilter.class);
		filterMap.put("MinYear", MinYearFilter.class);
	}
	
	public enum CompareType {
		LESSTHAN,
		GREATERTHAN,
		LESSTHANEQUAL,
		GREATERTHANEQUAL,
		EQUAL,
		NOTEQUAL,
	}
	
	static class Comparison
	{
		String arg;
		CompareType type;
		double value;
		
		public Comparison(String a, String t, String n) 
		{ 
			arg = a;
			if (t.equals("<")) type = CompareType.LESSTHAN;
			else if (t.equals(">")) type = CompareType.GREATERTHAN;
			else if (t.equals("<=")) type = CompareType.LESSTHANEQUAL;
			else if (t.equals(">=")) type = CompareType.GREATERTHANEQUAL;
			else if (t.equals("=")) type = CompareType.EQUAL;
			else if (t.equals("!=")) type = CompareType.NOTEQUAL;
			value = Double.parseDouble(n);
		}
		
		public String getName()
		{
			return arg;
		}
		
		public boolean compare(Double input)
		{			
			switch (type)
			{
				case LESSTHAN: return input < value;
				case LESSTHANEQUAL: return input <= value;
				case GREATERTHAN: return input > value;
				case GREATERTHANEQUAL: return input >= value;
				case EQUAL: return input == value;
				case NOTEQUAL: return input != value;
			}			
			return false;
		}
	}

}
