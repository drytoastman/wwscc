package org.wwscc.android.Results;

import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Handler;
import android.util.Log;

public class DataRetriever
{
	public static final int ENTRANT_DATA = 42;
	public static final int TOPTIME_DATA = 43;
	
	Context context;
	boolean done = true;
	boolean outstandingRequest = false;
	long lastupdate = 0;
	Thread active = null;
	Handler listener = null;
	
	public DataRetriever(Context c, Handler h)
	{
		context = c;
		listener = h;
	}
	
	public void start()
	{
		if (!done) return;
		lastupdate = 0;
		done = false;
		active = new Thread(new Runner());
		active.start();
	}
	
	public void stop()
	{
		done = true;
		if (active != null)
			active.interrupt();
	}
	
	class Runner implements Runnable, OnSharedPreferenceChangeListener
	{
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) 
		{
			lastupdate = 0;  // if the prefs changed, reset our couter so we rerequest things
			active.interrupt();
		}
		
		@Override
		public void run()
		{
			SharedPreferences prefs = context.getSharedPreferences(null, 0);
			prefs.registerOnSharedPreferenceChangeListener(this);			
			MobileURL url = new MobileURL(prefs);
			
			while (!done)
			{
				try
				{
					if (!url.validURLs())
					{
						lastupdate = 0;
						Thread.sleep(500);
						continue;
					}
					
				    JSONObject reply = Util.downloadJSONObject(url.getLastTime());
				    if (reply.getLong("updated") > lastupdate)
				    {					    
				    	lastupdate = reply.getLong("updated");
				    	int carid = reply.getInt("carid");
				    	JSONObject data = Util.downloadJSONObject(url.getEntrantResults(carid));
				    	listener.obtainMessage(ENTRANT_DATA, data).sendToTarget();
				    	JSONObject ttdata = Util.downloadJSONObject(url.getTopTimes(carid));
				    	listener.obtainMessage(TOPTIME_DATA, ttdata).sendToTarget();
				    	Thread.sleep(10000); // generally no one finishes within 10 seconds of each other
				    }
				    else
				    {
					    Thread.sleep(1500); // quicker recheck if we aren't loading anything
				    }
				}
				catch (InterruptedException ie) {}
				catch (Exception e)
				{
					Log.e("NetBrowser", "Failed to get last: " + e.getMessage());
					try { Thread.sleep(4000);  // keep out of super loop
					} catch (InterruptedException e1) {}
				}
			}
		}
	}
}
