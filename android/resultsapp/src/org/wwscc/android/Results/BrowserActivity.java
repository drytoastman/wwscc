package org.wwscc.android.Results;

import java.util.List;

import org.wwscc.android.Results.MyPreferences.ResultsView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

public class BrowserActivity extends SherlockFragmentActivity
{
	DataRetriever data;
	MyPreferences prefs;
	ViewPager pager;
	ResultFragmentAdapter adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
	    prefs = new MyPreferences(this);
	    
		ActionBar b = getSupportActionBar();
		b.setTitle(prefs.getEventName());
		setContentView(R.layout.base_oneviewer);

		FragmentManager mgr = getSupportFragmentManager();

		adapter = new ResultFragmentAdapter(mgr);
		pager = (ViewPager)findViewById(R.id.pager);
        pager.setAdapter(adapter);

		
		FragmentTransaction ft = mgr.beginTransaction();
		data = (DataRetriever)mgr.findFragmentByTag("data");        
		if (data == null)
		{
			data = new DataRetriever();
			data.setRetainInstance(true);
			ft.add(data, "data");
		}
		ft.commit();		
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
			DataListFragment ret = DataListFragment.newInstance(v.type, v.classcode);
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
    
    public boolean doSetup(MenuItem item) 
	{
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
		finish();
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
	    MenuInflater inflater = getSupportMenuInflater();
	    inflater.inflate(R.menu.mainmenu, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
	    switch (item.getItemId()) 
	    {	        	
	        case R.id.view:
	        	finish(); startActivity(new Intent(this, ViewSetupActivity.class));
	            return true;
	        case R.id.setup:
	        	finish(); startActivity(new Intent(this, SettingsActivity.class));
	            return true;
	        case R.id.browse:
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
}
