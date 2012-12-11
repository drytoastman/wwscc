package org.wwscc.android.Results;

import java.net.MalformedURLException;
import java.net.URL;

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
	
	public URL getLastTime() throws MalformedURLException
	{
		return new URL(String.format("%s/%s/last?class=%s", getBase(), prefs.getInt("EVENTID", 0), prefs.getString("CLASSCODE", "")));
	}
	
	public URL getEntrantResults(int carid) throws MalformedURLException
	{
		return new URL(String.format("%s/%s/entrant/%s", getBase(), prefs.getInt("EVENTID", 0), carid));			
	}

	public URL getTopTimes(int carid) throws MalformedURLException
	{
		return new URL(String.format("%s/%s/toptimes/%s", getBase(), prefs.getInt("EVENTID", 0), carid));			
	}
	
	public URL getEventList() throws MalformedURLException 
	{
		return new URL(getBase());
	}

	public URL getClassList() throws MalformedURLException 
	{
		return new URL(String.format("%s/classes",  getBase()));
	}

	public URL getIndexList() throws MalformedURLException 
	{
		return new URL(String.format("%s/indexes",  getBase()));
	}
}
