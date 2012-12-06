package org.wwscc.android.Results;


import org.wwscc.android.Results.NetworkStatus.NetworkWatcher;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

public class Browser extends SherlockFragmentActivity implements NetworkWatcher, TabListener
{	
	DataRetriever dataRetrieval;
	SettingsFragment settings;
	NetworkStatus networkStatus;
	boolean settingsActive;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		FragmentManager mgr = getSupportFragmentManager();
		settings = new SettingsFragment();
		mgr.beginTransaction().add(R.id.container, settings, "settings").commit();
		
		ActionBar b = getSupportActionBar();
		b.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
	    b.setDisplayShowTitleEnabled(false);
		b.addTab(b.newTab().setTabListener(this).setText(R.string.button_setup).setTag(settings));
		b.addTab(b.newTab().setTabListener(this).setText(R.string.button_event));
		b.addTab(b.newTab().setTabListener(this).setText(R.string.button_champ));
		b.addTab(b.newTab().setTabListener(this).setText(R.string.button_pax));
		
		dataRetrieval = new DataRetriever(this);
		networkStatus = new NetworkStatus();
		settingsActive = true;
	}


    @Override
    public void connected()
    {
    }
    
    @Override
    public void disconnected()
    {
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
		if (o instanceof SherlockFragment)
			ft.attach((SherlockFragment)o);		
	}


	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft)
	{
		Object o = tab.getTag();
		if (o instanceof SherlockFragment)
			ft.detach((SherlockFragment)o);
	}


	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft)
	{
	}
}
