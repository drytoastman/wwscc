package org.wwscc.android.Results;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Displays the updating class list from data
 */
public class ClassListAdapter extends JSONArrayAdapter
{
	private static final int[] labels = new int[] { R.id.position, R.id.name, R.id.sum };
	
	public ClassListAdapter(Context c) 
	{
		super(c);
	}

	public View generateView(ViewGroup parent)
	{
		return inflater.inflate(R.layout.line_eventresults, parent, false);
	}
	
	public int[] getLabels()
	{
		return labels;
	}
	
	public void updateLabels(JSONObject o) throws JSONException
	{
		components.get(R.id.position).setText(o.getString("position"));
		components.get(R.id.name).setText(o.getString("firstname") + " " + o.getString("lastname"));
		components.get(R.id.sum).setText(o.getString("sum"));
	}
}
