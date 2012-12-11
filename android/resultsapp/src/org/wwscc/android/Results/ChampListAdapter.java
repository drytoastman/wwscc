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
public class ChampListAdapter extends JSONArrayAdapter
{
	private static final int[] labels = new int[] { R.id.position, R.id.name, R.id.events, R.id.points };
	
	public ChampListAdapter(Context c) 
	{
		super(c);
	}

	public View generateView(ViewGroup parent)
	{
		return inflater.inflate(R.layout.line_champresults, parent, false);
	}
	
	public int[] getLabels()
	{
		return labels;
	}
	
	public void updateLabels(JSONObject o, Map<Integer, TextView> textviews) throws JSONException
	{
		textviews.get(R.id.position).setText(o.getString("position"));
		textviews.get(R.id.name).setText(o.getString("firstname") + " " + o.getString("lastname"));
		textviews.get(R.id.events).setText(o.getString("events"));
		textviews.get(R.id.points).setText(o.getString("points"));
	}
}
