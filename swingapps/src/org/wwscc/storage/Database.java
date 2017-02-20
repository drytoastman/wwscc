/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2009 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.util.logging.Logger;

import org.wwscc.util.MT;
import org.wwscc.util.Messenger;
import org.wwscc.util.Prefs;

/**
 */
public class Database
{
	private static final Logger log = Logger.getLogger("org.wwscc.storage.Database");
	public static DataInterface d;

	static
	{
		d = new FakeDatabase();
	}

	public static void openDefault()
	{
		try {
			String s = Prefs.getSeries("");
			if (!s.equals(""))
				openDatabase(s, Prefs.getPasswordFor(s));
		} catch (Exception ioe) {
			log.severe("Failed to open: " + ioe);
		}
	}

	public static void openFake()
	{
		d = new FakeDatabase();
		Messenger.sendEvent(MT.DATABASE_CHANGED, "<none>");
	}
	
	public static void openDatabase(String series, String password)
	{
		d = new PostgresqlDatabase();
		d.open(series, password);
		Messenger.sendEvent(MT.DATABASE_CHANGED, series);
	}

	public static void closeDatabase()
	{
		d.close();
		d = null;
	}
}

