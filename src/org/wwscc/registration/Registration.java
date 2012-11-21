/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2010 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.registration;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import org.wwscc.dialogs.DatabaseDialog;
import org.wwscc.storage.Database;
import org.wwscc.storage.MergeProcess;
import org.wwscc.util.Logging;
import org.wwscc.util.Prefs;


public class Registration extends JFrame implements ActionListener
{
	private static final Logger log = Logger.getLogger(Registration.class.getCanonicalName());

	SelectionBar setupBar;
	EntryPanel driverEntry;

	public Registration() throws IOException
	{
		super("Registration");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		setupBar = new SelectionBar();
		driverEntry = new EntryPanel();

		BorderLayout layout = new BorderLayout();
		JPanel content = new JPanel(layout);
		content.add(setupBar, BorderLayout.NORTH);
		content.add(driverEntry, BorderLayout.CENTER);

		setContentPane(content);		
		log.log(Level.INFO, "Starting Registration: {0}", new java.util.Date());

		JMenu file = new JMenu("File");
		file.add(createItem("Open Local Database"));
		file.add(createItem("Download Database Copy"));
		file.add(new JSeparator());
		file.add(createItem("Merge Database"));
		file.add(createItem("Quit"));
		
		JMenuBar bar = new JMenuBar();
		bar.add(file);
		setJMenuBar(bar);

		Database.openDefault();
		Database.d.trackRegChanges(true);
		
		pack();
		setVisible(true);
	}

	private JMenuItem createItem(String title)
	{
		JMenuItem item = new JMenuItem(title);
		item.addActionListener(this);
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
		else if (cmd.equals("Open Local Database"))
		{
			Database.open(true, false);
			Database.d.trackRegChanges(true);
		}
		else if (cmd.equals("Download Database Copy"))
		{
			new Thread(new Runnable() {
				public void run() {
					Database.download(false);
					Database.d.trackRegChanges(true);
				}
			}).start();
		}
		else if (cmd.equals("Merge Database"))
		{
			DatabaseDialog dd = new DatabaseDialog(null, Prefs.getMergeHost()+"/"+Database.d.getCurrentSeries(), true);
			dd.doDialog("Merge Series", null);

			if (dd.getResult() != null)
			{
				String spec = (String)dd.getResult();
				final String sp[] = spec.split("/");
				if (sp.length != 2)
				{
					log.log(Level.SEVERE, "Invalid network spec: {0}", spec);
					return;
				}

				Prefs.setMergeHost(sp[0]);
				new Thread(new Runnable() { public void run() { MergeProcess.mergeTo(sp[0], sp[1]); }}).start();
			}
		}
		else
		{
			log.log(Level.INFO, "Unknown command from menubar: {0}", cmd);
		}
	}
			
	/**
	 * Main
	 *
	 * @param args 
	 */
	public static void main(String[] args)
	{
		try
		{
			Logging.logSetup("registration");
			SwingUtilities.invokeLater(new Runnable() { public void run() {
				try {
					new Registration();
				} catch (Throwable ioe) {
					log.log(Level.SEVERE, "Registration failed to start: " + ioe, ioe);
				}
			}});
		}
		catch (Throwable e)
		{
			log.log(Level.SEVERE, "Registration main failure: " + e, e);
		}
	}
}

