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

	public boolean validURLs()
	{
		return	((prefs.getString("HOST", "").length() > 0) 
				&& (prefs.getString("SERIES", "").length() > 0)
				&& (prefs.getInt("EVENTID", 0) > 0));
	}
	
	public URL getLastTime(String classcode) throws MalformedURLException
	{
		return new URL(String.format("%s/%s/last?class=%s", getBase(), prefs.getInt("EVENTID", 0), classcode));
	}
	
	public URL getClassList(int carid) throws MalformedURLException
	{
		return new URL(String.format("%s/%s/classlist/%s", getBase(), prefs.getInt("EVENTID", 0), carid));			
	}
	
	public URL getChampList(int carid) throws MalformedURLException
	{
		return new URL(String.format("%s/%s/champlist/%s", getBase(), prefs.getInt("EVENTID", 0), carid));			
	}
	
	public URL getTopNet(int carid) throws MalformedURLException
	{
		return new URL(String.format("%s/%s/topnet/%s", getBase(), prefs.getInt("EVENTID", 0), carid));			
	}

	public URL getTopRaw(int carid) throws MalformedURLException
	{
		return new URL(String.format("%s/%s/topraw/%s", getBase(), prefs.getInt("EVENTID", 0), carid));			
	}
	
	/*
	public URL getSeriesEvents() throws MalformedURLException 
	{
		return new URL(getBase());
	}

	public URL getSeriesClasses() throws MalformedURLException 
	{
		return new URL(String.format("%s/classes",  getBase()));
	}

	public URL getSeriesIndexes() throws MalformedURLException 
	{
		return new URL(String.format("%s/indexes",  getBase()));
	}
	*/
	
	public static URL staticSeriesEvents(String addr, String series) throws MalformedURLException
	{
		return new URL(String.format("http://%s/mobile/%s", addr, series));
	}

	public static URL staticSeriesClasses(String addr, String series) throws MalformedURLException
	{
		return new URL(String.format("http://%s/mobile/%s/classes", addr, series));
	}
}
