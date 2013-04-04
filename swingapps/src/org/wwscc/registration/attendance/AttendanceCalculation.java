/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2013 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.registration.attendance;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.wwscc.util.NF;


/**
 * Represents a single boolean value that the user wishes to calculate from available attenance values
 * Calculated values are:
 * 	totalyears
 * 	totalseries
 * 	championships
 * 	minyearcount
 * 	maxyearcount
 * 	avgyearcount
 * 	totalevents
 * Other values:
 *	activeyears
 *	activeseries
 */

public class AttendanceCalculation implements Comparable<AttendanceCalculation>
{
	String processname;
	List<Object> processors; 
	
	protected AttendanceCalculation(String n)
	{
		processname = n;
		processors = new ArrayList<Object>();
	}
	
	protected void add(Object processor)
	{
		processors.add(processor);
	}
	
	
	public AttendanceResult getResult(String first, String last, List<AttendanceEntry> entries)
	{		
		// do final calculations on each value set and then run comparisons on each
		boolean needcaclulations = true;
		CalculatedAttendanceValues current = null;
		AttendanceResult ret = new AttendanceResult();
		
		entries = filterName(first, last, entries);
		
		for (Object o : processors)
		{
			if (o instanceof Syntax.Filter)
			{
				// filter out more entries from the list of entries, note need for new calculations
				Syntax.Filter filter = (Syntax.Filter)o;
				filterList(filter, entries);
				ret.pieces.add(new AttendanceResult.AttendanceResultPiece(filter.getName(), null));
				needcaclulations = true;
			}
			
			else if (o instanceof Syntax.Comparison) 
			{
				// make sure we do calculations if necessary and then run comparison and note results
				Syntax.Comparison comparison = (Syntax.Comparison)o;
				if (needcaclulations)
					current = runCalculations(entries);
				Double value = current.values.get(comparison.getName());
				if (!comparison.compare(value))
					ret.result = false;
				ret.pieces.add(new AttendanceResult.AttendanceResultPiece(comparison.getName(), NF.format(value)));
				needcaclulations = false;
			}
		}

		return ret;
	}

	/**
	 * Filter out the entries matching the name, note that we create a new list so we can work with our own copy
	 * @param first the first name
	 * @param last the last name
	 * @param entries the list of incoming entries
	 * @return a new list that contains only entries with the particular name
	 */
	private List<AttendanceEntry> filterName(String first, String last, List<AttendanceEntry> entries)
	{
		List<AttendanceEntry> ret = new ArrayList<AttendanceEntry>();
		String f = first.toLowerCase();
		String l = last.toLowerCase();
		for (AttendanceEntry entry : entries)
		{
			if (entry.first.equals(f) && entry.last.equals(l))
				ret.add(entry);
		}
		return ret;
	}
	
	/**
	 * Filter out entries that don't match the provided filter, works directly on the list provided
	 * @param filter the filter object
	 * @param entries the list of entries to filter
	 */
	private void filterList(Syntax.Filter filter, List<AttendanceEntry> entries)
	{
		ListIterator<AttendanceEntry> li = entries.listIterator();
		while (li.hasNext())
		{
			if (!filter.filter(li.next()))
				li.remove();
		}
	}
	
	/**
	 * Run the calculations on the provided set of entries
	 * @param entries the current entry set
	 * @return a AttendanceValues objects with all the calculated values
	 */
	private CalculatedAttendanceValues runCalculations(List<AttendanceEntry> entries)
	{
		CalculatedAttendanceValues values = new CalculatedAttendanceValues();
		for (AttendanceEntry entry : entries)
			values.addEntry(entry);
		values.calculate();
		return values;
	}
	
	
	@Override
	public int compareTo(AttendanceCalculation arg0)
	{
		return processname.compareTo(arg0.processname);
	}
}


