/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2013 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.registration.attendance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ExtraNameStorage implements NameStorage
{
	Set<Name> names;
	
	public ExtraNameStorage()
	{
		names = new HashSet<Name>();
	}
	
	public ExtraNameStorage(List<AttendanceEntry> entries)
	{
		names = new HashSet<Name>();
		for (AttendanceEntry e : entries)
			names.add(new Name(e.first, e.last));
	}
	
	@Override
	public List<Name> getNamesLike(String first, String last)
	{
		List<Name> ret = new ArrayList<Name>();
		if ((first == null) && (last == null)) return ret;
		if (first == null) first = "";
		if (last == null) last = "";
		
		for (Name n : names)
		{
			if (n.first.startsWith(first.toLowerCase()) && n.last.startsWith(last.toLowerCase()))
				ret.add(n.capitalized());
		}
		
		return ret;
	}
}