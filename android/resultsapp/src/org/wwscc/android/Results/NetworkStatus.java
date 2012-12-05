package org.wwscc.android.Results;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkStatus extends BroadcastReceiver 
{
	NetworkWatcher watcher;
	Activity source;

	public static interface NetworkWatcher
	{
		public void connected();
		public void disconnected();
	}
	
	public NetworkStatus()
	{
		super();
	}
	
	public void register(Activity source, NetworkWatcher watcher)
	{
        source.registerReceiver(this,  new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        this.source = source;
		this.watcher = watcher;
	}
	
	public void unregister()
	{
		source.unregisterReceiver(this);
	}
	
	public boolean isConnected()
	{
	    ConnectivityManager conn =  (ConnectivityManager)source.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo networkInfo = conn.getActiveNetworkInfo();
	    if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
	    	return true;
	    } else {
	        return false;
	    }
	}
	
	@Override
	public void onReceive(Context context, Intent intent) 
	{
		if (isConnected())
			watcher.connected();
		else
			watcher.disconnected();
	}
}
