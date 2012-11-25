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

import org.wwscc.services.FoundService;
import org.wwscc.services.ServiceFinder;
import org.wwscc.services.ServiceFinder.ServiceFinderListener;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current chat session.
 */
public class Viewer extends Activity 
{
    private ListView serviceList;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Initialize the array adapter for the conversation thread
        serviceListAdapter = new ArrayAdapter<FoundService>(this, R.layout.message);
        serviceList = (ListView) findViewById(R.id.in);
        serviceList.setAdapter(serviceListAdapter);

        try {
			serviceFinder = new ServiceFinder("RemoteDatabase");
			serviceFinder.addListener(new ServiceFinderListener() {
				@Override
				public void newService(FoundService service) {
						mHandler.obtainMessage(1, service).sendToTarget();
					
				} });
		} catch (IOException e) {
			Log.e("Viewer", "Failed to create service finder " + e.getMessage());
            Toast.makeText(this, "can't create finder", Toast.LENGTH_LONG).show();
		}
    }


    @Override
    public void onStart() {
        super.onStart();        
        serviceFinder.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        serviceFinder.stop();
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
