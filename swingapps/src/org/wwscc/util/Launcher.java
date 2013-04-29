/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2010 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.util;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import org.wwscc.bwtimer.Timer;
import org.wwscc.challenge.ChallengeGUI;
import org.wwscc.dataentry.DataEntry;
import org.wwscc.protimer.ProSoloInterface;
import org.wwscc.registration.Registration;

/**
 */
public class Launcher
{
	static String[] apps = new String[] {"DataEntry", "Registration", "ChallengeGUI", "ProTimer", "BWTimer" };

	public static void main(String args[])
	{
		try
		{
			System.setProperty("swing.defaultlaf", UIManager.getSystemLookAndFeelClassName());
			String app;
			if (args.length == 0)
			{
				Object o = JOptionPane.showInputDialog(null,
						"Select Application", "Launcher", JOptionPane.QUESTION_MESSAGE, null,
						apps, Prefs.getLastApplication());
				if (o == null)
					return;
				app = (String)o;
			}
			else
			{
				app = args[0];
			}

			
			Prefs.setLastApplication(app);
			LibLoader.installLibrary("sqliteintf", "database access");
			LibLoader.installLibrary("rxtxSerial", "serial port access");
			
			for (String arg : args) {
				if (arg.startsWith("prefs=")) {
					String node = arg.substring(arg.indexOf('=')+1);
					Prefs.setPrefsNode(node);
				}
			}
			
			if (app.equals("DataEntry"))
				DataEntry.main(args);
			else if (app.equals("ChallengeGUI"))
				ChallengeGUI.main(args);
			else if (app.equals("ProTimer"))
				ProSoloInterface.main(args);
			else if (app.equals("BWTimer"))
				Timer.main(args);
			else if (app.equals("Registration"))
				Registration.main(args);
		}
		catch (Throwable e)
		{
			JOptionPane.showMessageDialog(null, "Launcher catches Throwable: " + e,
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

}
