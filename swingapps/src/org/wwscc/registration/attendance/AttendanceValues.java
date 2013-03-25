package org.wwscc.registration.attendance;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class AttendanceValues
{
	public Map<Integer, Integer> activeyears;
	public Set<String> activeseries;
	public int championshipcount;
	
	public Map<String, Double> values;
	public Map<String, Boolean> calculations;
	/*
	 * totalyears, totalseries, totalevents, championships
	 * minyearcount, maxyearcount, avgyearcount
	 */
	
	public AttendanceValues()
	{
		activeyears = new HashMap<Integer, Integer>();
		activeseries = new HashSet<String>();
		values = new HashMap<String, Double>();
		calculations = new HashMap<String,Boolean>();
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