package org.wwscc.android.Results;


import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.wwscc.android.Results.NetworkStatus.NetworkWatcher;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

public class Browser extends SherlockFragmentActivity implements NetworkWatcher, TabListener
{	
	DataHandler dataHandler;
	DataRetriever dataRetrieval;
	SettingsFragment settings;
	NetworkStatus networkStatus;
	boolean requireDataRequests;
	
	ClassListAdapter classlist;
	ChampListAdapter champlist;
	PaxListAdapter paxlist;
	RawListAdapter rawlist;
	Map<String, PreLoadListFragment> fragments;

    @Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		settings = new SettingsFragment();
		classlist = new ClassListAdapter(this);
		champlist = new ChampListAdapter(this);
		paxlist = new PaxListAdapter(this);
		rawlist = new RawListAdapter(this);
		
		fragments = new HashMap<String, PreLoadListFragment>();
		fragments.put(getString(R.string.button_event), new PreLoadListFragment(classlist));
		fragments.put(getString(R.string.button_champ), new PreLoadListFragment(champlist));
		fragments.put(getString(R.string.button_pax), new PreLoadListFragment(paxlist));
		fragments.put(getString(R.string.button_raw), new PreLoadListFragment(rawlist));

		
		FragmentManager mgr = getSupportFragmentManager();
		FragmentTransaction trans = mgr.beginTransaction();
		trans.add(R.id.container, settings, "settings");
		trans.commit();

		dataHandler = new DataHandler();
		dataRetrieval = new DataRetriever(this, dataHandler);
		networkStatus = new NetworkStatus();
		requireDataRequests = false;

		ActionBar b = getSupportActionBar();
		b.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
	    b.setDisplayShowTitleEnabled(false);
		b.addTab(b.newTab().setTabListener(this).setText(R.string.button_setup).setTag(settings));
		b.addTab(b.newTab().setTabListener(this).setText(R.string.button_event));
		b.addTab(b.newTab().setTabListener(this).setText(R.string.button_champ));
		b.addTab(b.newTab().setTabListener(this).setText(R.string.button_pax));
		b.addTab(b.newTab().setTabListener(this).setText(R.string.button_raw));
	}


	class DataHandler extends Handler
	{
        @Override
        public void handleMessage(Message msg) 
        {
        	JSONObject obj = (JSONObject)msg.obj;
        	try 
        	{
	        	switch (msg.what)
	        	{
	        		case DataRetriever.ENTRANT_DATA: 
						classlist.updateData(obj.getJSONArray("classlist"));
						champlist.updateData(obj.getJSONArray("champlist"));
	        			break;
	        		case DataRetriever.TOPTIME_DATA:
						paxlist.updateData(obj.getJSONArray("topnet"));
						rawlist.updateData(obj.getJSONArray("topraw"));						
	        			break;
	        	}
			} catch (JSONException e) {
				e.printStackTrace();
			}
        }
	}
	
	
    @Override
    public void connected()
    {
    	if (requireDataRequests) dataRetrieval.start();
    }
    
    @Override
    public void disconnected()
    {
    	if (requireDataRequests) dataRetrieval.stop();
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


	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft)
	{
		Object o = tab.getTag();
		if (o instanceof Fragment)
		{
			ft.attach((Fragment)o);
		}
		else
		{
			PreLoadListFragment plf = fragments.get(tab.getText().toString());
			if (plf != null)
			{
				ft.add(R.id.container, plf);
				tab.setTag(plf);
			}
		}
		
		requireDataRequests = (o != settings);
		if (requireDataRequests)
			dataRetrieval.start();
		else
			dataRetrieval.stop();
	}


	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft)
	{
		Object o = tab.getTag();
		if (o instanceof Fragment)
			ft.detach((Fragment)o);
	}


	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft)
	{
	}
}
