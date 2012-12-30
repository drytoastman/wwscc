package org.wwscc.android.Results;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


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
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

public class BrowserFragment extends SherlockFragment
{
	DataRetriever data;
	MyPreferences prefs;
	ViewPager pager;
	
	@Override
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
		
		@SuppressWarnings("deprecation")
		int columns = (int)Math.ceil(getActivity().getWindowManager().getDefaultDisplay().getWidth() / 700.0);
		pager.setAdapter(new ResultFragmentAdapter(columns));
        
        TitlePageIndicator titleIndicator = (TitlePageIndicator)main.findViewById(R.id.titles);
        titleIndicator.setViewPager(pager);
        
		return main;
	}    
    
	@Override
	public void onStop()
	{
		super.onStop();
		data.stopListeningAll();
	}
	
	
	class ResultsPage
	{
		LinearLayout container;
		List<DataListFragment> fragments;
		String title;
		
		public ResultsPage(List<ResultsViewConfig> list)
		{
    		container = new LinearLayout(getActivity());
    		container.setOrientation(LinearLayout.HORIZONTAL);
    		
    		LayoutParams params;
    		params = new LayoutParams(0, LayoutParams.MATCH_PARENT);
    		params.weight = 1;
    		params.setMargins(3, 0, 3, 0);
    		
    		fragments = new ArrayList<DataListFragment>();
    		title = null;
    		for (ResultsViewConfig config : list)
    		{
    			DataListFragment frag = new DataListFragment(getActivity(), config);
    			container.addView(frag.getView(), params);
    			fragments.add(frag);
    			if (title == null)
        			title = String.format("%s (%s)", config.classcode, config.type);
    			else
    				title += String.format("  -  %s (%s) ", config.classcode, config.type);
    				
    		}
		}
		
		public String getTitle()
		{
			return title;
		}
		
		public void activate()
		{
			for (DataListFragment f : fragments)
			{
				if (!f.getConfig().type.equals(""))
					data.startListening(f, f.getConfig());
			}
		}
		
		public void deactivate()
		{
			for (DataListFragment f : fragments)
				data.stopListening(f);
		}
		
		public View getView()
		{
			return container;
		}
	}
	
	
	
    class ResultFragmentAdapter extends PagerAdapter
    {
    	ResultsPage displays[];
    	List<List<ResultsViewConfig>> configs;

		public ResultFragmentAdapter(int viewsPerPage) 
		{
			List<ResultsViewConfig> current = prefs.getViews();
			
			int size = (int) Math.ceil((float)current.size()/(float)viewsPerPage);
			displays = new ResultsPage[size];
			configs = new ArrayList<List<ResultsViewConfig>>();
			Iterator<ResultsViewConfig> iter = current.iterator();
			
			for (int ii = 0; ii < size; ii++)
			{
				List<ResultsViewConfig> single = new ArrayList<ResultsViewConfig>();
				configs.add(single);
				for (int jj = 0; jj < viewsPerPage; jj++)
				{
					if (iter.hasNext())
						single.add(iter.next());
					else
						single.add(new ResultsViewConfig("", ""));
				}
			}
		}
		
    	@Override
        public Object instantiateItem(ViewGroup container, int index)
        {
			if (displays[index] == null)
				displays[index] = new ResultsPage(configs.get(index));
			displays[index].activate();
			container.addView(displays[index].getView());
        	return displays[index];
        }

    	@Override
		public String getPageTitle(int index)
		{
			if (displays[index] == null)		
				displays[index] = new ResultsPage(configs.get(index));
			return displays[index].getTitle();
		}
        
        @Override
        public void destroyItem(ViewGroup container, int index, Object o)
        {
        	ResultsPage p = (ResultsPage)o;
        	container.removeView(p.getView());
        	p.deactivate();
        }
        
        @Override
        public boolean isViewFromObject(View v, Object o)
        {
        	ResultsPage p = (ResultsPage)o;
        	return (v == p.getView());
        }
		
    	@Override
		public int getCount() 
		{
    		return displays.length;
		}
    }
}
