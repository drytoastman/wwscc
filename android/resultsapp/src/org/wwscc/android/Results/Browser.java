package org.wwscc.android.Results;


import org.wwscc.android.Results.NetworkStatus.NetworkWatcher;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.app.Activity;

public class Browser extends Activity implements NetworkWatcher 
{	
	DataRetriever dataRetrieval;
	ListView table;
	SettingsDelegate settings;
	NetworkStatus networkStatus;
	boolean settingsActive;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.browser);
		
		table = (ListView)findViewById(R.id.listView);
		settings = new SettingsDelegate(this);
		dataRetrieval = new DataRetriever(this);
		networkStatus = new NetworkStatus();
		settingsActive = true;
	}


    @Override
    public void connected()
    {
    	if (settingsActive)
    		settings.connected();
    	else
    		dataRetrieval.start();
    }
    
    @Override
    public void disconnected()
    {
    	if (settingsActive)
    		settings.disconnected();
    	else
    		dataRetrieval.stop();
    }
    
	
	@Override
	protected void onResume()
	{
		super.onResume();
		Log.d("MAIN", "++RESUME");
		networkStatus.register(this, this);
    	if (!networkStatus.isConnected())
        	Util.alert(this, "No network, will try to search when available.");        
    	else
    		connected();
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		Log.e("MAIN", "++PAUSE");
    	disconnected();
    	networkStatus.unregister();
	}

	public void buttonSetup(View v)
	{
		Log.w("TEST", "Setup");
		settingsActive = true;
	}
	
	public void buttonClass(View v)
	{
		Log.w("TEST", "Class");
		settingsActive = false;
	}
	
	public void buttonChamp(View v)
	{
		Log.w("TEST", "Champ");
		settingsActive = false;
	}
	
	public void buttonPAX(View v)
	{
		Log.w("TEST", "PAX");
		settingsActive = false;
	}


}
