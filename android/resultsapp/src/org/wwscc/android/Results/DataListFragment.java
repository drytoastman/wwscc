package org.wwscc.android.Results;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

public class DataListFragment extends SherlockFragment implements ResultsInterface.DataDest
{
	private ListView display;
	private JSONArrayAdapter currentAdapter;
	
	public static DataListFragment newInstance(String type, String classcode, boolean left, boolean right)
	{
		Bundle bundle = new Bundle();
		bundle.putString("classcode", classcode);
		bundle.putString("type", type);
		bundle.putString("header", classcode + " " + type);
		bundle.putBoolean("leftarrow", left);
		bundle.putBoolean("rightarrow", right);
		DataListFragment ret = new DataListFragment();
		ret.setArguments(bundle);
		return ret;
	}
	
	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View main = inflater.inflate(R.layout.fragment_list, container, false);
        display = (ListView)main.findViewById(R.id.datalist);        
        currentAdapter = null;
        Bundle args = getArguments();
        String type = args.getString("type");
        String header = args.getString("header");

        TextView ltv = (TextView)main.findViewById(R.id.leftarrow);
        TextView tv = (TextView)main.findViewById(R.id.header);
        TextView rtv = (TextView)main.findViewById(R.id.rightarrow);

        ltv.setVisibility(args.getBoolean("leftarrow", false) ? TextView.VISIBLE : TextView.INVISIBLE);
       	tv.setText(header);
        rtv.setVisibility(args.getBoolean("rightarrow", false) ? TextView.VISIBLE : TextView.INVISIBLE);
        
		if (type.equals("event")) {
			currentAdapter = new EventListAdapter(getActivity());
		} else if (type.equals("champ")) {
			currentAdapter = new ChampListAdapter(getActivity());
		} else if (type.equals("pax")) {
			currentAdapter = new PaxListAdapter(getActivity());
		} else if (type.equals("raw")) {
			currentAdapter = new RawListAdapter(getActivity());
		}

		display.setAdapter(currentAdapter);		  
        return main;
	}
				
	@Override
	public void updateData(JSONArray newData)
	{
		currentAdapter.updateData(newData);
	}

	class ChampListAdapter extends JSONArrayAdapter
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

	class EventListAdapter extends JSONArrayAdapter
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

	class PaxListAdapter extends JSONArrayAdapter
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

	class RawListAdapter extends JSONArrayAdapter
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
