/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2013 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.registration.attendance;

import java.util.Map;

public class AttendanceEntry 
{
	public String series;
	public int year;
	public String first;
	public String last;
	public int attended;
	public boolean champ;
	
	public AttendanceEntry(Map<String,String> values)
	{
		series = values.get("series");
		year = Integer.parseInt(values.get("year"));
		first = values.get("first");
		last = values.get("last");
		attended = Integer.parseInt(values.get("attended"));
		champ = Boolean.parseBoolean(values.get("champ"));
	}
}