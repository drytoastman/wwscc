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
	DataHandler inPipe = new DataHandler();
	Map<Interface.DataDest, ListenerType> requestMap;
    
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
        			break;
        		}
        	}
        }
	} 
	

	@Override
	public void onCreate(Bundle b)
	{
		super.onCreate(b);
		Log.e("TEST", "create data");
		requestMap = new HashMap<Interface.DataDest, ListenerType>();
		runner = new Runner(inPipe);
		active = new Thread(runner);
		active.start();
	}

	@Override
	public void onStart()
	{
		super.onStart();
	}
	
	@Override
	public void onStop()
	{
		super.onStop();
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		Log.e("TEST", "destroy data");
		runner.done = true;
		if (active != null)
			active.interrupt();
	}

	@Override
	public void startListening(Interface.DataDest dest, int dataType, String classcode)
	{
		Log.e("TEST", "start listening: " + dest + ", " + dataType + ", " + classcode);
		requestMap.put(dest, new ListenerType(dataType, classcode));
		runner.setRequests(requestMap.values());
	}
	
	@Override
	public void stopListening(Interface.DataDest dest)
	{
		Log.e("TEST", "stop listening: " + dest);
		requestMap.remove(dest);
		runner.setRequests(requestMap.values());
	}
	
	static class ListenerType
	{
		int type;
		String classcode;
		public ListenerType(int type, String classcode)
		{
			this.type = type;
			this.classcode = classcode;
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
		JSONArray data;
		public MessageWrapper(int t, String c)
		{
			classcode = c;
			type = t;
		}
	}
	
	class Runner implements Runnable
	{
		boolean done;
		Handler outPipe;
		Collection<ListenerType> newrequests;
		Map<String, ClassStatus> requests;

		public Runner(Handler pipe)
		{
			outPipe = pipe;
			done = false;
			requests = new HashMap<String, ClassStatus>();
			newrequests = null;
		}
		
		public synchronized void setRequests(Collection<ListenerType> collection)
		{ // will be picked up on next loop, both access to newrequests are in synchronized methods
			newrequests = collection;
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

    	@Override
		public void run()
		{
			MobileURL url = new MobileURL(getActivity().getSharedPreferences(null, 0));
			long waittime;
			
			while (!done)
			{
				try
				{
					if (!url.validURLs())
					{
						Thread.sleep(1500);
						continue;
					}

					if (newrequests != null)
						prepareNewRequests();
					
					waittime = 10000;
					
					for (ClassStatus classStatus : requests.values())
					{		
						Log.e("TEST", "Loop for " + classStatus.classcode);
					    JSONObject reply = Util.downloadJSONObject(url.getLastTime(classStatus.classcode));
					    
					    if (reply.getLong("updated") > classStatus.lastupdate)
					    {				
					    	classStatus.lastupdate = reply.getLong("updated");
					    	int carid = reply.getInt("carid");
	
					    	for (Integer type : classStatus.requests)
					    	{
						    	Log.e("TEST", "subloop for " + type);
					    		MessageWrapper msg = new MessageWrapper(type, classStatus.classcode);
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
	        				waittime = Math.min(waittime, 10000); // generally no one finishes within 10 seconds of each other
					    }
					    else
					    {
						    waittime = Math.min(waittime, 1500); // quicker recheck if we aren't loading anything
					    }
					} // for ClassStatus loop
					
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

}
