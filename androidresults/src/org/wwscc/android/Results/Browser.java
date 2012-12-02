package org.wwscc.android.Results;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.app.Activity;
import android.content.SharedPreferences;

public class Browser extends Activity {
	
	WebView web;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.browser);
		web = (WebView)findViewById(R.id.webView1);
		web.setWebViewClient(new WebViewClient());
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		SharedPreferences prefs = this.getSharedPreferences(null, 0);
		String host = prefs.getString("HOST", "unknown");
		String series = prefs.getString("SERIES", "unknown");
		int eventid = prefs.getInt("EVENTID", 0);
		String classcode = prefs.getString("CLASSCODE", "NONE");
		String url = String.format("http://%s/mobile/%s/%d/last?class=%s", host, series, eventid, classcode);
		Log.w("BROWSE", "Loading " + url);
		web.loadUrl(url);
	}

	/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.activity_browser, menu);
		//return true;
	} */

}
