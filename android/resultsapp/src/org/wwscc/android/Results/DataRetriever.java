package org.wwscc.android.Results;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.actionbarsherlock.app.SherlockFragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class DataRetriever extends SherlockFragment implements ResultsInterface.DataSource
{		
	public static final int EVENTRESULT = 100;
	public static final int CHAMPRESULT = 101;
	public static final int TOPNET = 102;
	public static final int TOPRAW = 103;
	
	Thread active;
	Runner runner;
	Map<ResultsInterface.DataDest, ResultsViewConfig> requestMap;
	Map<ResultsViewConfig, MessageWrapper> cache;
    
	class DataHandler extends Handler
	{
        @Override
        public void handleMessage(Message msg) 
        {
        	MessageWrapper wrap = (MessageWrapper)msg.obj;
        	for (ResultsInterface.DataDest toUpdate : requestMap.keySet())
        	{
        		ResultsViewConfig t = requestMap.get(toUpdate);
        		if ((wrap.type.equals(t.type)) && (wrap.classcode.equals(t.classcode)))
        		{
        			toUpdate.updateData(wrap.data);
        			cache.put(t, wrap);
        		}
        	}
        }
	} 
	

	@Override
	public void onCreate(Bundle b)
	{
		super.onCreate(b);
		Log.i("Data", "Creating thread");
		requestMap = new HashMap<ResultsInterface.DataDest, ResultsViewConfig>();
		cache = new HashMap<ResultsViewConfig, MessageWrapper>();
		runner = new Runner(new DataHandler());
		active = new Thread(runner);
		active.setDaemon(true);
		active.start();
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		Log.i("Data", "Killing thread");
		requestMap.clear();
		cache.clear();
		runner.done = true;
		if (active != null)
			active.interrupt();
	}

	@Override
	public void startListening(ResultsInterface.DataDest dest, ResultsViewConfig type)
	{
		if (cache.containsKey(type))
			dest.updateData(cache.get(type).data);
		ResultsViewConfig old = requestMap.put(dest, type);
		if (!type.equals(old)) // don't update if we just overwrote with the same thing
			runner.setRequests(requestMap.values());
	}
	
	@Override
	public void stopListening(ResultsInterface.DataDest dest)
	{
		requestMap.remove(dest);
		runner.setRequests(requestMap.values());
	}
	
	@Override
	public void stopListeningAll()
	{
		requestMap.clear();
		runner.setRequests(requestMap.values());
	}
	
	class Runner implements Runnable
	{
		boolean done;
		Handler outPipe;
		Collection<ResultsViewConfig> newrequests;
		Map<String, ClassStatus> requests;
		MyPreferences prefs;

		public Runner(Handler pipe)
		{
			outPipe = pipe;
			done = false;
			requests = new HashMap<String, ClassStatus>();
			newrequests = null;
			prefs = new MyPreferences(getActivity());
		}
		
		public synchronized void setRequests(Collection<ResultsViewConfig> collection)
		{ // will be picked up on next loop, both access to newrequests are in synchronized methods
			newrequests = collection;
			active.interrupt();
		}
		
		private synchronized void prepareNewRequests()
		{ // also synchronized, formats are new requests
			requests.clear();
			for (ResultsViewConfig t : newrequests)
			{
				if (!requests.containsKey(t.classcode))
					requests.put(t.classcode, new ClassStatus(t.classcode));
				requests.get(t.classcode).requests.add(t.type);
			}
			newrequests = null;
		}

		private int doClass(ClassStatus classStatus)
		{
			try
			{
				Log.i("Data", "Loop for " + classStatus.classcode + ", updated " + classStatus.lastupdate);
			    JSONObject reply = Util.downloadJSONObject(prefs.getLastTimeURL(classStatus.classcode));			    
			    if (reply.getLong("updated") > classStatus.lastupdate)
			    {				
			    	classStatus.lastupdate = reply.getLong("updated");
			    	int carid = reply.getInt("carid");
	
			    	for (String type : classStatus.requests)
			    	{  // try and shortcut times when start back up and cached is already up to date
			    		MessageWrapper old = cache.get(new ResultsViewConfig(classStatus.classcode, type));
			    		if ((old != null) && (old.updatetime == classStatus.lastupdate))
			    			continue;
			    		
			    		MessageWrapper msg = new MessageWrapper(type, classStatus.classcode, classStatus.lastupdate);
			    		if (type.equals(MyPreferences.TYPE_EVENT))
			    			msg.data = Util.downloadJSONArray(prefs.getClassListURL(carid));
			    		else if (type.equals(MyPreferences.TYPE_CHAMP))
			    			msg.data = Util.downloadJSONArray(prefs.getChampListURL(carid));
			    		else if (type.equals(MyPreferences.TYPE_PAX))
		    				msg.data = Util.downloadJSONArray(prefs.getTopNetURL(carid));
			    		else if (type.equals(MyPreferences.TYPE_RAW))
		    				msg.data = Util.downloadJSONArray(prefs.getTopRawURL(carid));

		    			if (msg.data != null)
			    			outPipe.obtainMessage(0, msg).sendToTarget();	    	
			    	}
					
			    	return 10000; // generally no one finishes within 10 seconds of each other
			    }
			    
			    return 2000;
			}
			catch (Exception e)
			{
				Log.e("Data", "Error in processing for " + classStatus.classcode);
				return 5000;
			}
		}
		
		
    	@Override
		public void run()
		{
			while (!done)
			{
				try
				{
					if (newrequests != null)
						prepareNewRequests();
					
					long waittime = 10000;
					if (prefs.validURLs())
						for (ClassStatus classStatus : requests.values())
							waittime = Math.min(waittime, doClass(classStatus));
					
					Thread.sleep(waittime);
				}
				catch (InterruptedException ie) {}
				catch (Exception e)
				{
					Log.e("NetBrowser", "Failed in loop: " + e.getMessage());
					try { Thread.sleep(4000);  // keep out of super loop
					} catch (InterruptedException e1) {}
				}
			}
		}
	}

	/*** ********************************************************************************* ***/
	// Support and wrapper classes

	static class ClassStatus
	{
		String classcode;
		long lastupdate;
		Set<String> requests;

		public ClassStatus(String code)
		{
			classcode = code;
			lastupdate = 0;
			requests = new HashSet<String>();
		}
	}
	
	static class MessageWrapper
	{
		String type;
		String classcode;
		long updatetime;
		JSONArray data;
		
		public MessageWrapper(String t, String c, long u)
		{
			classcode = c;
			type = t;
			updatetime = u;
		}
	}
}
