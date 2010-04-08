/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dataentry;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import org.wwscc.dialogs.BaseDialog.DialogFinisher;
import org.wwscc.dialogs.GroupDialog;
import org.wwscc.dialogs.RunGroupDialog;
import org.wwscc.storage.Database;
import org.wwscc.storage.Event;
import org.wwscc.util.BrowserControl;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;
import org.wwscc.util.Prefs;


public class Menus extends JMenuBar implements ActionListener
{
	private static Logger log = Logger.getLogger("org.wwscc.dataentry.Menus");

	Map <String,JMenuItem> items;
	JCheckBoxMenuItem dcMode;
	JFileChooser chooser;

	public Menus()
	{
		items = new HashMap <String,JMenuItem>();

		/* File Menu */
		JMenu file = new JMenu("File");
		add(file);

		file.add(createItem("Open Database", null));
		file.add(createItem("Upload Database", null));
		file.add(createItem("Download Database", null));
		file.add(createItem("Quit", null));


		/* Edit Menu *
		JMenu edit = new JMenu("Edit");
		add(edit);
		
		edit.add(createItem("Find", KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK))); */

		/* Event Menu */
		JMenu event = new JMenu("Event");
		add(event);	

		JMenu runs = new JMenu("Set Runs");
		event.add(runs);
		for (int ii = 2; ii <= 10; ii++)
			runs.add(createItem(ii + " Runs", null));

		dcMode = new JCheckBoxMenuItem("Use Double Course Mode", Prefs.useDoubleCourseMode());
		dcMode.addActionListener(this);
		event.add(dcMode);
		event.add(createItem("Set Assigned RunGroups", null));
		event.add(createItem("Grid Order Test", null));

		/* Results Menu */
		JMenu results = new JMenu("Reports");
		add(results);

		results.add(createItem("Multiple Group Results", null));

		JMenu dialins = new JMenu("Dialins");
		results.add(dialins);
		dialins.add(createItem("All By Net", null));
		dialins.add(createItem("All By Diff", null));
		dialins.add(createItem("Ladies By Net", null));
		dialins.add(createItem("Ladies By Diff", null));
		
		JMenu audit = new JMenu("Current Group Audit");
		results.add(audit);
		audit.add(createItem("In Run Order", null));
		audit.add(createItem("Order By First Name", null));
		audit.add(createItem("Order By Last Name", null));
	}

	protected JMenuItem createItem(String title, KeyStroke ks)
	{ 
		JMenuItem item = new JMenuItem(title); 

		item.addActionListener(this); 
		if (ks != null)
			item.setAccelerator(ks);
		items.put(title, item);	
		return item; 
	} 

	@Override
	public void actionPerformed(ActionEvent e) 
	{ 
		String cmd = e.getActionCommand(); 
		if (cmd.equals("Quit")) 
		{ 
			System.exit(0); 
		} 
		else if (cmd.equals("Multiple Group Results"))
		{
			new GroupDialog().doDialog("Select groups to print:", new DialogFinisher<int[]>() {
				@Override
				public void dialogFinished(int[] result) {
					if (result != null)
						BrowserControl.openGroupResults(result);
				}
			});
		}
		else if (cmd.startsWith("Order By First Name")) BrowserControl.openAuditReport("firstname");
		else if (cmd.startsWith("Order By Last Name")) BrowserControl.openAuditReport("lastname");
		else if (cmd.startsWith("In Run Order")) BrowserControl.openAuditReport("runorder");
		else if (cmd.startsWith("All By Net")) BrowserControl.openDialinReport(null, "Net");
		else if (cmd.startsWith("All By Diff")) BrowserControl.openDialinReport(null, "Diff");
		else if (cmd.startsWith("Ladies By Net")) BrowserControl.openDialinReport("Ladies", "Net");
		else if (cmd.startsWith("Ladies By Diff")) BrowserControl.openDialinReport("Ladies", "Diff");
		else if (cmd.endsWith("Runs"))
		{
			int runs = Integer.parseInt(cmd.split(" ")[0]);
			if ((runs > 1) && (runs < 20))
			{
				Event event = Database.d.getCurrentEvent();
				int save = event.getRuns();
				event.setRuns(runs);
				if (Database.d.updateEvent())
					Messenger.sendEvent(MT.EVENT_CHANGED, null);
				else
					event.setRuns(save); // We bombed
			}
		}
		else if (cmd.equals("Set Assigned RunGroups"))
		{
			new RunGroupDialog(Database.d.getClass2RunGroupMapping()).doDialog("Set Groups", new DialogFinisher<Map<String,Integer>>() {
				@Override
				public void dialogFinished(Map<String,Integer> result) {
					if (result != null)
						Database.d.setClass2RunGroupMapping(result);
				}
			});
		}
		else if (cmd.equals("Open Database"))
		{
			Database.open();
		}
		else if (cmd.equals("Download Database"))
		{
			new Thread(new Runnable() {
				public void run() {
					Database.download(true);
				}
			}).start();
		}
		else if (cmd.equals("Upload Database"))
		{
			new Thread(new Runnable() {
				public void run() {
					Database.upload();
				}
			}).start();
		}
		else if (cmd.equals("Use Double Course Mode"))
		{
			Prefs.setDoubleCourseMode(dcMode.getState());
		}
		else
		{ 
			log.info("Unknown command from menubar: " + cmd); 
		} 
	} 
}

