/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2010 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.registration;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

import org.wwscc.barcodes.BarcodeScannerOptionsAction;
import org.wwscc.barcodes.BarcodeScannerWatcher;
import org.wwscc.dialogs.DatabaseDialog;
import org.wwscc.registration.attendance.Attendance;
import org.wwscc.registration.attendance.AttendancePanel;
import org.wwscc.storage.Database;
import org.wwscc.storage.MergeDialog;
import org.wwscc.util.CancelException;
import org.wwscc.util.Logging;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;
import org.wwscc.util.Prefs;


public class Registration extends JFrame implements ActionListener
{
	private static final Logger log = Logger.getLogger(Registration.class.getCanonicalName());

	SelectionBar setupBar;
	EntryPanel driverEntry;
	AttendancePanel attendanceDisplay;

	public Registration(String name) throws IOException
	{
		super(name);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new BarcodeScannerWatcher());

		setupBar = new SelectionBar();
		attendanceDisplay = new AttendancePanel();
		driverEntry = new EntryPanel(attendanceDisplay);

		BorderLayout layout = new BorderLayout();
		JPanel content = new JPanel(layout);
		content.add(setupBar, BorderLayout.NORTH);
		content.add(driverEntry, BorderLayout.CENTER);

		setContentPane(content);		
		log.log(Level.INFO, "Starting Registration: {0}", new java.util.Date());

		JMenu file = new JMenu("File");
		file.add(createItem("Open Database"));
		file.add(createItem("Download Database Copy"));
		file.add(new JSeparator());
		file.add(createItem("Merge Database"));
		file.add(createItem("Quit"));
		
		JMenu options = new JMenu("Options");
		options.add(new BarcodeScannerOptionsAction());
		
		JMenu attendance = new JMenu("Attendance");
		attendance.add(createItem("Download Attendance History"));
		attendance.add(createItem("Configure Attendance Values"));
		JCheckBoxMenuItem cb = new JCheckBoxMenuItem("Show Attendance Values");
		cb.addActionListener(this);
		attendance.add(cb);
		
		JMenuBar bar = new JMenuBar();
		bar.add(file);
		bar.add(options);
		bar.add(attendance);
		setJMenuBar(bar);

		Database.openDefault();
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
		else if (cmd.equals("Open Database"))
		{
			Database.open(true, true);
		}
		else if (cmd.equals("Download Database Copy"))
		{
			new Thread(new Runnable() {
				public void run() {
					Database.download(false);
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

				if (!sp[1].equals(Database.d.getCurrentSeries()))
				{
					if (JOptionPane.showConfirmDialog(this, "The series names are not the same, do you want to continue?", "Series Names Differ", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
						return;
				}

				Prefs.setMergeHost(sp[0]);
				new Thread(new Runnable() { public void run() { MergeDialog.mergeTo(sp[0], sp[1]); }}).start();
			}
		}
		else if (cmd.equals("Download Attendance History"))
		{
			new Thread(new Runnable() {
				public void run() {
					try {						
						Attendance.getAttendance(Database.getHost());
					} catch (CancelException ce) {
						// pass
					} catch (Exception e1) {
						log.severe("Failed to download attendance: " + e1);
					}
				}
			}).start();
		}
		else if (cmd.equals("Configure Attendance Values"))
		{
			JTextPane p = new JTextPane();
			p.setText(Prefs.getAttendanceCalculations());
			p.setPreferredSize(new Dimension(600,400));
			if (JOptionPane.showConfirmDialog(null, new JScrollPane(p), "Enter Requested Attendance Calculations", JOptionPane.OK_CANCEL_OPTION)
						== JOptionPane.OK_OPTION) {
				Prefs.setAttendanceCalculations(p.getText());
				Messenger.sendEvent(MT.ATTENDANCE_SETUP_CHANGE, null);
			}
		}
		else if (cmd.equals("Show Attendance Values"))
		{
			if (((JCheckBoxMenuItem)e.getSource()).isSelected())
			{
				getContentPane().add(attendanceDisplay, BorderLayout.EAST);
				pack();
				Messenger.sendEvent(MT.ATTENDANCE_SETUP_CHANGE, null);
			}
			else
			{
				getContentPane().remove(attendanceDisplay);
				pack();
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
			final String name = "Registration " + ((args.length>1)?args[1]:"");
			SwingUtilities.invokeLater(new Runnable() { public void run() {
				try {
					new Registration(name);
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

