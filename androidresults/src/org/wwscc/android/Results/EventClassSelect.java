package org.wwscc.android.Results;

import java.io.ByteArrayOutputStream;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;

public class EventClassSelect extends Activity implements OnItemSelectedListener
{
	class EventWrapper
	{
		public String name;
		public int eventid;
		public String toString() { return name; }
		public EventWrapper(String n, int i) { name = n; eventid = i; }
	}
	
	private Spinner events;
	private Spinner classes;
	private ProgressBar progress;
	private int openRequests;
	private ArrayAdapter<EventWrapper> eventArray;
	private ArrayAdapter<String> classArray;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.eventclassselect);

        events = (Spinner)findViewById(R.id.eventselect);
        classes = (Spinner)findViewById(R.id.classselect);
        progress = (ProgressBar)findViewById(R.id.progressBar);
        openRequests = 0;
        eventArray = new ArrayAdapter<EventWrapper>(this, R.layout.basicentry);
        eventArray.setDropDownViewResource(R.layout.bigentry);
        classArray = new ArrayAdapter<String>(this, R.layout.basicentry);
        classArray.setDropDownViewResource(R.layout.bigentry);
        
        events.setAdapter(eventArray);
        classes.setAdapter(classArray);
        
        events.setOnItemSelectedListener(this);
        classes.setOnItemSelectedListener(this);
	}

	
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) 
	{
	    SharedPreferences.Editor editor = getSharedPreferences(null, 0).edit();
	    if (events.getSelectedItem() != null)
	    	editor.putInt("EVENTID", ((EventWrapper)events.getSelectedItem()).eventid);
	    if (classes.getSelectedItem() != null)
	    	editor.putString("CLASSCODE", classes.getSelectedItem().toString());
	    editor.apply();
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) 
	{				
	}
	
	/** Button click from form */
	public void openBrowser(View v)
	{
	    Intent intent = new Intent(this, Browser.class);
	    startActivity(intent);
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		SharedPreferences prefs = this.getSharedPreferences(null, 0);
		String host = prefs.getString("HOST", "unknown");
		String series = prefs.getString("SERIES", "unknown");	
		progress.setVisibility(View.VISIBLE);
		openRequests = 2;
		new EventsTask().execute(String.format("http://%s/mobile/%s/events", host, series));
		new ClassesTask().execute(String.format("http://%s/mobile/%s/classes", host, series));
	}

	
	class JSONTask extends AsyncTask<String, Integer, JSONArray>
	{			
		@Override
		protected JSONArray doInBackground(String... args) 
		{
			AndroidHttpClient httpclient = null;
			try {
				httpclient = AndroidHttpClient.newInstance("AndroidResults");
				HttpResponse response = httpclient.execute(new HttpGet(args[0]));
				ByteArrayOutputStream bytes = new ByteArrayOutputStream((int)response.getEntity().getContentLength());
				response.getEntity().writeTo(bytes);
				Log.e("JSONTask", "got the reply to " + response.getParams());
			    return new JSONArray(bytes.toString());
			} catch(Exception e) {
				Log.e("JSONTask", "download failure " + e.getMessage());
				return new JSONArray();
			} finally {
				if (httpclient != null)
					httpclient.close();
			}
		}
	}
	
	
	/** Task to download and process events */
	class EventsTask extends JSONTask {
		protected void onPostExecute(JSONArray result) {
			if (--openRequests == 0)
				progress.setVisibility(View.INVISIBLE);
			try {
				eventArray.clear();
				for (int ii = 0; ii < result.length(); ii++)
				{
					JSONObject event = result.getJSONObject(ii);
					eventArray.add(new EventWrapper(event.getString("name"), event.getInt("id")));
				}
			} catch (JSONException je) {
				Log.e("GetEvents", "failed to process: " + je.getMessage());
		}}}

	
	/** Task to download and process classes */
	class ClassesTask extends JSONTask {
		protected void onPostExecute(JSONArray result) {
			if (--openRequests == 0)
				progress.setVisibility(View.INVISIBLE);
			try {
				classArray.clear();
				for (int ii = 0; ii < result.length(); ii++)
				{
					JSONObject myclass = result.getJSONObject(ii);
					classArray.add(myclass.getString("code"));
				}
			} catch (JSONException je) {
				Log.e("GetClasses", "failed to process: " + je.getMessage());
		}}}

}
