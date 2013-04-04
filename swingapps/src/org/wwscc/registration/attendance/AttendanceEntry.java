/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2013 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.registration.attendance;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendanceEntry 
{
	public final static List<String> truths = Arrays.asList(new String[] { "1", "True", "true"});
	public String series;
	public int year;
	public String first;
	public String last;
	public int attended;
	public boolean champ;
	
	public AttendanceEntry(Map<String,String> values)
	{
		build(values);
	}
	
	public AttendanceEntry(String[] titles, String values[])
	{
    	// last first years series isttotal istavg pcchamp pcevents istqualify pcqualify
		Map<String, String> object = new HashMap<String,String>();
		for (int ii = 0; ii < titles.length; ii++)
			object.put(titles[ii], values[ii].toLowerCase());
		build(object);
	}
	
	private void build(Map<String,String> values)
	{
		series = values.get("series");
		year = Integer.parseInt(values.get("year"));
		first = values.get("first");
		last = values.get("last");
		attended = Integer.parseInt(values.get("attended"));
		champ = truths.contains(values.get("champ"));
	}
}