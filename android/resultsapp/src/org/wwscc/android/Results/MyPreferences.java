package org.wwscc.android.Results;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;

public class MyPreferences 
{
	private static final String[] TYPES = new String[] { "Event", "Champ", "PAX", "Raw" };
	/** IP address string of selected series */
	public static final String HOST = "HOST";
	/** string name of selected series */
	public static final String SERIES = "SERIES";
	/** string name of selected event */
	public static final String EVENTNAME = "EVENTNAME";
	/** integer id of selected event */
	public static final String EVENTID = "EVENTID";
	/** classes available in selected event */
	public static final String CLASSLIST = "CLASSLIST";
	/** string formatted list of view types for the series, uses ${CLASSLIST}VIEWEXT */
	public static final String VIEWEXT = ".VIEW";

	public static class ResultsView 
	{
		String classcode, type;
		public ResultsView()
		{
			classcode = "";
			type = "";
		}
		
		public ResultsView(String c, String t) 
		{ 
			classcode = c; 
			type = t; 
		}
		
		public String toString()
		{
			return classcode+"/"+type;
		}
		
		public ResultsView decode(String s)
		{
			String p[] = s.split("/");
			classcode = p[0];
			type = p[1];
			return this;
		}
	}
	

	SharedPreferences store;

	public MyPreferences(Context c) 
	{
		store = c.getSharedPreferences(null, 0);
	}
	
	public String getSeries() { return store.getString(SERIES, ""); }
	public String getHost() { return store.getString(HOST, ""); }
	public int getEventId() { return store.getInt(EVENTID, 0); }
	public String getEventName() { return store.getString(EVENTNAME, ""); }
	public String[] getViewTypes() { return TYPES; }
	public String[] getClasses() { return store.getString(CLASSLIST, "").split(","); }
	public List<ResultsView> getViews() 
	{
		List<ResultsView> ret = new ArrayList<ResultsView>();
		for (String view : store.getString(getSeries() + VIEWEXT, "").split(","))
			ret.add(new ResultsView().decode(view));
		return ret;
	}
	
	public void setCurrentEvent(String host, String series, int eventid, String eventname, List<String> classlist)
	{
		SharedPreferences.Editor edit = store.edit();
		edit.putString(HOST, host);
		edit.putString(SERIES, series);
		edit.putInt(EVENTID, eventid);
		edit.putString(EVENTNAME, eventname);
		edit.putString(CLASSLIST, join(classlist));
		edit.apply();
	}
	
	public void setViews(List<ResultsView> views)
	{
		SharedPreferences.Editor edit = store.edit();
		edit.putString(getSeries()+VIEWEXT, join(views));
		edit.apply();
	}

	
    public static String join(String c[])
    {
    	if (c.length == 0) return "";
    	StringBuilder b = new StringBuilder(c[0]);
    	for (int ii = 0; ii < c.length; ii++)
    		b.append(',').append(c[ii]);
    	return b.toString();
    }
	
    public static String join(List<? extends Object> c)
    {
    	if (c.size() == 0) return "";
    	Iterator<? extends Object> iter = c.iterator();
    	StringBuilder b = new StringBuilder(iter.next().toString());
    	while(iter.hasNext())
    		b.append(',').append(iter.next().toString());
    	return b.toString();
    }
    
    
	/********** URL creation ***************/
	
	private String getURLBase()
	{
		return String.format("http://%s/mobile/%s", getHost(), getSeries());
	}

	public boolean validURLs()
	{
		return ((getHost().length() > 0)  && (getSeries().length() > 0) && (getEventId() > 0));
	}
	
	public URL getLastTimeURL(String classcode) throws MalformedURLException
	{
		return new URL(String.format("%s/%s/last?class=%s", getURLBase(), getEventId(), classcode));
	}
	
	public URL getClassListURL(int carid) throws MalformedURLException
	{
		return new URL(String.format("%s/%s/classlist/%s", getURLBase(), getEventId(), carid));			
	}
	
	public URL getChampListURL(int carid) throws MalformedURLException
	{
		return new URL(String.format("%s/%s/champlist/%s", getURLBase(), getEventId(), carid));			
	}
	
	public URL getTopNetURL(int carid) throws MalformedURLException
	{
		return new URL(String.format("%s/%s/topnet/%s", getURLBase(), getEventId(), carid));			
	}

	public URL getTopRawURL(int carid) throws MalformedURLException
	{
		return new URL(String.format("%s/%s/topraw/%s", getURLBase(), getEventId(), carid));			
	}
		
	public URL seriesEventsURL() throws MalformedURLException
	{
		return new URL(getURLBase());
	}
	
	public URL getSeriesClassesURL() throws MalformedURLException
	{
		return new URL(String.format("%s/classes", getURLBase()));
	}
}
