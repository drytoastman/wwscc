/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.admin;

import java.awt.BorderLayout;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultRowSorter;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import org.wwscc.storage.Database;
import org.wwscc.util.Logging;
import org.wwscc.util.SearchTrigger;

/**
 */
public class Admin extends JFrame
{
	private static Logger log = Logger.getLogger("org.wwscc.admin.Admin");

	Menus menus;
	protected JTabbedPane tabs;
	protected DataStore dataStore;

	public Admin()
	{
		super("Admin");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		dataStore = new DataStore();
		tabs = new JTabbedPane();
		tabs.addTab("Drivers", createDriversTab());
		tabs.addTab("Cars", createCarsTab());

		menus = new Menus();
		setJMenuBar(menus);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(tabs, BorderLayout.CENTER);
		setSize(1024,768);
		pack();
		setVisible(true);

		Database.openDefault();
		dataStore.loadData();
	}

	@SuppressWarnings("unchecked")
	public JComponent createDriversTab()
	{
		final JTable d = new BetterTable(dataStore.getDriverModel()) {
			public void doubleClickModelRow(int r) {
				System.out.println("double click on " + r);
			}
			public void cutModelRows(int r[]) {
				dataStore.deleteDrivers(r);
			}
		};
		d.setAutoCreateRowSorter(true);

		JTextField search = new JTextField();
		search.getDocument().addDocumentListener(new SearchTrigger() { public void search(String s) {
			((DefaultRowSorter)d.getRowSorter()).setRowFilter(new BasicWordFilter(s));
		}});

		JPanel p = new JPanel(new BorderLayout());
		p.add(search, BorderLayout.NORTH);
		p.add(new JScrollPane(d), BorderLayout.CENTER);
		return p;
	}

	@SuppressWarnings("unchecked")
	public JComponent createCarsTab()
	{
		AbstractTableModel m = dataStore.getCarModel();
		final JTable c = new BetterTable(m) {
			public void doubleClickModelRow(int r) {
				System.out.println("double click on " + r);
			}
			public void cutModelRows(int r[]) {
				dataStore.deleteCars(r);
			}
		};
		c.setAutoCreateRowSorter(true);

		JTextField search = new JTextField();
		search.getDocument().addDocumentListener(new SearchTrigger() { public void search(String s) {
			((DefaultRowSorter)c.getRowSorter()).setRowFilter(new BasicWordFilter(s));
		}});

		c.getRowSorter().toggleSortOrder(m.findColumn("Number"));
		c.getRowSorter().toggleSortOrder(m.findColumn("Class"));

		JPanel p = new JPanel(new BorderLayout());
		p.add(search, BorderLayout.NORTH);
		p.add(new JScrollPane(c), BorderLayout.CENTER);
		return p;
	}

	public static void main(String args[])
	{
		try
		{
			Logging.logSetup("admin");
			new Admin();
		}
		catch (Throwable e)
		{
			log.log(Level.SEVERE, "Admin app died: " + e, e);
		}
	}

}
