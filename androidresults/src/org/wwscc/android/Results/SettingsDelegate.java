package org.wwscc.android.Results;

import java.io.IOException;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wwscc.android.Results.NetworkStatus.NetworkWatcher;
import org.wwscc.services.FoundService;
import org.wwscc.services.ServiceFinder;
import org.wwscc.services.ServiceFinder.ServiceFinderListener;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.app.Activity;
import android.content.SharedPreferences;


/**
 * Handles everything in the settings panel as we don't use a separate activity
 */
public class SettingsDelegate implements OnItemSelectedListener, NetworkWatcher
{
	class EventWrapper
	{
		public String name;
		public int eventid;
		@Override
		public String toString() { return name; }
		public EventWrapper(String n, int i) { name = n; eventid = i; }
	}
	
	private Activity parent;
    private SharedPreferences prefs;
	private ProgressBar progress;
	private ServiceFinder serviceFinder;
    
    private Spinner series;
	private Spinner events;
	private Spinner classes;
	
	private ArrayAdapter<FoundService> seriesArray;
	private ArrayAdapter<EventWrapper> eventArray;
	private ArrayAdapter<String> classArray;
	
	
	public SettingsDelegate(Activity p) 
	{
		parent = p;
		
		series = (Spinner)parent.findViewById(R.id.seriesselect);
        events = (Spinner)parent.findViewById(R.id.eventselect);
        classes = (Spinner)parent.findViewById(R.id.classselect);
        progress = (ProgressBar)parent.findViewById(R.id.progressBar);
        
        seriesArray = new ArrayAdapter<FoundService>(parent, R.layout.basicentry);
        seriesArray.setDropDownViewResource(R.layout.bigentry);
        series.setAdapter(seriesArray);
        series.setOnItemSelectedListener(this);
        
        eventArray = new ArrayAdapter<EventWrapper>(parent, R.layout.basicentry);
        eventArray.setDropDownViewResource(R.layout.bigentry);
        events.setAdapter(eventArray);
        events.setOnItemSelectedListener(this);
        
        classArray = new ArrayAdapter<String>(parent, R.layout.basicentry);
        classArray.setDropDownViewResource(R.layout.bigentry);
        classes.setAdapter(classArray);
        classes.setOnItemSelectedListener(this);
        
        prefs = parent.getSharedPreferences(null, 0);
        
        try
        {
	        serviceFinder = new ServiceFinder("RemoteDatabase");
			serviceFinder.addListener(new ServiceFinderListener() {
				@Override
				public void newService(FoundService service) {
					mHandler.obtainMessage(1, service).sendToTarget();
			}});
        } 
        catch (IOException ioe)
        {
        	Util.alert(parent, "Failed to start service finder: " + ioe.getMessage());
        }
	}

	
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	seriesArray.add((FoundService)msg.obj);
        	seriesArray.sort(new FoundService.Compare());
        }
    };
    
    
    @Override
    public void connected()
    {
    	if (serviceFinder != null)
    		serviceFinder.start();
    }
    
    @Override
    public void disconnected()
    {
    	if (serviceFinder != null)
    		serviceFinder.stop();
    	seriesArray.clear();
    }
	
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) 
	{
		SharedPreferences.Editor editor = prefs.edit();
		 
		if (parent == series)
		{
			FoundService found = (FoundService)parent.getSelectedItem();
		    editor.putString("HOST", found.getHost().getHostAddress());
		    editor.putString("SERIES", found.getId());
		    new UpdateSpinners().execute();
		}
		else
		{	   
		    if (events.getSelectedItem() != null)
		    	editor.putInt("EVENTID", ((EventWrapper)events.getSelectedItem()).eventid);
		    if (classes.getSelectedItem() != null)
		    	editor.putString("CLASSCODE", classes.getSelectedItem().toString());
		}
		
	    editor.apply();
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {}
	

	class UpdateSpinners extends AsyncTask<Void, Void, Void>
	{
		JSONArray eventsJSON;
		JSONArray classesJSON;
		
		@Override
		protected Void doInBackground(Void... params)
		{
			try
			{
				MobileURL url = new MobileURL(prefs);
				eventsJSON = Util.downloadJSONArray(new URL(url.getEventList()));
				classesJSON = Util.downloadJSONArray(new URL(url.getClassList()));
			}
			catch (Exception e)
			{
				Log.e("ClassSelect", "Failed to update classes/events: " + e.getMessage());
			}
			
			return null;
		}
		
		@Override
		protected void onPreExecute()
		{
			progress.setVisibility(View.VISIBLE);
		}
		
		@Override
		protected void onPostExecute(Void result) 
		{
			try
			{
				eventArray.clear();
				for (int ii = 0; ii < eventsJSON.length(); ii++)
				{
					JSONObject event = eventsJSON.getJSONObject(ii);
					eventArray.add(new EventWrapper(event.getString("name"), event.getInt("id")));
				}
						
				classArray.clear();
				for (int ii = 0; ii < classesJSON.length(); ii++)
				{
					JSONObject myclass = classesJSON.getJSONObject(ii);
					classArray.add(myclass.getString("code"));
				}
			}
			catch (Exception je)
			{
				Util.alert(parent, "Can't get event or class list: " + je.getMessage());  
			}
					
			progress.setVisibility(View.INVISIBLE);
	    }
	}
}
