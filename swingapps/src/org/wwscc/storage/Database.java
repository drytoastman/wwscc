/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2009 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.sql.SQLException;
import java.util.logging.Logger;

import org.wwscc.util.MT;
import org.wwscc.util.Messenger;
import org.wwscc.util.Prefs;

/**
 */
public class Database
{
	private static final Logger log = Logger.getLogger(Database.class.getCanonicalName());
	public static DataInterface d;

	static
	{
		d = new FakeDatabase();
	}

	/**
	 * Used at startup to open the series that was previously opened
	 */
	public static void openDefault()
	{
		try {
			String s = Prefs.getSeries("");
			openSeries(s, Prefs.getPasswordFor(s));
		} catch (Exception ioe) {
			log.severe("Failed to open default: " + ioe);
		}
	}
	
	/**
	 * Used when the user wants to select a new specific series
	 * @param series
	 * @param password
	 */
	public static void openSeries(String series, String password)
	{
		if (d != null)
			d.close();
		
		try {
			if (series.equals(""))
			{
				d = new FakeDatabase();
				Messenger.sendEvent(MT.SERIES_CHANGED, "<none>");
			}
			else
			{
				d = new PostgresqlDatabase(series, password);
				Messenger.sendEvent(MT.SERIES_CHANGED, series);
			}
			Prefs.setSeries(series);
		} catch (SQLException sqle) {
			log.severe(String.format("Unable to open series %s due to error %s", series, sqle));
		}
	}
}

