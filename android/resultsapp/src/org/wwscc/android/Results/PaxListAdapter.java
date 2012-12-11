package org.wwscc.android.Results;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Displays the updating class list from data
 */
public class PaxListAdapter extends JSONArrayAdapter
{
	private static final int[] labels = new int[] { R.id.position, R.id.name, R.id.index, R.id.sum };
	
	public PaxListAdapter(Context c) 
	{
		super(c);
	}

	public View generateView(ViewGroup parent)
	{
		return inflater.inflate(R.layout.line_paxresults, parent, false);
	}
	
	public int[] getLabels()
	{
		return labels;
	}
	
	public void updateLabels(JSONObject o, Map<Integer, TextView> textviews) throws JSONException
	{
		textviews.get(R.id.position).setText(o.getString("position"));
		textviews.get(R.id.name).setText(o.getString("name"));
		textviews.get(R.id.index).setText(o.getString("index"));
		textviews.get(R.id.sum).setText(o.getString("time"));
	}
}
