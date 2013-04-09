/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2013 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.registration.attendance;

import java.util.ArrayList;
import java.util.List;

public class AttendanceResult
{
	boolean result = true;
	List<AttendanceResultPiece> pieces = new ArrayList<AttendanceResultPiece>();
	
	public static class AttendanceResultPiece
	{
		String name;
		String value;
		public AttendanceResultPiece(String n, String v)
		{
			name = n;
			value = v;
		}
	}
}