package org.wwscc.android.Results;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import com.actionbarsherlock.app.SherlockListFragment;

/**
 * List fragment that I can set the list adapter on before its visible.
 */
public class PreLoadListFragment extends SherlockListFragment
{
	ListAdapter saveAdapter;
	public PreLoadListFragment(ListAdapter a)
	{
		saveAdapter = a;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        setListAdapter(saveAdapter);
        return v;
    }			
}
