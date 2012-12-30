package org.wwscc.android.Results;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class DataListFragment implements ResultsInterface.DataDest
{
	private ListView display;
	private JSONArrayAdapter currentAdapter;
	private ResultsViewConfig config;
		
	public DataListFragment(Context context, ResultsViewConfig rv)
	{
		display = new ListView(context);
        currentAdapter = null;
        config = rv;

        if (rv.type.equals("Event")) {
			currentAdapter = new EventListAdapter(context);
		} else if (rv.type.equals("Champ")) {
			currentAdapter = new ChampListAdapter(context);
		} else if (rv.type.equals("PAX")) {
			currentAdapter = new PaxListAdapter(context);
		} else if (rv.type.equals("Raw")) {
			currentAdapter = new RawListAdapter(context);
		} else {
			currentAdapter = new BlankAdapter(context);
		}

		display.setAdapter(currentAdapter);
	}

	public View getView()
	{
		return display;
	}
	
	public ResultsViewConfig getConfig()
	{
		return config;
	}
	
	@Override
	public void updateData(JSONArray newData)
	{
		if (currentAdapter != null) {
			currentAdapter.updateData(newData);
		} else {
			System.out.println("oops, nothing here");
		}
	}

	class ChampListAdapter extends JSONArrayAdapter
	{
		public ChampListAdapter(Context c) 
		{
			super(c, new int[] {  R.id.position, R.id.name, R.id.events, R.id.points }, R.layout.line_champresults);
		}

		@Override
		public void updateLabels(JSONObject o, Map<Integer, TextView> textviews) throws JSONException
		{
			textviews.get(R.id.position).setText(o.getString("position"));
			textviews.get(R.id.name).setText(o.getString("firstname") + " " + o.getString("lastname"));
			textviews.get(R.id.events).setText(o.getString("events"));
			textviews.get(R.id.points).setText(o.getString("points"));
		}
	}	

	class EventListAdapter extends JSONArrayAdapter
	{
		public EventListAdapter(Context c) 
		{
			super(c, new int[] {  R.id.position, R.id.name, R.id.sum }, R.layout.line_eventresults);
		}

		@Override
		public void updateLabels(JSONObject o, Map<Integer, TextView> textviews) throws JSONException
		{
			textviews.get(R.id.position).setText(o.getString("position"));
			textviews.get(R.id.name).setText(o.getString("firstname") + " " + o.getString("lastname"));
			textviews.get(R.id.sum).setText(o.getString("sum"));
		}
	}	

	class PaxListAdapter extends JSONArrayAdapter
	{
		public PaxListAdapter(Context c) 
		{
			super(c, new int[] { R.id.position, R.id.name, R.id.index, R.id.sum }, R.layout.line_paxresults);
		}

		@Override
		public void updateLabels(JSONObject o, Map<Integer, TextView> textviews) throws JSONException
		{
			textviews.get(R.id.position).setText(o.getString("position"));
			textviews.get(R.id.name).setText(o.getString("name"));
			textviews.get(R.id.index).setText(o.getString("indexstr"));
			textviews.get(R.id.sum).setText(o.getString("toptime"));
		}
	}

	class RawListAdapter extends JSONArrayAdapter
	{
		public RawListAdapter(Context c) 
		{
			super(c, new int[] { R.id.position, R.id.name, R.id.classcode, R.id.sum }, R.layout.line_rawresults);
		}

		@Override
		public void updateLabels(JSONObject o, Map<Integer, TextView> textviews) throws JSONException
		{
			textviews.get(R.id.position).setText(o.getString("position"));
			textviews.get(R.id.name).setText(o.getString("name"));
			textviews.get(R.id.classcode).setText(o.getString("classcode"));
			textviews.get(R.id.sum).setText(o.getString("toptime"));
		}
	}
	
	class BlankAdapter extends JSONArrayAdapter
	{
		public BlankAdapter(Context c) 
		{
			super(c, new int[] { R.id.position, R.id.name, R.id.classcode, R.id.sum }, R.layout.line_rawresults);
		}

		@Override
		public void updateLabels(JSONObject o, Map<Integer, TextView> textviews) throws JSONException
		{
			textviews.get(R.id.position).setText("blank");
		}
	}
}
