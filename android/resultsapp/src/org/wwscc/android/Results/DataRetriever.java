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

public class DataRetriever extends SherlockFragment implements Interface.DataSource
{		
	public static final int EVENTRESULT = 100;
	public static final int CHAMPRESULT = 101;
	public static final int TOPNET = 102;
	public static final int TOPRAW = 103;
	
	Thread active;
	Runner runner;
	Map<Interface.DataDest, ListenerType> requestMap;
	Map<ListenerType, MessageWrapper> cache;
    
	class DataHandler extends Handler
	{
        @Override
        public void handleMessage(Message msg) 
        {
        	MessageWrapper wrap = (MessageWrapper)msg.obj;
        	for (Interface.DataDest toUpdate : requestMap.keySet())
        	{
        		ListenerType t = requestMap.get(toUpdate);
        		if ((wrap.type == t.type) && (wrap.classcode.equals(t.classcode)))
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
		requestMap = new HashMap<Interface.DataDest, ListenerType>();
		cache = new HashMap<ListenerType, MessageWrapper>();
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
	public void startListening(Interface.DataDest dest, int dataType, String classcode)
	{
		ListenerType t = new ListenerType(dataType, classcode);
		if (cache.containsKey(t))
			dest.updateData(cache.get(t).data);
		ListenerType old = requestMap.put(dest, t);
		if (!t.equals(old)) // don't update if we just overwrote with the same thing
			runner.setRequests(requestMap.values());
	}
	
	@Override
	public void stopListening(Interface.DataDest dest)
	{
		requestMap.remove(dest);
		runner.setRequests(requestMap.values());
	}
	
	
	class Runner implements Runnable
	{
		boolean done;
		Handler outPipe;
		Collection<ListenerType> newrequests;
		Map<String, ClassStatus> requests;
		MobileURL url;

		public Runner(Handler pipe)
		{
			outPipe = pipe;
			done = false;
			requests = new HashMap<String, ClassStatus>();
			newrequests = null;
			url = new MobileURL(getActivity().getSharedPreferences(null, 0));
		}
		
		public synchronized void setRequests(Collection<ListenerType> collection)
		{ // will be picked up on next loop, both access to newrequests are in synchronized methods
			newrequests = collection;
			active.interrupt();
		}
		
		private synchronized void prepareNewRequests()
		{ // also synchronized, formats are new requests
			requests.clear();
			for (ListenerType t : newrequests)
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
			    JSONObject reply = Util.downloadJSONObject(url.getLastTime(classStatus.classcode));			    
			    if (reply.getLong("updated") > classStatus.lastupdate)
			    {				
			    	classStatus.lastupdate = reply.getLong("updated");
			    	int carid = reply.getInt("carid");
	
			    	for (Integer type : classStatus.requests)
			    	{  // try and shortcut times when start back up and cached is already up to date
			    		MessageWrapper old = cache.get(new ListenerType(type, classStatus.classcode));
			    		if ((old != null) && (old.updatetime == classStatus.lastupdate))
			    			continue;
			    		
			    		MessageWrapper msg = new MessageWrapper(type, classStatus.classcode, classStatus.lastupdate);
			    		switch (type)
			    		{
			    			case EVENTRESULT:
			    				msg.data = Util.downloadJSONArray(url.getClassList(carid));
			    				break;
			    			case CHAMPRESULT:
			    				msg.data = Util.downloadJSONArray(url.getChampList(carid));
			    				break;
			    			case TOPNET:
			    				msg.data = Util.downloadJSONArray(url.getTopNet(carid));
			    				break;
			    			case TOPRAW:
			    				msg.data = Util.downloadJSONArray(url.getTopRaw(carid));
			    				break;
			    		}
			    		if (msg.data != null)
			    			outPipe.obtainMessage(msg.type, msg).sendToTarget();	    	
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
					if (url.validURLs())
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
	static class ListenerType
	{
		int type;
		String classcode;
		public ListenerType(int type, String classcode)
		{
			this.type = type;
			this.classcode = classcode;
		}
		
		@Override
		public int hashCode() 
		{ 
			return type + classcode.hashCode(); 
		}
		
		@Override
		public boolean equals(Object o) 
		{
			if (o instanceof ListenerType)
			{
				ListenerType t = (ListenerType)o;
				return (t.classcode.equals(classcode) && (t.type == type));
			}
			return false;
		}
	}

	static class ClassStatus
	{
		String classcode;
		long lastupdate;
		Set<Integer> requests;

		public ClassStatus(String code)
		{
			classcode = code;
			lastupdate = 0;
			requests = new HashSet<Integer>();
		}
	}
	
	static class MessageWrapper
	{
		int type;
		String classcode;
		long updatetime;
		JSONArray data;
		
		public MessageWrapper(int t, String c, long u)
		{
			classcode = c;
			type = t;
			updatetime = u;
		}
	}
}
