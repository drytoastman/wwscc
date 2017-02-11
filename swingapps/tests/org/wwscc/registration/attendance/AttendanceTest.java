/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2013 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.registration.attendance;

import java.io.File;
import java.util.List;
import org.junit.Assert;

import org.junit.Test;

/**
 *
 */
public class AttendanceTest
{
	@Test
	public void testPunchcardIST() throws Exception
	{
		List<AttendanceCalculation> calcs = Syntax.scanAll("PunchCard = Series(nwr,pro); championships=0; MinYear(2011); maxyearcount<=4; \n\nISTClass = totalevents<=9; avgyearcount<=3");
		List<AttendanceEntry> entries = Attendance.scanFile(new File("tests/org/wwscc/registration/attendance/attendance.csv"));
		AttendanceResult result = calcs.get(0).getResult("Brett", "Wilson", entries);
		Assert.assertEquals("Series(nwr,pro)", result.pieces.get(0).name);
		Assert.assertEquals("championships", result.pieces.get(1).name);
		Assert.assertEquals("5.000", result.pieces.get(1).value);
		Assert.assertEquals("MinYear(2011)", result.pieces.get(2).name);
		Assert.assertEquals("maxyearcount", result.pieces.get(3).name);
		Assert.assertEquals("3.000", result.pieces.get(3).value);
		Assert.assertEquals(false, result.result);

		result = calcs.get(1).getResult("Brett", "Wilson", entries);
		Assert.assertEquals("totalevents", result.pieces.get(0).name);
		Assert.assertEquals("104.000", result.pieces.get(0).value);
		Assert.assertEquals("avgyearcount", result.pieces.get(1).name);
		Assert.assertEquals("9.455", result.pieces.get(1).value);
		Assert.assertEquals(false, result.result);
	}
}
