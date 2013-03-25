package org.wwscc.registration.attendance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Processor
{
	String processname;
	List<Syntax.Filter> filters; 
	List<Syntax.Comparison> comparisons;
	
	
	protected Processor(String n, List<Syntax.Filter> f, List<Syntax.Comparison> c)
	{
		processname = n;
		filters = f;
		comparisons = c;
	}
	
	public void processEntry(AttendanceEntry entry)
	{
		HashMap<String, AttendanceValues> entrants = new HashMap<String, AttendanceValues>();
		
		// apply filters and process the values provided
		for (Syntax.Filter f : filters)
		{
			if (!f.filter(entry))
				continue;
		
			String name = entry.last + ", " + entry.first;
			AttendanceValues values = entrants.get(name);
			if (values == null)
			{
				values = new AttendanceValues();
				entrants.put(name, values);
			}
			
			values.addEntry(entry);
		}

		// do final calculations on each value set and then run comparisons on each
		for (AttendanceValues v : entrants.values())
		{
			v.calculate();
		
			// run the comparisons to see if everything matches
			for (Syntax.Comparison c : comparisons)
			{
				if (!c.compare(values)
			}
		}
	}
	
}
