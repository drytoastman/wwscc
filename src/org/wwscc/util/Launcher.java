/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.wwscc.util;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import org.wwscc.challenge.ChallengeGUI;
import org.wwscc.dataentry.DataEntry;
import org.wwscc.protimer.ProSoloInterface;

/**
 *
 * @author bwilson
 */
public class Launcher
{
	static String[] apps = new String[] {"DataEntry", "Challenge GUI", "Pro Timer"};

	public static void main(String args[])
	{
		System.setProperty("swing.defaultlaf", UIManager.getSystemLookAndFeelClassName());
		Object o = JOptionPane.showInputDialog(null, 
				"Select Application", "Launcher", JOptionPane.QUESTION_MESSAGE, null,
				apps, Prefs.getLastApplication());

		if (o == null)
			return;

		String app = (String)o;
		Prefs.setLastApplication(app);
		if (app.equals("DataEntry"))
			DataEntry.main(args);
		else if (app.equals("Challenge GUI"))
			ChallengeGUI.main(args);
		else if (app.equals("Pro Timer"))
			ProSoloInterface.main(args);
	}

}
