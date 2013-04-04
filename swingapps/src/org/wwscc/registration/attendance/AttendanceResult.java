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