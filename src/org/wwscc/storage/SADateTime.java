/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2009 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQLite doesn't have builtin types for Date, Timestmp, Datetime but
 * sqlalchmey does its own storage format for it.  These classes allow
 * reading/writing of the SQLAlchmey formatting via a wrapper around
 * Calendar.
 */
public class SADateTime implements Serializable
{
	static Pattern dtread = Pattern.compile("(\\d+)-(\\d+)-(\\d+)(?: (\\d+):(\\d+):(\\d+)(?:\\.(\\d+))?)?");

	protected Calendar date;

	public SADateTime()
	{
		date = Calendar.getInstance();
		date.setTime(new Date());
	}

	public SADateTime(long seconds)
	{
		date = Calendar.getInstance();
		date.setTimeInMillis(seconds * 1000);
	}

	public SADateTime(String s)
	{
		this();
		Matcher m = dtread.matcher(s);
		if (m.lookingAt())
		{
			date.set(Calendar.YEAR, Integer.parseInt(m.group(1)));
			date.set(Calendar.MONTH, Integer.parseInt(m.group(2))-1);
			date.set(Calendar.DAY_OF_MONTH, Integer.parseInt(m.group(3)));
			if (m.groupCount() > 3)
			{
				date.set(Calendar.HOUR_OF_DAY, Integer.parseInt(m.group(4)));
				date.set(Calendar.MINUTE, Integer.parseInt(m.group(5)));
				date.set(Calendar.SECOND, Integer.parseInt(m.group(6)));
			}
		}
	}

	public long getSeconds()
	{
		return (date.getTimeInMillis() / 1000);
	}

	@Override
	public String toString()
	{
		return String.format("%04d-%02d-%02d %02d:%02d:%02d.0",
					date.get(Calendar.YEAR), date.get(Calendar.MONTH)+1, date.get(Calendar.DAY_OF_MONTH),
					date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE), date.get(Calendar.SECOND));
	}

	public static class SADate extends SADateTime
	{
		Pattern dread = Pattern.compile("(\\d+)-(\\d+)-(\\d+)");
		public SADate(String s)
		{
			super();
			Matcher m = dread.matcher(s);
			if (m.lookingAt())
			{
				date.set(Calendar.YEAR, Integer.parseInt(m.group(1)));
				date.set(Calendar.MONTH, Integer.parseInt(m.group(2))-1);
				date.set(Calendar.DAY_OF_MONTH, Integer.parseInt(m.group(3)));
				date.set(Calendar.HOUR_OF_DAY, 0);
				date.set(Calendar.MINUTE, 0);
				date.set(Calendar.SECOND, 0);
			}
		}

		public SADate(long seconds)
		{
			super(seconds);
		}

		@Override
		public String toString()
		{
			return String.format("%04d-%02d-%02d",
				date.get(Calendar.YEAR), date.get(Calendar.MONTH)+1, date.get(Calendar.DAY_OF_MONTH));
		}
	}

	public static class SATime extends SADateTime
	{
		Pattern tread = Pattern.compile("(\\d+):(\\d+):(\\d+)(?:\\.(\\d+))?");
		public SATime(String s)
		{
			super();
			Matcher m = tread.matcher(s);
			if (m.lookingAt())
			{
				date.set(Calendar.HOUR_OF_DAY, Integer.parseInt(m.group(1)));
				date.set(Calendar.MINUTE, Integer.parseInt(m.group(2)));
				date.set(Calendar.SECOND, Integer.parseInt(m.group(3)));
			}
		}

		@Override
		public String toString()
		{
			return String.format("%02d:%02d:%02d.0",
				date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE), date.get(Calendar.SECOND));
		}
	}
}
