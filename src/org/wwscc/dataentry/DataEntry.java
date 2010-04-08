/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */


package org.wwscc.dataentry;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.wwscc.storage.Database;
import org.wwscc.storage.Entrant;
import org.wwscc.util.LastManEventQueue;
import org.wwscc.util.Logging;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;


public class DataEntry extends JFrame implements MessageListener
{
	private static Logger log = Logger.getLogger("org.wwscc.dataentry.DataEntry");

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

		JPanel content = new JPanel(layout);
		content.add(setupBar, BorderLayout.NORTH);
		content.add(tabs, BorderLayout.WEST);
		content.add(tableScroll, BorderLayout.CENTER);
		content.add(timeEntry, BorderLayout.EAST);
		content.add(new JLabel("here I am"), BorderLayout.SOUTH);
		
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
			System.setProperty("swing.defaultlaf", UIManager.getSystemLookAndFeelClassName());
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

