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
	
	@Override
	public void onCreate(Bundle savedInstance)
	{
		super.onCreate(savedInstance);
		setContentView(R.layout.activity_main);
		
		ActionBar b = getSupportActionBar();
		b.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		b.addTab(b.newTab().setText("Events").setTabListener(this).setTag("settings"));
		b.addTab(b.newTab().setText("Views").setTabListener(this).setTag("views"));
		b.addTab(b.newTab().setText("Browser").setTabListener(this).setTag("browser"));
	}
	    
	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) 
	{
		String tag = (String)tab.getTag();
		SherlockFragment frag = (SherlockFragment)getSupportFragmentManager().findFragmentByTag(tag);
		if (frag == null)
		{
			if (tag.equals("browser"))
				ft.add(R.id.main, new BrowserActivity(), tag);
			else if (tag.equals("views"))
				ft.add(R.id.main, new ViewSetupActivity(), tag);
			else if (tag.equals("settings"))
				ft.add(R.id.main, new SettingsActivity(), tag);
		}
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
