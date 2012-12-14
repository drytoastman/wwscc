package org.wwscc.android.Results;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wwscc.android.Results.R;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

@SuppressLint("UseSparseArrays")
public abstract class JSONArrayAdapter extends BaseAdapter implements Interface.DataDest
{
	List<JSONObject> data;
	LayoutInflater inflater;
	int[] labelIds;
	int layoutId;
	
	int current_background;
	int current_foreground;
	int old_background;
	int old_foreground;
	int raw_background;
	int raw_foreground;
	int regular_background;
	int regular_foreground;
		
	public abstract void updateLabels(JSONObject o, Map<Integer, TextView> textviews) throws JSONException;
	
	public JSONArrayAdapter(Context c, int[] labels, int layout)
	{
		data = new ArrayList<JSONObject>();
		inflater = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		labelIds = labels;
		layoutId = layout;
		
		Resources r = c.getResources();
		current_background = r.getColor(R.color.current_background);
		current_foreground = r.getColor(R.color.current_foreground);
		old_background = r.getColor(R.color.old_background);
		old_foreground = r.getColor(R.color.old_foreground);
		raw_background = r.getColor(R.color.raw_background);
		raw_foreground = r.getColor(R.color.raw_foreground);
		regular_background = r.getColor(R.color.regular_background);
		regular_foreground = r.getColor(R.color.regular_foreground);
	}

	protected void updateColors(JSONObject obj,  Map<Integer, TextView> textviews) throws JSONException
	{
		int fore = regular_foreground;
		int back = regular_background;
		
		if (obj.has("label"))
        {
        	String label = obj.getString("label");
        	if (label.equals("old"))
        	{
        		back = old_background;
        		fore = old_foreground;
        	}
        	else if (label.equals("current"))
        	{
        		back = current_background;
        		fore = current_foreground;
        	}
        	else if (label.equals("raw"))
        	{
        		back = raw_background;
        		fore = raw_foreground;
        	}
        }
		
		for (TextView v : textviews.values())
		{
			v.setBackgroundColor(back);
			v.setTextColor(fore);
		}
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
        if (convertView == null) 
        {  			
    		convertView = inflater.inflate(layoutId, parent, false);
    		Map<Integer, TextView> components = new HashMap<Integer, TextView>();
    		convertView.setTag(components);
    		for (int id : labelIds)
    		{
    			TextView v = (TextView)convertView.findViewById(id);
    			v.setTextSize(20);
    			components.put(id, v);
    		}
        }
        
        Map<Integer, TextView> components = (HashMap<Integer, TextView>)convertView.getTag();
        try
        {
	        JSONObject o = getItem(position);
	        updateColors(o, components);
	        updateLabels(o, components);
        }
        catch (JSONException je)
        {
        	components.values().iterator().next().setText(je.getMessage());
        }
 
        return convertView;
	}	

	public void clear()
	{
		data.clear();
		notifyDataSetChanged();
	}
	
	public void updateData(JSONArray newData)
	{
		data.clear();
		for (int ii = 0; ii < newData.length(); ii++)
		{
			try {
				data.add(newData.getJSONObject(ii));
			} catch (JSONException e) {}
		}
		notifyDataSetChanged();
	}

	@Override
	public int getCount() 
	{
		return data.size();
	}

	@Override
	public JSONObject getItem(int position) 
	{
		return data.get(position);
	}

	@Override
	public long getItemId(int position) 
	{
		return position;
	}
}
