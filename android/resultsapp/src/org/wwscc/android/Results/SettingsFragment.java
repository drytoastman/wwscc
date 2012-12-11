package org.wwscc.android.Results;

import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wwscc.services.FoundService;
import org.wwscc.services.ServiceFinder;
import org.wwscc.services.ServiceFinder.ServiceFinderListener;

import com.actionbarsherlock.app.SherlockFragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.content.Context;
import android.content.SharedPreferences;


/**
 * Handles everything in the settings panel as we don't use a separate activity
 */
public class SettingsFragment extends SherlockFragment implements OnItemSelectedListener
{
	class EventWrapper
	{
		public String name;
		public int eventid;
		@Override
		public String toString() { return name; }
		public EventWrapper(String n, int i) { name = n; eventid = i; }
	}
	
	//private Activity parent;
    private SharedPreferences prefs;
	private ProgressBar progress;
	private ServiceFinder serviceFinder;
    private FoundServiceHandler servicePipe;
    
    private Spinner series;
	private Spinner events;
	private Spinner classes;
	
	private ArrayAdapter<FoundService> seriesArray;
	private ArrayAdapter<EventWrapper> eventArray;
	private ArrayAdapter<String> classArray;
	
	public SettingsFragment()
	{
	}
	
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	    servicePipe = new FoundServiceHandler();
        try
        {
	        serviceFinder = new ServiceFinder("RemoteDatabase");
			serviceFinder.addListener(new ServiceFinderListener() {
				@Override
				public void newService(FoundService service) {
					servicePipe.obtainMessage(1, service).sendToTarget();
			}});
        } 
        catch (IOException ioe)
        {
        	Util.alert(getActivity(), "Failed to create service finder: " + ioe.getMessage());
        }
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View ret = inflater.inflate(R.layout.fragment_settings, container, false);
		
		series = (Spinner)ret.findViewById(R.id.seriesselect);
        events = (Spinner)ret.findViewById(R.id.eventselect);
        classes = (Spinner)ret.findViewById(R.id.classselect);
        progress = (ProgressBar)ret.findViewById(R.id.progressBar);
        
        seriesArray = new ServiceListAdapter(getActivity());
        seriesArray.setDropDownViewResource(R.layout.spinner_display);
        series.setAdapter(seriesArray);
        series.setOnItemSelectedListener(this);
        
        eventArray = new ArrayAdapter<EventWrapper>(getActivity(), R.layout.spinner_basic);
        eventArray.setDropDownViewResource(R.layout.spinner_display);
        events.setAdapter(eventArray);
        events.setOnItemSelectedListener(this);
        
        classArray = new ArrayAdapter<String>(getActivity(), R.layout.spinner_basic);
        classArray.setDropDownViewResource(R.layout.spinner_display);
        classes.setAdapter(classArray);
        classes.setOnItemSelectedListener(this);
        
        prefs = getActivity().getSharedPreferences(null, 0);
        return ret;
	}

    class FoundServiceHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg) 
        {
        	seriesArray.add((FoundService)msg.obj);
        	seriesArray.sort(new FoundService.Compare());
        }
    };    
    
    @Override
    public void onResume()
    {
    	super.onResume();
    	if (serviceFinder != null)
    		serviceFinder.start();
    }
    
    @Override
    public void onPause()
    {
    	super.onPause();
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
				eventsJSON = Util.downloadJSONArray(url.getEventList());
				classesJSON = Util.downloadJSONArray(url.getClassList());
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
				Util.alert(getActivity(), "Can't get event or class list: " + je.getMessage());  
			}
					
			progress.setVisibility(View.INVISIBLE);
	    }
	}
	
	
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
}
