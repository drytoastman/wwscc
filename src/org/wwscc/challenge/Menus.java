/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.challenge;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.wwscc.storage.Database;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;


public class Menus extends JMenuBar implements ActionListener
{
	private static Logger log = Logger.getLogger("org.wwscc.challenge.Menus");

	Hashtable <String,JMenuItem> items;
	JFileChooser chooser;

	public Menus()
	{
		items = new Hashtable <String,JMenuItem>();
		chooser = new JFileChooser();

		/* File Menu */
		JMenu file = new JMenu("File");
		add(file);
		file.add(createItem("Open Database", null));
		file.add(createItem("Save Bracket as Image", null));
		file.addSeparator();
		file.add(createItem("Quit", null));
		
		JMenu chl = new JMenu("Challenge");
		add(chl);
		chl.add(createItem("New Open Challenge", null));
		chl.add(createItem("New Ladies Challenge", null));
		chl.add(createItem("New Bonus Challenge", null));
		chl.add(createItem("Auto Load Current", null));

		JMenu timer = new JMenu("Timer");
		add(timer);
		timer.add(createItem("Connect", null));
	}

	protected JMenuItem createItem(String title, Character key)
	{ 
		JMenuItem item = new JMenuItem(title); 

		item.addActionListener(this); 
		if (key != null) item.setAccelerator(KeyStroke.getKeyStroke(key, ActionEvent.CTRL_MASK)); 

		items.put(title, item);	
		return item; 
	}
	
	protected void createChallenge(String defaultname, boolean bonus)
	{
		String name = JOptionPane.showInputDialog("Challenge Name", defaultname);
		if (name == null)
			return;
		
		Integer size = (Integer)JOptionPane.showInputDialog(
					null, "Entrants:", "Challenge Size", JOptionPane.PLAIN_MESSAGE,
					null, new Integer[] { 4, 8, 16, 32, 64 }, null);			
		if (size == null)
			return;
		
		Database.d.newChallenge(name, size, bonus);
		Messenger.sendEvent(MT.NEW_CHALLENGE, null);
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
		}
		else if (cmd.equals("Save Bracket as Image"))
		{
			chooser.setFileFilter(new FileNameExtensionFilter("PNG Image", "png"));
			chooser.setCurrentDirectory(null); //new java.io.File(""));
				
			int returnVal = chooser.showSaveDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION) 
			{
				log.fine("Saveing image to : " + chooser.getSelectedFile().getName());
				Messenger.sendEvent(MT.PRINT_BRACKET, chooser.getSelectedFile());
			}
		}
		else if (cmd.equals("Auto Load Current"))
		{
			Messenger.sendEvent(MT.PRELOAD_MENU, null);
		}
		else if (cmd.equals("New Open Challenge"))
		{
			createChallenge("Open Challenge", false);
		}
		else if (cmd.equals("New Ladies Challenge"))
		{
			createChallenge("Ladies Challenge", false);
		}
		else if (cmd.equals("New Bonus Challenge"))
		{
			createChallenge("Bonus Challenge", true);
		}
		else if (cmd.equals("Connect"))
		{
			Messenger.sendEvent(MT.CONNECT_REQUEST, null);
		}
		else
		{ 
			log.info("Unknown command from menubar: " + cmd); 
		} 
	} 
}

