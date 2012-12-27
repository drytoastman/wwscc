package org.wwscc.android.Results;

import java.util.List;

import org.wwscc.android.Results.MyPreferences.ResultsView;

import com.actionbarsherlock.app.SherlockFragment;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class BrowserActivity extends SherlockFragment
{
	DataRetriever data;
	MyPreferences prefs;
	ViewPager pager;
	ResultFragmentAdapter adapter;
	
	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
	    prefs = new MyPreferences(getActivity());
	    
		View main = inflater.inflate(R.layout.base_oneviewer, container, false);

		adapter = new ResultFragmentAdapter(getChildFragmentManager());
		pager = (ViewPager)main.findViewById(R.id.pager);
        pager.setAdapter(adapter);

		
		FragmentTransaction ft = getChildFragmentManager().beginTransaction();
		data = (DataRetriever)getChildFragmentManager().findFragmentByTag("data");        
		if (data == null)
		{
			data = new DataRetriever();
			data.setRetainInstance(true);
			ft.add(data, "data");
		}
		ft.commit();
		
		return main;
	}    
    
    
    class ResultFragmentAdapter extends FragmentPagerAdapter
    {
    	List<ResultsView> views;
    	
		public ResultFragmentAdapter(FragmentManager fm) 
		{
			super(fm);
			views = prefs.getViews();
		}

		@Override
		public Fragment getItem(int index) 
		{
			ResultsView v = views.get(index);		
			DataListFragment ret = DataListFragment.newInstance(v.type, v.classcode, index > 0, index < views.size()-1);
			data.startListening(ret, v.type, v.classcode);
			System.out.println("getitem " + index);
			return ret;
		}

		@Override
		public int getCount() 
		{
			return prefs.getViews().size();
		}
    	
    }
}
