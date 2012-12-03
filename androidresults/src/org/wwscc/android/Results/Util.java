package org.wwscc.android.Results;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Util 
{
	public static boolean networkAvailable(Activity a)
	{
		ConnectivityManager connectivityManager = (ConnectivityManager) a.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return ((activeNetworkInfo != null) && activeNetworkInfo.isConnected());
	}
}
