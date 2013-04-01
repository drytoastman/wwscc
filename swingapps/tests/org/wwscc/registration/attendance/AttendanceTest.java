/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2013 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.registration.attendance;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

/**
 *
 */
public class AttendanceTest
{
	@Test
	public void testPunchcard() throws Exception
	{
		AttendanceCalculation a = Syntax.scan("PunchCard = Series(nwr,pro); MinYear(2011); maxyearcount<=4; championships=0");
		Attendance.readFile(new File("tests/org/wwscc/registration/attendance/attendance.csv"), Arrays.asList(new AttendanceCalculation[] {a}));
		Map<String,Double> m = a.getResult("wilson, brett");
		Assert.assertEquals(m.get("totalevents"), 17.0);
		Assert.assertEquals(m.get("avgyearcount"), 4.25);
		Assert.assertEquals(m.get("calculation"), 0.0);
	}
	
	@Test
	public void testIST() throws Exception
	{
		AttendanceCalculation b = Syntax.scan("ISTClass = totalevents<=9; avgyearcount<=3");
		Attendance.readFile(new File("tests/org/wwscc/registration/attendance/attendance.csv"), Arrays.asList(new AttendanceCalculation[] {b}));
		Map<String,Double> m = b.getResult("wilson, brett");
		Assert.assertEquals(m.get("totalevents"), 23.0);
		Assert.assertEquals(m.get("avgyearcount"), 5.75);
		Assert.assertEquals(m.get("calculation"), 0.0);
	}

}
