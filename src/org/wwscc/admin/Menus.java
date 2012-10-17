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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
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
}

