package org.wwscc.android.Results;

import org.wwscc.services.FoundService;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


public class ServiceListAdapter extends ArrayAdapter<FoundService> 
{
	public ServiceListAdapter(Context c)
	{
		super(c, R.layout.foundserviceentry);
	}
	
    @Override
    public View getView(int position, View v, ViewGroup parent)
    {
    	if (v == null)
    	{
    		LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = li.inflate(R.layout.foundserviceentry, parent, false);
            v.setTag(new TextView[] { (TextView)v.findViewById(R.id.remoteName),
            						(TextView)v.findViewById(R.id.remoteAddress)});
    	}
    	
        final FoundService service = getItem(position);
        TextView[] fields = (TextView[])v.getTag();
        fields[0].setText(service.getId());
        fields[1].setText(service.getHost().getHostName());
        return v;
    }
}
