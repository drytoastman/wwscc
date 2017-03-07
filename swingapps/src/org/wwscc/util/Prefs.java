/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2009 Brett Wilson.
 * All rights reserved.
 */
package org.wwscc.util;

import java.awt.Rectangle;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


/**
 *
 * @author bwilson
 */
public class Prefs
{
	private static final Logger log = Logger.getLogger(Prefs.class.getCanonicalName());
	
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

	public static void setPrefsNode(String name)
	{
		prefs = Preferences.userRoot().node(name);
	}
	
	
	public static Map<String,String> getPasswords() 
	{
		Map<String,String> ret = new HashMap<String,String>();
		try {
			for (String k : prefs.keys())
			{
				if (k.startsWith("password-"))
					ret.put(k.substring(9), prefs.get(k, ""));
			}
		} catch (BackingStoreException bse) {
			log.log(Level.WARNING, "Failed to load passwords from prefs: {0}", bse.getMessage());
		}
		
		return ret;
	}
	
	public static boolean isWindows()
	{
		return System.getProperty("os.name").split("\\s")[0].equals("Windows");
	}
	
	public static boolean isLinux()
	{
		return System.getProperty("os.name").split("\\s")[0].equals("Linux");
	}
	
	public static boolean isMac()
	{
		return System.getProperty("os.name").split("\\s")[0].equals("Mac");
	}
	
	public static String getInstallRoot() 
	{
		return new File(System.getProperty("user.home"), "scorekeeper").getAbsolutePath();
	}


	public static String getHomeServer() { return prefs.get("hostname", "scorekeeper.wwscc.org"); }
	public static String getPasswordFor(String series) { return prefs.get("password-"+series, ""); }
	public static String getSeries(String def) { return prefs.get("series", def); }
	public static int getEventId(int def) { return prefs.getInt("eventid", def); }
	public static int getChallengeId(int def) { return prefs.getInt("challengeid", def); }
	public static boolean useReorderingTable() { return prefs.getBoolean("reorderingtable", false); }
	public static int getLightCount() { return prefs.getInt("lights", 2); }
	public static String getScannerConfig() { return prefs.get("scannerconfig", ""); }
	public static String getDefaultPrinter() { return prefs.get("defaultprinter", ""); }
	public static boolean usePaidFlag() { return prefs.getBoolean("paidflag", false); }
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
	public static void setPasswordFor(String series, String s) { prefs.put("password-"+series, s); }
	public static void setSeries(String s) { prefs.put("series", s); }
	public static void setEventId(int i) { prefs.putInt("eventid", i); }
	public static void setChallengeId(int i) { prefs.putInt("challengeid", i); }
	public static void setReorderingTable(boolean b) { prefs.putBoolean("reorderingtable", b); }
	public static void setLightCount(int i) { prefs.putInt("lights", i); }
	public static void setScannerConfig(String s) { prefs.put("scannerconfig", s); }
	public static void setDefaultPrinter(String s) { prefs.put("defaultprinter", s); }
	public static void setUsePaidFlag(boolean b) { prefs.putBoolean("paidflag", b); }
	public static void setTimerWindow(Rectangle r)
	{
		prefs.putInt("timer.x", r.x);
		prefs.putInt("timer.y", r.y);
		prefs.putInt("timer.width", r.width);
		prefs.putInt("timer.height", r.height);
	}
}
