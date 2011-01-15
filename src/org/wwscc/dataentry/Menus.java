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
import java.awt.event.KeyEvent;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import org.wwscc.dialogs.BaseDialog.DialogFinisher;
import org.wwscc.dialogs.GroupDialog;
import org.wwscc.storage.Database;
import org.wwscc.storage.Event;
import org.wwscc.util.BrowserControl;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;
import org.wwscc.util.Prefs;


public class Menus extends JMenuBar implements ActionListener, MessageListener
{
	private static final Logger log = Logger.getLogger("org.wwscc.dataentry.Menus");

	Map <String,JMenuItem> items;
	JCheckBoxMenuItem dcMode;
	JFileChooser chooser;
	ButtonGroup runGrouping;

	public Menus()
	{
		items = new HashMap <String,JMenuItem>();
		Messenger.register(MT.DATABASE_CHANGED, this);
		Messenger.register(MT.EVENT_CHANGED, this);

		/* File Menu */
		JMenu file = new JMenu("File");
		add(file);

		file.add(createItem("Open Database", null));
		file.add(createItem("Download and Lock Database", null));
		file.add(createItem("Upload and Unlock Database", null));
		file.add(createItem("Quit", null));

		/* Edit Menu */
		JMenu edit = new JMenu("Edit");
		add(edit);
		edit.add(createItem("Find", KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK)));

		/* Event Menu */
		JMenu event = new JMenu("Event");
		add(event);	

		/* Runs Submenu */
		runGrouping = new ButtonGroup();
		JMenu runs = new JMenu("Set Runs");
		event.add(runs);
		for (int ii = 2; ii <= 20; ii++)
		{
			JRadioButtonMenuItem m = new JRadioButtonMenuItem(ii + " Runs");
			m.addActionListener(this);
			runGrouping.add(m);
			runs.add(m);
		}

		dcMode = new JCheckBoxMenuItem("Use Double Course Mode", Prefs.useDoubleCourseMode());
		dcMode.addActionListener(this);
		event.add(dcMode);

		/* Results Menu */
		JMenu results = new JMenu("Reports");
		add(results);

		JMenu audit = new JMenu("Current Group Audit");
		audit.add(createItem("In Run Order", null));
		audit.add(createItem("Order By First Name", null));
		audit.add(createItem("Order By Last Name", null));
		
		results.add(createItem("Multiple Group Results", null));
		results.add(audit);
		results.add(createItem("Results Page", null));
		results.add(createItem("Admin Page", null));
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
		else if (cmd.startsWith("Results Page")) BrowserControl.openResults("");
		else if (cmd.startsWith("Admin Page")) BrowserControl.openAdmin("");
		else if (cmd.endsWith("Runs"))
		{
			int runs = Integer.parseInt(cmd.split(" ")[0]);
			if ((runs > 1) && (runs < 100))
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
		else if (cmd.equals("Open Database"))
		{
			Database.open();
		}

		else if (cmd.equals("Download and Lock Database"))
		{
			new Thread(new Runnable() {
				public void run() {
					Database.download(true);
				}
			}).start();
		}
		else if (cmd.equals("Upload and Unlock Database"))
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
		else if (cmd.equals("Find"))
		{
			Messenger.sendEvent(MT.OPEN_FIND, null);
		}
		else
		{ 
			log.info("Unknown command from menubar: " + cmd); 
		} 
	}

	
	@Override
	public void event(MT type, Object data)
	{
		switch (type)
		{
			case DATABASE_CHANGED:
				items.get("Upload and Unlock Database").setEnabled(Database.file != null);
				break;

			case EVENT_CHANGED:
				/* when we first start or the a new event is selected, will also double when selecting via menu */
				Enumeration<AbstractButton> e = runGrouping.getElements();
				while (e.hasMoreElements())
				{
					AbstractButton b = e.nextElement();
					int run = Integer.parseInt(b.getActionCommand().split(" ")[0]);
					if (run == Database.d.getCurrentEvent().getRuns())
						b.setSelected(true);
				}
		}
	}
}

