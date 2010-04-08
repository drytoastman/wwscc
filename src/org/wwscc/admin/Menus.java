/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.admin;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import org.wwscc.storage.Database;
import org.wwscc.util.MultiInputDialog;
import org.wwscc.util.Prefs;


public class Menus extends JMenuBar implements ActionListener
{
	private static Logger log = Logger.getLogger("org.wwscc.admin.Menus");

	Map <String,JMenuItem> items;

	public Menus()
	{
		items = new HashMap <String,JMenuItem>();

		/* File Menu */
		JMenu file = new JMenu("File");
		add(file);

		file.add(createItem("Open Database", null));
		file.add(createItem("Checkout Database", null));
		file.add(createItem("Checkin Database", null));
		file.add(createItem("Runs To Reg", null));
		file.addSeparator();
		file.add(createItem("Copy Online Database", null));
		file.addSeparator();
		file.add(createItem("Online Setup", null));
		file.add(createItem("Quit", null));


		/* Edit Menu *
		JMenu edit = new JMenu("Edit");
		add(edit);

		edit.add(createItem("Find", 'F')); */
	}

	protected JMenuItem createItem(String title, Character key)
	{ 
		JMenuItem item = new JMenuItem(title); 

		item.addActionListener(this); 
		if (key != null) item.setAccelerator(KeyStroke.getKeyStroke(key, ActionEvent.CTRL_MASK)); 

		items.put(title, item);	
		return item; 
	} 

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
		}

		else if (cmd.equals("Copy Online Database"))
		{
			new NetworkTask(false, false).execute(); // Download without lock
		}

		else if (cmd.equals("Checkout Database"))
		{
			new NetworkTask(false, true).execute(); // Download with lock
		}

		else if (cmd.equals("Checkin Database"))
		{
			new NetworkTask(true, false).execute(); // Upload
		}

		else if (cmd.equals("Online Setup"))
		{
			MultiInputDialog d = new MultiInputDialog("Online Setup");
			d.addString("Hostname", Prefs.getHomeServer());
			//d.addString("Password", Prefs.getPasswordFor(Database.d.getCurrentSeries()), false);
			
			if (!d.runDialog())
				return;

			Prefs.setHomeServer(d.getResponse("Hostname"));
			//Prefs.setPasswordFor(Database.d.getCurrentSeries(), d.getResponse("Password"));
		}
		
		else if (cmd.equals("Runs To Reg"))
		{
			// backup here? or move this to upload
			// TODO, deal with the upload stuff Database.d.updateRegistrationFromRuns();
		}

		else
		{ 
			log.info("Unknown command from menubar: " + cmd); 
		} 
	} 


	class NetworkTask extends SwingWorker<File, Object>
	{
		boolean upload;
		boolean dolock;
	
		public NetworkTask(boolean upload, boolean dolock)
		{
			this.upload = upload;
			this.dolock = dolock;
		}
	
		@Override
		public File doInBackground() 
		{ return null; /*
			DatabaseTransfer web = new DatabaseTransfer();
	
			if (upload)
				return web.uploadDatabase();
			else
				return web.downloadDatabase(dolock); */
		}

		@Override
		protected void done() 
		{
			try 
			{
				File f = get();
				if (f != null)
				{
					if (upload)
					{
						//TODO backup here if local and upload, Database.backup(f);
						Database.closeDatabase();
					}
					else
					{
						Database.open();
					}
				}
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "Web Interface Error", e);
			}
		}
	}

}

