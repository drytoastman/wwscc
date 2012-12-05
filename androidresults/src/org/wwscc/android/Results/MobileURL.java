package org.wwscc.android.Results;

import android.content.SharedPreferences;

/** Generator for urls */
public class MobileURL 
{
	SharedPreferences prefs;
	
	public MobileURL(SharedPreferences sp)
	{
		prefs = sp;
	}
	
	private String getBase()
	{
		return String.format("http://%s/mobile/%s", prefs.getString("HOST", ""), prefs.getString("SERIES", ""));
	}
	
	public String getLastTime()
	{
		return String.format("%s/%s/last?class=%s", getBase(), prefs.getInt("EVENTID", 0), prefs.getString("CLASSCODE", ""));
	}
	
	public String getClassResults(int carid)
	{
		return String.format("%s/%s/classlist?carid=%s", getBase(), prefs.getInt("EVENTID", 0), carid);			
	}
	
	public String getEventList() 
	{
		return String.format("%s/events",  getBase());
	}

	public String getClassList() 
	{
		return String.format("%s/classes",  getBase());
	}
}
