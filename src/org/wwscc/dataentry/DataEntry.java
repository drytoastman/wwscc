/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */


package org.wwscc.dataentry;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import net.miginfocom.swing.MigLayout;
import org.wwscc.components.CurrentDatabaseLabel;
import org.wwscc.components.MyIpLabel;
import org.wwscc.storage.Database;
import org.wwscc.storage.Entrant;
import org.wwscc.storage.Run;
import org.wwscc.util.LastManEventQueue;
import org.wwscc.util.Logging;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;


public class DataEntry extends JFrame implements MessageListener
{
	private static Logger log = Logger.getLogger(DataEntry.class.getName());

	Menus menus;
	EntryModel dataModel;
	SelectionBar setupBar;
	DriverEntry driverEntry;
	ClassTree  numberTree;
	ResultsPane announcer;
	EntryTable table;
	JScrollPane tableScroll;

	TimeEntry timeEntry;

	JTabbedPane tabs;

	class HelpPanel extends JLabel implements MessageListener
	{
		public HelpPanel()
		{
			super("Use Tabbed Panels to add Entrants");
			setHorizontalAlignment(CENTER);
			setFont(getFont().deriveFont(12f));
			Messenger.register(MT.OBJECT_CLICKED, this);
			Messenger.register(MT.OBJECT_DCLICKED, this);
		}

		@Override
		public void event(MT type, Object data) {
			switch (type)
			{
				case OBJECT_CLICKED:
					if (data instanceof Entrant)
						setText("Entrant: Ctrl-X or Delete to remove them, Drag to move them, Swap Entrant to change them");
					else if (data instanceof Run)
						setText("Runs: Ctrl-X or Delete to cut, Ctrl-C to copy, Ctrl-V to paste");
					else
						setText("Ctrl-V to paste a Run");
					break;
				case OBJECT_DCLICKED:
					if (data instanceof Entrant)
						setText("Click Swap Entrant to change the entrant for the given runs");
					break;
			}
		}
	}

	public DataEntry() throws IOException
	{
		super("Data Entry");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		menus = new Menus();
		setJMenuBar(menus);

		dataModel = new EntryModel();
		table = new EntryTable(dataModel);
		setupBar = new SelectionBar();
		numberTree = new ClassTree();
		driverEntry = new DriverEntry();
		announcer = new ResultsPane();


		tabs = new JTabbedPane();
		tabs.setMinimumSize(new Dimension(250, 400));
		tabs.setPreferredSize(new Dimension(250, 768));
		tabs.addTab("Add By Name", driverEntry);
		tabs.addTab("Preregistered", new JScrollPane(numberTree));
		tabs.addTab(" Announcer Data ", announcer);

		tableScroll = new JScrollPane(table);
		tableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		
		BorderLayout layout = new BorderLayout();
		layout.setHgap(5);
		layout.setVgap(5);

		timeEntry = new TimeEntry();
		menus.add(timeEntry.getTimerMenu());


		CurrentDatabaseLabel curdb = new CurrentDatabaseLabel();
		HelpPanel help = new HelpPanel();
		MyIpLabel myip = new MyIpLabel();
		curdb.setBorder(BorderFactory.createLoweredBevelBorder());
		help.setBorder(BorderFactory.createLoweredBevelBorder());
		myip.setBorder(BorderFactory.createLoweredBevelBorder());

		JPanel infoBoxes = new JPanel(new MigLayout("fill, ins 0", "[18%]0[64%]0[18%]"));
		infoBoxes.add(curdb, "grow");
		infoBoxes.add(help, "grow, hmin 20");
		infoBoxes.add(myip, "grow");

		JPanel content = new JPanel(layout);
		content.add(setupBar, BorderLayout.NORTH);
		content.add(tabs, BorderLayout.WEST);
		content.add(tableScroll, BorderLayout.CENTER);
		content.add(timeEntry, BorderLayout.EAST);
		content.add(infoBoxes, BorderLayout.SOUTH);
		
		setContentPane(content);
		setSize(1024,768);
		setVisible(true);

		getRootPane().setDefaultButton(timeEntry.getEnterButton());
		log.info("Starting Application: " + new java.util.Date());
		
		Messenger.register(MT.OBJECT_DCLICKED, this);
		Database.openDefault();
	}


	@Override
	public void event(MT type, Object o)
	{
		switch (type)
		{
			case OBJECT_DCLICKED:
				if (o instanceof Entrant)
					tabs.setSelectedComponent(driverEntry);
				break;
		}
	}

	/**
	 * Main
	 */
	public static void main(String[] args)
	{
		try
		{
			Class.forName("org.wwscc.dataentry.Sounds");
			Logging.logSetup("dataentry");
			
			SwingUtilities.invokeLater(new Runnable() { public void run() {
				try {
					new DataEntry();
					Toolkit.getDefaultToolkit().getSystemEventQueue().push(new LastManEventQueue());
				} catch (Throwable e) {
					log.log(Level.SEVERE, "DataEntry creation failed: " + e, e);
				}
			}});
		}
		catch (Throwable e)
		{
			log.log(Level.SEVERE, "App failure: " + e, e);
		}
	}
}

