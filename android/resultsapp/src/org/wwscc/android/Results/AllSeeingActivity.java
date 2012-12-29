package org.wwscc.android.Results;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class AllSeeingActivity extends SherlockFragmentActivity implements TabListener
{
	MyPreferences prefs;
	
	@Override
	public void onCreate(Bundle inState)
	{
		super.onCreate(inState);
		setContentView(R.layout.activity_main);
		prefs = new MyPreferences(this);
		
		ActionBar b = getSupportActionBar();
		b.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		b.setDisplayShowTitleEnabled(false);
		b.addTab(b.newTab().setText("Events").setTabListener(this).setTag("settings"));
		b.addTab(b.newTab().setText("Views").setTabListener(this).setTag("views"));
		b.addTab(b.newTab().setText("Browser").setTabListener(this).setTag("browser"));
		
		long current = System.currentTimeMillis();
		long old = prefs.getLastRun();
		int lasttab = prefs.getLastTab();
		// < 12hrs, go back to last state, otherwise browse again
		if ((current < old + 43200000) && (lasttab < b.getNavigationItemCount()))
		{
			getSupportFragmentManager().executePendingTransactions();
			b.setSelectedNavigationItem(lasttab);
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) 
	{
	    super.onSaveInstanceState(outState);
	    prefs.setLastState(getSupportActionBar().getSelectedNavigationIndex(), System.currentTimeMillis());
	}
	
	private SherlockFragment createFragment(String tag)
	{
		if (tag.equals("browser"))
			return new BrowserActivity();
		else if (tag.equals("views"))
			return new ViewSetupActivity();
		else if (tag.equals("settings"))
			return new SettingsActivity();
		return null;
	}
	    
	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) 
	{
		String tag = (String)tab.getTag();
		SherlockFragment frag = (SherlockFragment)getSupportFragmentManager().findFragmentByTag(tag);
		if (frag == null)
			ft.add(R.id.main, createFragment(tag), tag);
		else
			ft.attach(frag);
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) 
	{
		ft.detach(getSupportFragmentManager().findFragmentByTag((String)tab.getTag()));
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) 
	{
	}
}
