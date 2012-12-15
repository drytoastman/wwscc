package org.wwscc.android.Results;

import org.json.JSONArray;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockFragment;

public class DataListFragment extends SherlockFragment implements OnItemSelectedListener, Interface.DataDest
{
	private static final String[] TYPES =  new String[] {"event", "champ", "pax", "raw"};
	
    private Spinner classes;
	private Spinner types;
	private ListView display;
	private JSONArrayAdapter currentAdapter;
	private int currentType;
	private Interface.DataSource retriever;
	
	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View main = inflater.inflate(R.layout.fragment_list, container, false);

		classes = (Spinner)main.findViewById(R.id.classselect);
        types = (Spinner)main.findViewById(R.id.typeselect);
        display = (ListView)main.findViewById(R.id.datalist);
        
        ArrayAdapter<String> classList = new ArrayAdapter<String>(getActivity(), R.layout.spinner_basic);
        classList.setDropDownViewResource(R.layout.spinner_display);
        String available[] = getActivity().getSharedPreferences(null, 0).getString("CLASSES", "").split(",");
        for (String s : available)
        	classList.add(s);
        classes.setAdapter(classList);
        classes.setOnItemSelectedListener(this);

        ArrayAdapter<String> typesList = new ArrayAdapter<String>(getActivity(), R.layout.spinner_basic);
        typesList.setDropDownViewResource(R.layout.spinner_display);
        for (String s : TYPES)
        	typesList.add(s);
        types.setAdapter(typesList);
        types.setOnItemSelectedListener(this);
        
        currentAdapter = null;
        currentType = 0;
        return main;
	}
		
	@Override
    public void onActivityCreated(Bundle inState) 
	{
        super.onActivityCreated(inState);
		SharedPreferences prefs = getActivity().getSharedPreferences(null, 0);
		
		int csel = prefs.getInt(getId()+"classSel", classes.getSelectedItemPosition());
		if (csel < classes.getCount())
			classes.setSelection(csel);
		
        int tsel = prefs.getInt(getId()+"typeSel", types.getSelectedItemPosition());
		if (tsel < types.getCount())
			types.setSelection(tsel);
	}
	
	public void setDataSource(Interface.DataSource d)
	{
		retriever = d;
	}

	@Override
	public void onStop()
	{
		super.onStop();
		if (retriever != null)
			retriever.stopListening(this);
	}
	
	@Override
	public void updateData(JSONArray newData)
	{
		currentAdapter.updateData(newData);
	}
	
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
	{
		if (parent == types)
		{
			String type = (String)types.getSelectedItem();
			if (type.equals("event")) {
				currentAdapter = new A.EventListAdapter(getActivity());
				currentType = DataRetriever.EVENTRESULT;
			} else if (type.equals("champ")) {
				currentAdapter = new A.ChampListAdapter(getActivity());
				currentType = DataRetriever.CHAMPRESULT;
			} else if (type.equals("pax")) {
				currentAdapter = new A.PaxListAdapter(getActivity());
				currentType = DataRetriever.TOPNET;
			} else if (type.equals("raw")) {
				currentAdapter = new A.RawListAdapter(getActivity());
				currentType = DataRetriever.TOPRAW;
			}
			display.setAdapter(currentAdapter);
		}
		else // class selection
		{
			if (currentAdapter != null)
				currentAdapter.clear();
		}
	
		SharedPreferences.Editor prefs = getActivity().getSharedPreferences(null, 0).edit();
		prefs.putInt(getId()+"classSel", classes.getSelectedItemPosition());
		prefs.putInt(getId()+"typeSel", types.getSelectedItemPosition());
		prefs.apply();
		
		if ((retriever != null) && (currentType > 0))
		{
			retriever.startListening(this, currentType, (String)classes.getSelectedItem());
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {}

}
