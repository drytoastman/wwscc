package org.wwscc.android.Results;

import android.app.Activity;
import android.content.SharedPreferences;

/** Generator for urls */
public class MobileURL 
{
	SharedPreferences prefs;
	
	public MobileURL(Activity a)
	{
		prefs = a.getSharedPreferences(null, 0);		
	}
	
	public String getBase()
	{
		return String.format("http://%s/mobile/%s/%d", 
			prefs.getString("HOST", ""), prefs.getString("SERIES", ""), prefs.getInt("EVENTID", 0));
	}
	
	public String getLast()
	{
		return String.format("%s/last?class=%s", getBase(), prefs.getString("CLASSCODE", ""));
	}
	
	public String getClass(int carid)
	{
		return String.format("%s/classlist?carid=%s", getBase(), carid);			
	}
}
