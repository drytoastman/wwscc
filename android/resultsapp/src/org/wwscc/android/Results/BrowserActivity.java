package org.wwscc.android.Results;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public class BrowserActivity extends SherlockFragmentActivity
{
	DataRetriever data;
	
    @SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
	    		
		ActionBar b = getSupportActionBar();
		b.setTitle(new MyPreferences(this).getEventName());
		
		if (getWindowManager().getDefaultDisplay().getWidth() > 600)
			setContentView(R.layout.base_twoviewers);
		else
			setContentView(R.layout.base_oneviewer);
		
		FragmentManager mgr = getSupportFragmentManager();
		FragmentTransaction ft = mgr.beginTransaction();
		data = (DataRetriever)mgr.findFragmentByTag("data");

		if (data == null)
		{
			data = new DataRetriever();
			data.setRetainInstance(true);
			ft.add(data, "data");
		}
				
		verifyList(R.id.list1, "list1", ft);
		verifyList(R.id.list2, "list2", ft);
		
		ft.commit();
	}    
	   
    private void verifyList(int rootid, String label, FragmentTransaction ft)
    {
		if (findViewById(rootid) != null)
		{
			DataListFragment frag = (DataListFragment)getSupportFragmentManager().findFragmentByTag(label);
			if (frag == null)
			{
				frag = new DataListFragment();
				Bundle bundle = new Bundle();
				bundle.putString("type", "event");
				frag.setArguments(bundle);
				ft.add(rootid, frag, label);
			}
			//frag.setDataSource(data);
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
