package org.wwscc.android.Results;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;

public class Util 
{
	public static JSONObject downloadJSONObject(URL loc) throws JSONException, IOException
	{
		return new JSONObject(downloadJSON(loc));
	}
	
	public static JSONArray downloadJSONArray(URL loc) throws JSONException, IOException
	{
		return new JSONArray(downloadJSON(loc));
	}
	
	private static JSONTokener downloadJSON(URL loc) throws IOException
	{
		URLConnection conn = loc.openConnection();
		InputStream is = conn.getInputStream();
		
		Log.i("Util", "Downloading " + conn.getContentLength() + " bytes of json");
		ByteArrayOutputStream bytes = new ByteArrayOutputStream(conn.getContentLength());
		byte buffer[] = new byte[512];
		int read;
		while ((read = is.read(buffer)) >= 0) {
			bytes.write(buffer, 0, read);
		}
		
	    return new JSONTokener(bytes.toString());
	}
	
	public static void alert(Context c, String msg) 
	{
    	new AlertDialog.Builder(c).setTitle("Alert").setMessage(msg).show();
	}
}
