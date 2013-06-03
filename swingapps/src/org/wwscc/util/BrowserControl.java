/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.util;

import java.awt.Desktop;
import java.net.URI;
import java.util.logging.Logger;

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
		try{
			Desktop.getDesktop().browse(new URI(url));
		} catch (Exception ex) {
			log.severe("Couldn't open default web browser:" + ex);
		}
	}
	
	
	public static void printGroupResults(int[] groups)
	{
		if (groups.length == 0)
			return;
		
		String g = new String();
		int ii;
		for (ii = 0; ii < groups.length-1; ii++)
			g += groups[ii]+",";
		g += groups[ii];

		printURL(String.format("http://%s/results/%s/%s/bygroup?course=%s&list=%s", 
				Database.d.getCurrentHost(),
				Database.d.getCurrentSeries(), 
				Database.d.getCurrentEvent().getId(), 
				Database.d.getCurrentCourse(), g));
	}
	
	public static void printURL(String url)
	{
		try
		{
			Runtime r = Runtime.getRuntime();
			String os = System.getProperty("os.name");
			if ((os != null) && (os.startsWith("Windows"))) {
				r.exec("rundll32.exe \"C:\\Windows\\system32\\mshtml.dll\",PrintHTML \"" + url + "\"").waitFor();
			} else {
				openURL(url);
			}
		}
		catch (Exception ex)
		{
			log.severe("Couldn't open web browser:" + ex);
		}
	}

}

