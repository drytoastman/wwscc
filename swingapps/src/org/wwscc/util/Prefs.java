/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2009 Brett Wilson.
 * All rights reserved.
 */
package org.wwscc.util;

import java.awt.Rectangle;
import java.util.prefs.Preferences;

/**
 *
 * @author bwilson
 */
public class Prefs
{
	private static Preferences prefs;
	static
	{
		prefs = Preferences.userNodeForPackage(Prefs.class);
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				try { prefs.sync(); } catch (Exception e) { }
			}
		}));
	}

	public static String getHomeServer() { return prefs.get("hostname", "scorekeeper.wwscc.org"); }
	public static String getMergeHost() { return prefs.get("mergehost", ""); }
	public static String getPasswordFor(String db) { return prefs.get("password-"+db, "none"); }
	public static String getInstallRoot() { return prefs.get("installroot", System.getProperty("user.home")); }
	public static String getSeriesFile(String def) { return prefs.get("seriesfile", def); }
	public static String getSeriesURL(String def) { return prefs.get("seriesurl", def); }
	public static boolean useSeriesURL() { return prefs.getBoolean("useurl", false); }
	public static int getEventId(int def) { return prefs.getInt("eventid", def); }
	public static int getChallengeId(int def) { return prefs.getInt("challengeid", def); }
	public static boolean useDoubleCourseMode() { return prefs.getBoolean("doublecoursemode", false); }
	public static boolean useReorderingTable() { return prefs.getBoolean("reorderingtable", false); }
	public static String getLastApplication() { return prefs.get("application", ""); }
	public static int getLightCount() { return prefs.getInt("lights", 2); }
	public static String getScannerConfig() { return prefs.get("scannerconfig", ""); }
	public static String getDefaultPrinter() { return prefs.get("defaultprinter", ""); }
	public static Rectangle getTimerWindow()
	{
		Rectangle r = new Rectangle();
		r.x = prefs.getInt("timer.x", 0);
		r.y = prefs.getInt("timer.y", 0);
		r.width = prefs.getInt("timer.width", 610);
		r.height = prefs.getInt("timer.height", 600);
		return r;
	}

	public static void setHomeServer(String s) { prefs.put("hostname", s); }
	public static void setMergeHost(String h) { prefs.put("mergehost", h); }
	public static void setPasswordFor(String db, String s) { prefs.put("password-"+db, s); }
	public static void setInstallRoot(String s) { prefs.put("installroot", s); }
	public static void setSeriesFile(String s) { prefs.put("seriesfile", s); }
	public static void setSeriesURL(String s) { prefs.put("seriesurl", s); }
	public static void setUseSeriesURL(boolean b) { prefs.putBoolean("useurl", b); }
	public static void setEventId(int i) { prefs.putInt("eventid", i); }
	public static void setChallengeId(int i) { prefs.putInt("challengeid", i); }
	public static void setDoubleCourseMode(boolean b) { prefs.putBoolean("doublecoursemode", b); }
	public static void setReorderingTable(boolean b) { prefs.putBoolean("reorderingtable", b); }
	public static void setLastApplication (String s) { prefs.put("application", s); }
	public static void setLightCount(int i) { prefs.putInt("lights", i); }
	public static void setScannerConfig(String s) { prefs.put("scannerconfig", s); }
	public static void setDefaultPrinter(String s) { prefs.put("defaultprinter", s); }
	public static void setTimerWindow(Rectangle r)
	{
		prefs.putInt("timer.x", r.x);
		prefs.putInt("timer.y", r.y);
		prefs.putInt("timer.width", r.width);
		prefs.putInt("timer.height", r.height);
	}
}
