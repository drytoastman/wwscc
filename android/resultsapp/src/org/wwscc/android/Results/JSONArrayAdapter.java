package org.wwscc.android.Results;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

@SuppressLint("UseSparseArrays")
public abstract class JSONArrayAdapter extends BaseAdapter
{
	List<JSONObject> data;
	LayoutInflater inflater;
	Map<Integer, TextView> components;
	int current_background;
	int current_foreground;
	int old_background;
	int old_foreground;
	int raw_background;
	int raw_foreground;
	int regular_background;
	int regular_foreground;
		
	public JSONArrayAdapter(Context c)
	{
		data = new ArrayList<JSONObject>();
		inflater = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);    			
		current_background = c.getResources().getColor(R.color.current_background);
		current_foreground = c.getResources().getColor(R.color.current_foreground);
		old_background = c.getResources().getColor(R.color.old_background);
		old_foreground = c.getResources().getColor(R.color.old_foreground);
		raw_background = c.getResources().getColor(R.color.raw_background);
		raw_foreground = c.getResources().getColor(R.color.raw_foreground);
		regular_background = c.getResources().getColor(R.color.regular_background);
		regular_foreground = c.getResources().getColor(R.color.regular_foreground);
		components = new HashMap<Integer, TextView>();
	}
	
	public abstract View generateView(ViewGroup parent);
	public abstract int[] getLabels();
	public abstract void updateLabels(JSONObject o) throws JSONException;

	protected void updateColors(JSONObject obj) throws JSONException
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
		
		for (TextView v : components.values())
		{
			v.setBackgroundColor(back);
			v.setTextColor(fore);
		}
	}
	
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
        if (convertView == null) 
        {  			
    		convertView = generateView(parent);
    		components.clear();
    		for (int id : getLabels())
    		{
    			TextView v = (TextView)convertView.findViewById(id);
    			v.setTextSize(24);
    			components.put(id, v);
    		}
        }
        
        try
        {
	        JSONObject o = getItem(position);
	        updateColors(o);
	        updateLabels(o);
        }
        catch (JSONException je)
        {
        	components.values().iterator().next().setText(je.getMessage());
        }
 
        return convertView;
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
