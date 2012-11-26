/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wwscc.android.Results;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.wwscc.services.FoundService;
import org.wwscc.services.ServiceFinder;
import org.wwscc.services.ServiceFinder.ServiceFinderListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current chat session.
 */
public class SeriesSelector extends Activity 
{
	public final static String HOST = "org.wwscc.android.Results.HOST";
	public final static String SERIES = "org.wwscc.android.Results.SERIES";
	
	private static final String LABEL = "Viewer";
    private ListView serviceList;
    private EditText editHost, editSeries;
    private ArrayAdapter<FoundService> serviceListAdapter;
    private ServiceFinder serviceFinder = null;

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
             serviceListAdapter.add((FoundService)msg.obj);
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
    	Log.e(LABEL, "++CREATE");
    	Logger.getLogger(ServiceFinder.class.getCanonicalName()).setLevel(Level.WARNING);
        setContentView(R.layout.main);

        // Initialize the array adapter for the conversation thread
        serviceListAdapter = new ArrayAdapter<FoundService>(this, R.layout.remoteentry)
        {
            @Override
            public View getView(int position, View v, ViewGroup parent)
            {
            	if (v == null)
            	{
            		LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = li.inflate(R.layout.remoteentry, parent, false);
                    v.setTag(new TextView[] { (TextView)v.findViewById(R.id.remoteName),
                    						(TextView)v.findViewById(R.id.remoteAddress)});
            	}
            	
                final FoundService service = getItem(position);
                TextView[] fields = (TextView[])v.getTag();
                fields[0].setText(service.getId());
                fields[1].setText(service.getHost().getHostName());
                return v;
            }
        };

        editSeries = (EditText)findViewById(R.id.EditSeries);
        editHost = (EditText)findViewById(R.id.EditHost);

        serviceList = (ListView)findViewById(R.id.SeriesList);
        serviceList.setAdapter(serviceListAdapter);
        serviceList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        
        serviceList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	FoundService s = serviceListAdapter.getItem(position);
                editSeries.setText(s.getId());
                editHost.setText(s.getHost().getHostName());
                openSeries(null); // simulate the button press
            }
        }); 
       
        
        try {
			serviceFinder = new ServiceFinder("RemoteDatabase");
			serviceFinder.addListener(new ServiceFinderListener() {
				@Override
				public void newService(FoundService service) {
						Log.e(LABEL, "new service " + service);
						mHandler.obtainMessage(1, service).sendToTarget();
					
				} });
		} catch (IOException e) {
			Log.e(LABEL, "Failed to create service finder " + e.getMessage());
            Toast.makeText(this, "can't create finder", Toast.LENGTH_LONG).show();
		}
    }


    public void openSeries(View v)
    {
		Log.e(LABEL, "open viewer with " + editHost.getText() + "/" + editSeries.getText());
	    Intent intent = new Intent(this, Browser.class);
	    intent.putExtra(HOST, editHost.getText().toString());
	    intent.putExtra(SERIES, editSeries.getText().toString());
	    startActivity(intent);
	}

    
    @Override
    public void onStart() {
        super.onStart();
    	Log.e(LABEL, "++START");
    	if (serviceFinder != null)
    		serviceFinder.start();
    	else
    		Log.e(LABEL, "Huh, no service finder");
    }

    @Override
    public void onStop() {
        super.onStop();
    	Log.e(LABEL, "++STOP");
    	if (serviceFinder != null)
    		serviceFinder.stop();
    	else
    		Log.e(LABEL, "still no service finder");
        serviceListAdapter.clear();
    }
        

    public void onDestroy() {
    	super.onDestroy();
    	Log.e(LABEL, "++DESTROY");
    }
    
    /*
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
         }
    } 
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
        case R.id.secure_connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            return true;
        case R.id.insecure_connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
            return true;
        }
        return false;
    } */

}
