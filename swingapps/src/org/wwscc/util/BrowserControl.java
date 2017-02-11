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

public class BrowserControl
{
	private static Logger log = Logger.getLogger("org.wwscc.util.Web");

	public static void openAuditReport(ApplicationState state, String order)
	{
		openResults(state, String.format("audit?order=%s", order));
	}

	public static void openDialinReport(ApplicationState state, String filter, String order)
	{
		if (filter == null)
			openResults(state, "dialins?order=" + order);
		else
			openResults(state, "dialins?filter=" + filter + "&order=" + order);
	}

	public static void openResults(ApplicationState state, String selection)
	{
		openURL(String.format("http://127.0.0.1/results/%s/%s/%s", state.getCurrentSeries(), state.getCurrentEventId(), selection));
	}

	public static void openAdmin(ApplicationState state, String selection)
	{
		openURL(String.format("http://127.0.0.1/admin/%s/%s/%s", state.getCurrentSeries(), state.getCurrentEventId(), selection));
	}

	public static void openURL(String url)
	{
		try{
			Desktop.getDesktop().browse(new URI(url));
		} catch (Exception ex) {
			log.severe("Couldn't open default web browser:" + ex);
		}
	}
	
	public static void printGroupResults(ApplicationState state, int[] groups)
	{
		if (groups.length == 0)
			return;
		
		String g = new String();
		int ii;
		for (ii = 0; ii < groups.length-1; ii++)
			g += groups[ii]+",";
		g += groups[ii];

		printURL(String.format("http://127.0.0.1/results/%s/%s/bygroup?course=%s&list=%s", state.getCurrentSeries(), state.getCurrentEventId(), state.getCurrentCourse(), g));
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

