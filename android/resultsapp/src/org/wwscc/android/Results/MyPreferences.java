package org.wwscc.android.Results;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

public class MyPreferences 
{
	public static final String TYPE_EVENT = "Event";
	public static final String TYPE_CHAMP = "Champ";
	public static final String TYPE_PAX = "PAX";
	public static final String TYPE_RAW = "Raw";
	public static final String[] TYPES = new String[] { TYPE_EVENT, TYPE_CHAMP, TYPE_PAX, TYPE_RAW };
	
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
	/** time of last run */
	public static final String LASTRUN = "LASTRUN";
	/** what tab were we on last */
	public static final String LASTTAB = "LASTTAB";
	
	SharedPreferences store;

	public MyPreferences(Context c) 
	{
		store = c.getSharedPreferences(null, 0);
	}
	
	public long getLastRun() { return store.getLong(LASTRUN, 0); }
	public int getLastTab() { return store.getInt(LASTTAB, 0); }
	public String getSeries() { return store.getString(SERIES, ""); }
	public String getHost() { return store.getString(HOST, ""); }
	public int getEventId() { return store.getInt(EVENTID, 0); }
	public String getEventName() { return store.getString(EVENTNAME, ""); }
	public String[] getViewTypes() { return TYPES; }
	public String[] getClasses() { return store.getString(CLASSLIST, "").split(","); }
	public List<ResultsViewConfig> getViews() 
	{
		List<ResultsViewConfig> ret = new ArrayList<ResultsViewConfig>();
		for (String view : store.getString(getSeries() + VIEWEXT, "").split(","))
		{
			if (view.trim().equals("")) continue;
			ret.add(new ResultsViewConfig().decode(view));
		}
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
	
	public void setViews(List<ResultsViewConfig> views)
	{
		store.edit().putString(getSeries()+VIEWEXT, join(views)).apply();
	}

	public void setLastState(int tab, long time)
	{
		store.edit().putInt(LASTTAB, tab).putLong(LASTRUN, time).apply();
	}
	
	public void registerListener(OnSharedPreferenceChangeListener listener)
	{
		store.registerOnSharedPreferenceChangeListener(listener);
	}

	public void unregisterListener(OnSharedPreferenceChangeListener listener)
	{
		store.unregisterOnSharedPreferenceChangeListener(listener);
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
		
	public static URL getSeriesEventsURL(String host, String series) throws MalformedURLException
	{
		return new URL(String.format("http://%s/mobile/%s", host, series));
	}
	
	public static URL getSeriesClassesURL(String host, String series) throws MalformedURLException
	{
		return new URL(String.format("http://%s/mobile/%s/classes", host, series));
	}
}
