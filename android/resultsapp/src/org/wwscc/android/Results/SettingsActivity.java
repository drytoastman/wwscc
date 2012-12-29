package org.wwscc.android.Results;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wwscc.android.Results.R;
import org.wwscc.services.FoundService;
import org.wwscc.services.ServiceFinder;
import org.wwscc.services.ServiceFinder.ServiceFinderListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.content.Context;


/**
 * Handles everything in the settings panel as we don't use a separate activity
 */
public class SettingsActivity extends SherlockFragment
{
	class EventWrapper
	{
		public String name;
		public int eventid;
		@Override
		public String toString() { return name; }
		public EventWrapper(String n, int i) { name = n; eventid = i; }
	}
	
    private MyPreferences prefs;
	private ProgressBar progress;
	private ServiceFinder serviceFinder;
    private FoundServiceHandler servicePipe;
    
    private Spinner series;
	private Spinner events;
	
	private ArrayAdapter<FoundService> seriesArray;
	private ArrayAdapter<EventWrapper> eventArray;
	private List<String> savedClasses;
	
	
	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View main = inflater.inflate(R.layout.activity_settings, container, false);
		
		servicePipe = new FoundServiceHandler();
        try
        {
	        serviceFinder = new ServiceFinder("RemoteDatabase");
			serviceFinder.addListener(new ServiceFinderListener() {
				@Override
				public void newService(FoundService service) {
					servicePipe.obtainMessage(1, service).sendToTarget();
			}});
        } catch (IOException ioe) {
        	Util.alert(getActivity(), "Failed to create service finder: " + ioe.getMessage());
        }
		
		series = (Spinner)main.findViewById(R.id.seriesselect);
        events = (Spinner)main.findViewById(R.id.eventselect);
        progress = (ProgressBar)main.findViewById(R.id.progressBar);
        
        seriesArray = new ServiceListAdapter(getActivity());
        seriesArray.setDropDownViewResource(R.layout.spinner_display);
        series.setAdapter(seriesArray);
        
        eventArray = new ArrayAdapter<EventWrapper>(getActivity(), R.layout.spinner_basic);
        eventArray.setDropDownViewResource(R.layout.spinner_display);
        events.setAdapter(eventArray);
        
        
        savedClasses = new ArrayList<String>();
        prefs = new MyPreferences(getActivity());
        
        series.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,int arg2, long arg3) { new UpdateSpinners().execute(); }
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
        
		Button ok = (Button)main.findViewById(R.id.okbutton);
		ok.setOnClickListener(new OnClickListener() { public void onClick(View v) { setupDone(); }});
        return main;
	}
	

    @Override
    public void onStart()
    {
    	super.onStart();
    	if (serviceFinder != null)
    		serviceFinder.start();
    }

    @Override
	public void onStop()
	{
    	super.onStop();
    	if (serviceFinder != null)
    		serviceFinder.stop();
    	seriesArray.clear();
	}	
    
	public void setupDone() 
	{
    	try
    	{
	    	FoundService selected = (FoundService)series.getSelectedItem();
	    	EventWrapper event = (EventWrapper)events.getSelectedItem();
	    	savedClasses.add("*");
	    	prefs.setCurrentEvent(
	    			selected.getHost().getHostAddress(), 
	    			selected.getId(), 
	    			event.eventid, 
	    			event.name, 
	    			savedClasses);
    	} catch (Exception e) {}

		ActionBar b = ((SherlockFragmentActivity)getActivity()).getSupportActionBar();
		b.setSelectedNavigationItem(b.getSelectedNavigationIndex()+1);				
	}

    class FoundServiceHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg) 
        {
        	FoundService fs = (FoundService)msg.obj;
        	seriesArray.add(fs);
        	seriesArray.sort(new FoundService.Compare());

        	String oldseries = prefs.getSeries();
            String oldhost = prefs.getHost();
            if ((oldseries == null) || (oldhost == null))
            	return;
        	if (fs.getHost().getHostAddress().equals(oldhost) && (fs.getId().equals(oldseries)))
        		series.setSelection(seriesArray.getPosition(fs));
        }
    };    
    
	
	static class ServiceListAdapter extends ArrayAdapter<FoundService> 
	{
		public ServiceListAdapter(Context c)
		{
			super(c, R.layout.line_foundservice);
		}
		
	    @Override
	    public View getView(int position, View v, ViewGroup parent)
	    {
	    	if (v == null)
	    	{
	    		LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            v = li.inflate(R.layout.line_foundservice, parent, false);
	            v.setTag(new TextView[] { (TextView)v.findViewById(R.id.remoteName),
	            						(TextView)v.findViewById(R.id.remoteAddress)});
	    	}
	    	
	        final FoundService service = getItem(position);
	        TextView[] fields = (TextView[])v.getTag();
	        fields[0].setText(service.getId());
	        fields[1].setText(service.getHost().getHostName());
	        return v;
	    }
	    
	    @Override
	    public View getDropDownView(int position, View convertView, ViewGroup parent)
	    {
	    	return getView(position, convertView, parent);
	    }
	}

	
	class UpdateSpinners extends AsyncTask<Void, Void, Void>
	{
		String selectedAddr;
		String selectedSeries;
		JSONArray eventsJSON;
		JSONArray classesJSON;
		
		@Override
		protected Void doInBackground(Void... params)
		{
			try {
				eventsJSON = Util.downloadJSONArray(MyPreferences.getSeriesEventsURL(selectedAddr, selectedSeries));
				classesJSON = Util.downloadJSONArray(MyPreferences.getSeriesClassesURL(selectedAddr, selectedSeries));
			} catch (Exception e) {
				Log.e("ClassSelect", "Failed to update classes/events: " + e.getMessage());
			}
			
			return null;
		}
		
		@Override
		protected void onPreExecute()
		{
			progress.setVisibility(View.VISIBLE);
			FoundService selected = (FoundService)series.getSelectedItem();
			selectedAddr = selected.getHost().getHostAddress();
			selectedSeries = selected.getId();
		}
		
		@Override
		protected void onPostExecute(Void result) 
		{
			try {
		    	int matchid = prefs.getEventId();
		    	EventWrapper foundEvent = null;
		    	
		    	eventArray.clear();
				for (int ii = 0; ii < eventsJSON.length(); ii++)
				{
					JSONObject event = eventsJSON.getJSONObject(ii);
					EventWrapper wrap = new EventWrapper(event.getString("name"), event.getInt("id"));
					if (wrap.eventid == matchid)
						foundEvent = wrap;
					eventArray.add(wrap);
				}
				
				savedClasses.clear();
				for (int ii = 0; ii < classesJSON.length(); ii++)
					savedClasses.add(classesJSON.getJSONObject(ii).getString("code"));
				
				if (foundEvent != null)
					events.setSelection(eventArray.getPosition(foundEvent));
			} catch (Exception je) {
				Util.alert(getActivity(), "Can't get event list: " + je.getMessage());  
			}
					
			progress.setVisibility(View.INVISIBLE);
	    }
	}
}
