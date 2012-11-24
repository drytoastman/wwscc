/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.util;

import java.util.logging.*;
import org.wwscc.storage.Database;

public class BrowserControl
{
	private static Logger log = Logger.getLogger("org.wwscc.util.Web");

	public static void openAuditReport(String order)
	{
		for (int ii = 1; ii <= Database.d.getCurrentEvent().getCourses(); ii++)
		{
			openResults(String.format("audit?course=%d&group=%d&order=%s",
				ii, Database.d.getCurrentRunGroup(), order));
		}
	}

	public static void openDialinReport(String filter, String order)
	{
		if (filter == null)
			openResults("dialins?order=" + order);
		else
			openResults("dialins?filter=" + filter + "&order=" + order);
	}

	public static void openGroupResults(int[] groups)
	{
		if (groups.length == 0)
			return;
		
		String g = new String();
		int ii;
		for (ii = 0; ii < groups.length-1; ii++)
			g += groups[ii]+",";
		g += groups[ii];

		openResults("bygroup?course="+Database.d.getCurrentCourse()+"&list="+g);
	}

	public static void openResults(String selection)
	{
		openURL(String.format("http://%s/results/%s/%s/%s", Database.d.getCurrentHost(),
				Database.d.getCurrentSeries(), Database.d.getCurrentEvent().getId(), selection));
	}

	public static void openAdmin(String selection)
	{
		openURL(String.format("http://%s/admin/%s/%s/%s", Database.d.getCurrentHost(),
				Database.d.getCurrentSeries(), Database.d.getCurrentEvent().getId(), selection));
	}

	public static void openURL(String url)
	{
		try
		{
			Runtime r = Runtime.getRuntime();
			String os = System.getProperty("os.name");
			if ((os != null) && (os.startsWith("Windows")))
			{
				log.info("Spawning Windows browser on " + url);
				r.exec("rundll32 url.dll,FileProtocolHandler " + url).waitFor();
			}
			else
			{
				log.info("Spawning firefox on " + url);
				r.exec("firefox " + url).waitFor();  // TBD, get the -remote command line for firefox
			}
		}
		catch (Exception ex)
		{
			log.severe("Couldn't open web browser:" + ex);
		}
	}
}

