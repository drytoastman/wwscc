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


public class Registration extends JFrame implements ActionListener
{
	private static Logger log = Logger.getLogger(Registration.class.getCanonicalName());

	SelectionBar setupBar;
	EntryPanel driverEntry;

	public Registration() throws IOException
	{
		super("Registration");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		setupBar = new SelectionBar();
		driverEntry = new EntryPanel();

		BorderLayout layout = new BorderLayout();
		layout.setHgap(5);
		layout.setVgap(5);

		JPanel content = new JPanel(layout);
		content.add(setupBar, BorderLayout.NORTH);
		content.add(driverEntry, BorderLayout.CENTER);

		setContentPane(content);
		setSize(450,768);
		setVisible(true);
		
		log.info("Starting Application: " + new java.util.Date());

		JMenu file = new JMenu("File");
		file.add(createItem("Open Database"));
		file.add(createItem("Download Database"));
		file.add(new JSeparator());
		file.add(createItem("Merge Database"));
		file.add(createItem("Quit"));
		
		JMenuBar bar = new JMenuBar();
		bar.add(file);
		this.setJMenuBar(bar);

		Database.openDefault();
		Database.d.trackRegChanges(true);
	}

	protected JMenuItem createItem(String title)
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
		else if (cmd.equals("Open Database"))
		{
			Database.open();
			Database.d.trackRegChanges(true);
		}
		else if (cmd.equals("Download Database"))
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
			DatabaseDialog dd = new DatabaseDialog(null, "1.1.1.1/"+Database.d.getCurrentSeries(), true);
			dd.doDialog("Merge Series", null);

			if (dd.getResult() != null)
			{
				String spec = (String)dd.getResult();
				final String sp[] = spec.split("/");
				if (sp.length != 2)
					log.severe("Invalid network spec: " + spec);
				else
					new Thread(new Runnable() { public void run() { MergeProcess.mergeTo(sp[0], sp[1]); }}).start();
			}
		}
		else
		{
			log.info("Unknown command from menubar: " + cmd);
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

