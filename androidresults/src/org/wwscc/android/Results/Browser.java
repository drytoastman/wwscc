package org.wwscc.android.Results;

import android.os.Bundle;
import android.util.Log;
import android.app.Activity;
import android.content.Intent;

public class Browser extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.browser);
		
		Intent intent = getIntent();
		String host = intent.getStringExtra(SeriesSelector.HOST);
		String series = intent.getStringExtra(SeriesSelector.SERIES);
		
		Log.e("Browser", "created with host = " + host + ", " + series);
	}

	/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.activity_browser, menu);
		//return true;
	} */

}
