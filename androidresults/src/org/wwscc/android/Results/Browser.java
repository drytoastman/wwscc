package org.wwscc.android.Results;

import java.io.ByteArrayOutputStream;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONObject;

import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.app.Activity;

public class Browser extends Activity 
{	
	WebView web;
	TextView lastLabel;
	NetworkThread thread;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.browser);

		web = (WebView)findViewById(R.id.webView1);
		web.setWebViewClient(new WebViewClient() {
	    	@Override
	    	public void onPageFinished(WebView view, String url) {
	    		view.setInitialScale((int)(view.getScale()*100));
	        } 			
		});
		web.getSettings().setBuiltInZoomControls(true);
		web.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		
		lastLabel = (TextView)findViewById(R.id.lastlabel);
		thread = new NetworkThread();
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		thread.start();
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		thread.stop();
	}

	
	class NetworkThread implements Runnable
	{
		boolean done = true;
		boolean outstandingRequest = false;
		long lastupdate = 0;
		Thread active = null;
		
		public void start()
		{
			lastupdate = 0;
			if (!done) return;
			try {
				if (active != null)
					active.join(); // incase it was still running after done was set to true
			} catch (InterruptedException e) {}
			done = false;
			active = new Thread(this);
			active.start();
		}
		
		public void stop()
		{
			if (active != null)
				active.interrupt();
			done = true;
		}
		
		@Override
		public void run()
		{
			AndroidHttpClient httpclient = AndroidHttpClient.newInstance("AndroidResults");
			MobileURL url = new MobileURL(Browser.this);
			
			while (!done)
			{
				try
				{
					HttpResponse response = httpclient.execute(new HttpGet(url.getLast()));
					ByteArrayOutputStream bytes = new ByteArrayOutputStream((int)response.getEntity().getContentLength());
					response.getEntity().writeTo(bytes);
				    JSONObject reply = new JSONObject(bytes.toString());
				    
				    JSONObject last = reply.getJSONArray("data").getJSONObject(0);
				    if (last.getLong("updated") > lastupdate)
				    {					    
				    	lastupdate = last.getLong("updated");
				    	lastLabel.post(new Runnable() {
							@Override
							public void run() {
								lastLabel.setText("Last update: " + lastupdate);
							}				    			
				    	});
				    	
				    	Log.e("TEST", "loading " + url.getClass(last.getInt("carid")));
				    	web.loadUrl(url.getClass(last.getInt("carid")));
				    	Thread.sleep(10000); // generally no one finishes within 10 seconds of each other
				    }
				    else
				    {
					    Thread.sleep(1500); // quicker recheck if we aren't loading anything
				    }
				}
				catch (Exception e)
				{
					Log.e("NetBrowser", "Failed to get last: " + e.getMessage());
				}
			}
			
			httpclient.close();
		}
	}

}
