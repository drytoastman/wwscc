package org.wwscc.android.Results;

import java.util.ArrayList;
import java.util.List;

import org.wwscc.android.Results.MyPreferences.ResultsView;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class ViewSetupFragment extends SherlockFragment
{
	ListView mainList;
	SettingAdapter settings;
	MyPreferences prefs;
	
	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View main = inflater.inflate(R.layout.activity_viewsetup, container, false);
		
		settings = new SettingAdapter(getActivity(), R.layout.line_classsetting);
		mainList = (ListView)main.findViewById(R.id.list);
		mainList.setAdapter(settings);
		prefs = new MyPreferences(getActivity());

		Button add = (Button)main.findViewById(R.id.addbutton);
		add.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				settings.add(new ResultsView(prefs.getClasses()[0], prefs.getViewTypes()[0]));				
		}});

		Button ok = (Button)main.findViewById(R.id.okbutton);
		ok.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ActionBar b = ((SherlockFragmentActivity)getActivity()).getSupportActionBar();
				b.setSelectedNavigationItem(b.getSelectedNavigationIndex()+1);				
		}});
		
		return  main;
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		settings.clear();
		for (ResultsView v : prefs.getViews())
			settings.add(v);
	}
	
	/** pull all the current options and write to preferences */
	public void rewriteOptions()
	{
		List<ResultsView> collection = new ArrayList<ResultsView>();
		for (int ii = 0; ii < settings.getCount(); ii++)
			collection.add(settings.getItem(ii));
		prefs.setViews(collection);
	}

	
	class SettingAdapter extends ArrayAdapter<ResultsView> implements OnItemSelectedListener
	{
		public SettingAdapter(Context context, int textViewResourceId) 
		{
			super(context, textViewResourceId);
		}

		@Override
		public void onItemSelected(AdapterView<?> spinner, View v, int position, long id) 
		{
			int p[] = (int[])spinner.getTag();
			String s = (String)spinner.getItemAtPosition(position);
			ResultsView setting = getItem(p[1]);
			
			if (p[0] == 0)
				setting.classcode = s;
			else
				setting.type = s;
			
			rewriteOptions();
		}

		@Override
		public void onNothingSelected(AdapterView<?> spinner) {}
		
		private void spinnerSetup(Spinner spinner, String[] strings)
		{
    		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_basic, strings);
    		adapter.setDropDownViewResource(R.layout.spinner_display);
    		spinner.setAdapter(adapter);
    		spinner.setOnItemSelectedListener(this);
		}
		
		@SuppressWarnings("unchecked")
		public View getView(int position, View convertView, ViewGroup parent)
		{
	        if (convertView == null) 
	        {
	            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    		convertView = inflater.inflate(R.layout.line_classsetting, parent, false);
	    		Spinner cSelect = (Spinner)convertView.findViewById(R.id.classselect);
	    		spinnerSetup(cSelect, prefs.getClasses());
	    		Spinner tSelect = (Spinner)convertView.findViewById(R.id.typeselect);
	    		spinnerSetup(tSelect, prefs.getViewTypes());
	    		Button button = (Button)convertView.findViewById(R.id.deleteentry);
	    		button.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						remove(getItem((Integer)v.getTag()));
						rewriteOptions();
				}});
	    		View[] components = new View[] { cSelect, tSelect, button }; 
	    		convertView.setTag(components);
	        }
	        
	        View components[] = (View[])convertView.getTag();
	        Spinner cSelect = (Spinner)components[0];
	        Spinner tSelect = (Spinner)components[1];
	        Button button = (Button)components[2];
	        
		    ResultsView setting = getItem(position);
		    cSelect.setSelection(((ArrayAdapter<String>)cSelect.getAdapter()).getPosition(setting.classcode));
		    cSelect.setTag(new int[] { 0, position });
		    tSelect.setSelection(((ArrayAdapter<String>)tSelect.getAdapter()).getPosition(setting.type));
		    tSelect.setTag(new int[] { 1, position });
		    button.setTag(position);
		    
	        return convertView;
		}
	}
}

