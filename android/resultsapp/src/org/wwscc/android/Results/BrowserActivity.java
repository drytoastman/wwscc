package org.wwscc.android.Results;

import java.util.ArrayList;
import java.util.List;

import org.wwscc.android.Results.MyPreferences.ResultsView;

import com.actionbarsherlock.app.SherlockFragment;
import com.viewpagerindicator.TitlePageIndicator;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class BrowserActivity extends SherlockFragment
{
	DataRetriever data;
	MyPreferences prefs;
	ViewPager pager;
	
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
	    prefs = new MyPreferences(getActivity());
        
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
		data = (DataRetriever)getChildFragmentManager().findFragmentByTag("data");        
		if (data == null)
		{
			data = new DataRetriever();
			data.setRetainInstance(true);
			ft.add(data, "data");
		}
		ft.commit();		
	}
	
	
	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{		
		View main = inflater.inflate(R.layout.base_oneviewer, container, false);
		pager = (ViewPager)main.findViewById(R.id.pager);
		pager.setAdapter(new ResultFragmentAdapter());
        
        TitlePageIndicator titleIndicator = (TitlePageIndicator)main.findViewById(R.id.titles);
        titleIndicator.setViewPager(pager);
        
		return main;
	}    
    

	@Override
	public void onDestroyView() 
	{
		System.out.println("destroy view");
		super.onDestroyView();
		data.stopListeningAll();
	}

	
    class ResultFragmentAdapter extends PagerAdapter
    {
    	List<ResultsView> views;
    	DataListFragment displays[];

		public ResultFragmentAdapter() 
		{
			views = prefs.getViews();
			displays = new DataListFragment[views.size()];
		}
		
    	@Override
        public Object instantiateItem(ViewGroup container, int index)
        {
			ResultsView v = views.get(index);
			if (displays[index] == null)
				displays[index] = new DataListFragment(getActivity(), v.type);
			data.startListening(displays[index], v.type, v.classcode);
			container.addView(displays[index].getDisplay());
        	return displays[index];
        }

    	@Override
		public String getPageTitle(int index)
		{
			ResultsView v = views.get(index);
			return String.format("%s (%s)", v.classcode, v.type);
		}
        
        @Override
        public void destroyItem(ViewGroup container, int index, Object o)
        {
        	System.out.println("destroy " + index);
        	DataListFragment f = (DataListFragment)o;
        	container.removeView((View)f.getDisplay());
        	data.stopListening(f);
        }
        
        @Override
        public boolean isViewFromObject(View v, Object o)
        {
        	DataListFragment f = (DataListFragment)o;
        	return (v == f.getDisplay());
        }
		
    	@Override
		public int getCount() 
		{
			return views.size();
		}
    }
}
