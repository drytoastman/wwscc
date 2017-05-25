/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2013 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.registration.attendance;

public class Name implements Comparable<Name>
{
	String first;
	String last;
	public Name(String f, String l) 
	{ 
		first = f; 
		last = l; 
	}
	
	public Name capitalized()
	{
		throw new UnsupportedOperationException("Need to reimplement getAttendance if anyone uses it anymore");
		//return new Name(WordUtils.capitalize(first), WordUtils.capitalize(last));
	}
	
	public String toString() 
	{ 
		return String.format("%s %s", first, last); 
	}
	

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((last == null) ? 0 : last.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}
		if (!(obj instanceof Name))
		{
			return false;
		}
		Name other = (Name) obj;
		if (first == null)
		{
			if (other.first != null)
			{
				return false;
			}
		} else if (!first.equals(other.first))
		{
			return false;
		}
		if (last == null)
		{
			if (other.last != null)
			{
				return false;
			}
		} else if (!last.equals(other.last))
		{
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(Name o)
	{
		return (first+last).compareTo(o.first+o.last);
	}
}