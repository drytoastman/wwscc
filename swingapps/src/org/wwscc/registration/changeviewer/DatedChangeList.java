package org.wwscc.registration.changeviewer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.wwscc.storage.Change;

class DatedChangeList
{
	int index;
	Date date;
	List<Change> changes;
	
	public DatedChangeList(int ii, long mod, List<Change> list)
	{
		index = ii;
		date = new Date(mod);
		changes = list;
	}
	
	public String toString() 
	{
		if (index == 0) return "Active";
		return String.format("Archive %s - %s", index, new SimpleDateFormat("MMM dd HH:mm").format(date)); 
	}
}