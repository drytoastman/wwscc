package org.wwscc.android.Results;

import org.json.JSONObject;

import android.content.Context;
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
		try {
			if (active != null)
				active.join(); // incase it was still running after done was set to true
		} catch (InterruptedException e) {}
		done = false;
		active = new Thread(new Runner());
		active.start();
	}
	
	public void stop()
	{
		if (active != null)
			active.interrupt();
		done = true;
	}
	
	class Runner implements Runnable
	{
		@Override
		public void run()
		{
			MobileURL url = new MobileURL(context.getSharedPreferences(null, 0));
			
			while (!done)
			{
				try
				{
				    JSONObject reply = Util.downloadJSONObject(url.getLastTime());
				    if (reply.getLong("updated") > lastupdate)
				    {					    
				    	lastupdate = reply.getLong("updated");
				    	int carid = reply.getInt("carid");
				    	JSONObject data = Util.downloadJSONObject(url.getEntrantResults(carid));
				    	listener.obtainMessage(ENTRANT_DATA, data).sendToTarget();
				    	Thread.sleep(10000); // generally no one finishes within 10 seconds of each other
				    }
				    else
				    {
					    Thread.sleep(1500); // quicker recheck if we aren't loading anything
				    }
				}
				catch (Exception e)
				{
					Log.e("NetBrowser", "Failed to get last: " + e.getMessage());
					try {
						Thread.sleep(6000);  // keep out of super loop
					} catch (InterruptedException e1) {}
				}
			}
		}
	}
}
