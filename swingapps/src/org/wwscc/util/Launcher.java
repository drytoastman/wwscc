/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2010 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.util;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import org.wwscc.bwtimer.Timer;
import org.wwscc.challenge.ChallengeGUI;
import org.wwscc.dataentry.DataEntry;
import org.wwscc.protimer.ProSoloInterface;
import org.wwscc.registration.Registration;
import org.wwscc.tray.TrayMonitor;

/**
 */
public class Launcher
{
	static String[] apps = new String[] {"DataEntry", "Registration", "ChallengeGUI", "ProTimer", "BWTimer", "TrayMonitor"};

	public static void main(String args[])
	{
		String app = "";
        List<String> passthru;
        int startarg = 0;

		try
		{
			System.setProperty("swing.defaultlaf", UIManager.getSystemLookAndFeelClassName());
			if (args.length > 0) {
				app = args[0];
                startarg = 1;
			} else {
				app = "TrayMonitor";
			}

			// Check if we are using a special prefs node, only used for testing
            passthru = new ArrayList<String>();
			for (int ii = startarg; ii < args.length; ii++) {
				if (args[ii].startsWith("prefs=")) {
					String node = args[ii].substring(args[ii].indexOf('=')+1);
					Prefs.setPrefsNode(node);
				} else {
                    passthru.add(args[ii]);
                }
			}			
			
			// Repurpose the args and launch the application
            args = new String[passthru.size()];
            for (int ii = 0; ii < args.length; ii++)
                args[ii] = passthru.get(ii);

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
			else if (app.equals("TrayMonitor"))
				TrayMonitor.main(args);
            else
			    JOptionPane.showMessageDialog(null, "Unknown application " + app, "Error", JOptionPane.ERROR_MESSAGE);
		}
		catch (Throwable e)
		{
			JOptionPane.showMessageDialog(null, "Launcher catches Throwable: " + e, "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
