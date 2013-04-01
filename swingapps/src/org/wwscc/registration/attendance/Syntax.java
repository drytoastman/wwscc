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
 * PunchCard = Series(nwr,pro);MinYear(2011);maxyear<=4;championships=0
 * ISTClass = totalevents<10;avgperyear<3
 */

public class Syntax
{
	private static final Logger log = Logger.getLogger(Syntax.class.getCanonicalName());
	
	static Pattern filter = Pattern.compile("(\\w+)\\((.*?)\\)");
	static Pattern comparison = Pattern.compile("(\\w+)([<>=!]*)(\\d+)");
			 
	public static AttendanceCalculation scan(String s) throws Exception // all sorts of parsing and reflection stuff
	{
		List<Filter> filters = new ArrayList<Filter>();
		List<Comparison> comparisons = new ArrayList<Comparison>();
		
		String a[] = s.replace(" ",  "").split("=", 2);
		for (String action : a[1].split(";"))
		{
			Matcher m = filter.matcher(action);
			if (m.find()) {
				filters.add(filterMap.get(m.group(1)).getConstructor(String.class).newInstance(m.group(2)));
				continue;
			}

			m = comparison.matcher(action);
			if (m.find()) {
				comparisons.add(new Comparison(m.group(1), m.group(2), m.group(3))); 
			}	
		}
		
		return new AttendanceCalculation(a[0], filters, comparisons);
	}
	
	
	public interface Filter
	{
		public boolean filter(AttendanceEntry in);
	}
	
	static class YesFilter implements Filter
	{
		public boolean filter(AttendanceEntry in) { return true; }
	}
	
	static class MinYearFilter implements Filter
	{
		int minyear;
		public MinYearFilter(String arg) { minyear = Integer.parseInt(arg); }
		public boolean filter(AttendanceEntry in) { return (in.year >= minyear); }
	}
	
	static class SeriesFilter implements Filter
	{
		List<String> matchseries;
		public SeriesFilter(String arg) { matchseries = Arrays.asList(arg.split(",")); }
		public boolean filter(AttendanceEntry in) { return (matchseries.contains(in.series.toLowerCase())); }
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
		
		public boolean compare(Map<String,Double> values)
		{
			Double input = values.get(arg);
			if (input == null)
			{
				log.warning("Unknown variable name " + arg);
				return false;
			}
			
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
