/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2013 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.registration.attendance;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Represents a single boolean value that the user wishes to calculate from available attenance values
 */
public class AttendanceCalculation
{
	String processname;
	List<Syntax.Filter> filters; 
	List<Syntax.Comparison> comparisons;
	HashMap<String, AttendanceValues> entrants;
	
	protected AttendanceCalculation(String n, List<Syntax.Filter> f, List<Syntax.Comparison> c)
	{
		processname = n;
		filters = f;
		comparisons = c;
		entrants = new HashMap<String, AttendanceValues>();
		
		if (filters.size() == 0)
			filters.add(new Syntax.YesFilter()); // simply processEntry
	}
	
	public void processEntries(List<AttendanceEntry> entries)
	{
		for (AttendanceEntry entry : entries)
		{
			processEntry(entry);
		}
	}
	
	public void processEntry(AttendanceEntry entry)
	{
		for (Syntax.Filter f : filters)
		{
			if (!f.filter(entry))
				continue;
		
			String name = entry.last + ", " + entry.first;
			AttendanceValues values = entrants.get(name);
			if (values == null)
			{
				values = new AttendanceValues();
				entrants.put(name, values);
			}
			
			values.addEntry(entry);
		}
	}
	
	public Collection<String> getNames()
	{
		return entrants.keySet();
	}
	
	public Map<String,Double> getResult(String name)
	{
		if (!entrants.containsKey(name))
			return null;
		
		// do final calculations on each value set and then run comparisons on each
		AttendanceValues v = entrants.get(name);
		v.values.put("calculation", 1.0);
		v.calculate();
	
		// run the comparisons to see if everything matches
		for (Syntax.Comparison c : comparisons) {
			if (!c.compare(v.values)) {
				v.values.put("calculation", 0.0);
			}
		}
		
		return v.values;
	}
	
	class AttendanceValues
	{
		public Map<Integer, Integer> activeyears;
		public Set<String> activeseries;
		public int championshipcount;		
		public Map<String, Double> values;

		public AttendanceValues()
		{
			activeyears = new HashMap<Integer, Integer>();
			activeseries = new HashSet<String>();
			values = new HashMap<String, Double>();
			championshipcount = 0;
		}
		
		public void addEntry(AttendanceEntry entry)
		{
			if (!activeyears.containsKey(entry.year))
				activeyears.put(entry.year, 0);
			activeyears.put(entry.year, activeyears.get(entry.year) + entry.attended);
			activeseries.add(entry.series);
			championshipcount += entry.champ ? 1 : 0;
		}
		
		public void calculate()
		{
			values.put("totalyears", (double)activeyears.size());
			values.put("totalseries", (double)activeseries.size());
			values.put("championships", (double)championshipcount);
			
			int min = Integer.MAX_VALUE, max = 0, total = 0;
			for (Integer count : activeyears.values())
			{
				min = Math.min(min, count);
				max = Math.max(max, count);
				total += count;
			}
			values.put("minyearcount", (double)min);
			values.put("maxyearcount", (double)max);
			values.put("avgyearcount", (double)total/activeyears.size());
			values.put("totalevents", (double)total);
		}
	}
}


