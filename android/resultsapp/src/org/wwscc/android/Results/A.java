package org.wwscc.android.Results;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.widget.TextView;

public class A 
{
	public static class ChampListAdapter extends JSONArrayAdapter
	{
		public ChampListAdapter(Context c) 
		{
			super(c, new int[] {  R.id.position, R.id.name, R.id.events, R.id.points }, R.layout.line_champresults);
		}

		public void updateLabels(JSONObject o, Map<Integer, TextView> textviews) throws JSONException
		{
			textviews.get(R.id.position).setText(o.getString("position"));
			textviews.get(R.id.name).setText(o.getString("firstname") + " " + o.getString("lastname"));
			textviews.get(R.id.events).setText(o.getString("events"));
			textviews.get(R.id.points).setText(o.getString("points"));
		}
	}	

	public static class EventListAdapter extends JSONArrayAdapter
	{
		public EventListAdapter(Context c) 
		{
			super(c, new int[] {  R.id.position, R.id.name, R.id.sum }, R.layout.line_eventresults);
		}

		public void updateLabels(JSONObject o, Map<Integer, TextView> textviews) throws JSONException
		{
			textviews.get(R.id.position).setText(o.getString("position"));
			textviews.get(R.id.name).setText(o.getString("firstname") + " " + o.getString("lastname"));
			textviews.get(R.id.sum).setText(o.getString("sum"));
		}
	}	

	public static class PaxListAdapter extends JSONArrayAdapter
	{
		public PaxListAdapter(Context c) 
		{
			super(c, new int[] { R.id.position, R.id.name, R.id.index, R.id.sum }, R.layout.line_paxresults);
		}

		public void updateLabels(JSONObject o, Map<Integer, TextView> textviews) throws JSONException
		{
			textviews.get(R.id.position).setText(o.getString("position"));
			textviews.get(R.id.name).setText(o.getString("name"));
			textviews.get(R.id.index).setText(o.getString("indexstr"));
			textviews.get(R.id.sum).setText(o.getString("toptime"));
		}
	}

	public static class RawListAdapter extends JSONArrayAdapter
	{
		public RawListAdapter(Context c) 
		{
			super(c, new int[] { R.id.position, R.id.name, R.id.classcode, R.id.sum }, R.layout.line_rawresults);
		}

		public void updateLabels(JSONObject o, Map<Integer, TextView> textviews) throws JSONException
		{
			textviews.get(R.id.position).setText(o.getString("position"));
			textviews.get(R.id.name).setText(o.getString("name"));
			textviews.get(R.id.classcode).setText(o.getString("classcode"));
			textviews.get(R.id.sum).setText(o.getString("toptime"));
		}
	}
}
