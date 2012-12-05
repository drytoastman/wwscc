package org.wwscc.android.Results;

import java.io.ByteArrayOutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONObject;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.util.Log;

public class DataRetriever
{
	Context context;
	boolean done = true;
	boolean outstandingRequest = false;
	long lastupdate = 0;
	Thread active = null;
	
	public DataRetriever(Context c)
	{
		context = c;
	}
	
	public void start()
	{
		lastupdate = 0;
		if (!done) return;
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
			AndroidHttpClient httpclient = AndroidHttpClient.newInstance("AndroidResults");
			MobileURL url = new MobileURL(context.getSharedPreferences(null, 0));
			
			while (!done)
			{
				try
				{
					HttpResponse response = httpclient.execute(new HttpGet(url.getLastTime()));
					ByteArrayOutputStream bytes = new ByteArrayOutputStream((int)response.getEntity().getContentLength());
					response.getEntity().writeTo(bytes);
				    JSONObject reply = new JSONObject(bytes.toString());
				    
				    JSONObject last = reply.getJSONArray("data").getJSONObject(0);
				    if (last.getLong("updated") > lastupdate)
				    {					    
				    	lastupdate = last.getLong("updated");				    	
				    	Log.e("TEST", "loading " + url.getClassResults(last.getInt("carid")));
				    	//web.loadUrl(url.getClassResults(last.getInt("carid")));
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
				}
			}
			
			httpclient.close();
		}
	}
}