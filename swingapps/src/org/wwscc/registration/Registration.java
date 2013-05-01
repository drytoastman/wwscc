/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2010 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.registration;

import java.awt.AWTKeyStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.FocusManager;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.wwscc.actions.BarcodeScannerOptionsAction;
import org.wwscc.actions.DatabaseCopyAction;
import org.wwscc.actions.DatabaseOpenAction;
import org.wwscc.actions.QuitAction;
import org.wwscc.barcodes.BarcodeScannerWatcher;
import org.wwscc.dialogs.DatabaseDialog;
import org.wwscc.registration.attendance.Attendance;
import org.wwscc.registration.attendance.AttendancePanel;
import org.wwscc.registration.changeviewer.ChangeViewer;
import org.wwscc.storage.Database;
import org.wwscc.storage.RemoteHTTPConnection;
import org.wwscc.util.CancelException;
import org.wwscc.util.Logging;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;
import org.wwscc.util.Prefs;


public class Registration extends JFrame
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
		file.add(new DatabaseOpenAction());
		file.add(new DatabaseCopyAction());
		file.add(new JSeparator());
		file.add(new QuitAction());
		
		JMenu find = new JMenu("Find By...");
		find.add(new FindByAction("Membership"));
		find.add(new FindByAction("DriverId"));
		find.add(new FindByAction("CarId"));
		
		JMenu options = new JMenu("Options");
		options.add(new BarcodeScannerOptionsAction());
		
		JMenu attendance = new JMenu("Attendance");
		attendance.add(new AttendanceDownloadAction());
		attendance.add(new AttendanceConfigureAction());
		attendance.add(new JCheckBoxMenuItem(new AttendanceShowAction()));
		
		JMenu merge = new JMenu("Merge");
		merge.add(new ChangeViewerAction());
		merge.add(new LocalDatabaseCopyAction());
		
		JMenuBar bar = new JMenuBar();
		bar.add(file);
		bar.add(find);
		bar.add(options);
		bar.add(attendance);
		bar.add(merge);
		setJMenuBar(bar);

		Database.openDefault();
		pack();
		setVisible(true);
	}


	class FindByAction extends AbstractAction
	{
		String type;
		char prefix;
		public FindByAction(String t) 
		{ 
			super();
			type = t;
			prefix = type.charAt(0);
			putValue(NAME, type);
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(AWTKeyStroke.getAWTKeyStroke(prefix).getKeyChar(), ActionEvent.CTRL_MASK));				 
		}
		
		public void actionPerformed(ActionEvent e) 
		{
			Object o = JOptionPane.showInputDialog("Enter " + type);
			if (o != null)
				Messenger.sendEvent(MT.BARCODE_SCANNED, (prefix != 'M') ? prefix+o.toString() : o);
		}
	}
	
	
	class ChangeViewerAction extends AbstractAction
	{
		public ChangeViewerAction() { super("Open Change Viewer"); }
		public void actionPerformed(ActionEvent e) {  new ChangeViewer(Database.d.getCurrentSeries()); }
	}
	
	class AttendanceDownloadAction extends AbstractAction
	{
		public AttendanceDownloadAction() { super("Download Attendance History"); }
		@Override
		public void actionPerformed(ActionEvent e)
		{
			new Thread(new Runnable() {
				public void run() {
					try {						
						Attendance.getAttendance(Database.getHost());
					} catch (CancelException ce) {
					} catch (Exception e1) {
						log.severe("Failed to download attendance: " + e1);
					}
				}
			}).start();
		}
	}
	
	class AttendanceConfigureAction extends AbstractAction
	{
		public AttendanceConfigureAction() { super("Configure Attendance Values"); }
		@Override
		public void actionPerformed(ActionEvent e)
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
	}
	
	class AttendanceShowAction extends AbstractAction
	{
		public AttendanceShowAction() { super("Show Attendance Values"); }
		@Override
		public void actionPerformed(ActionEvent e)
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
	}
	
	class LocalDatabaseCopyAction extends AbstractAction
	{
		public LocalDatabaseCopyAction() { super("Get Fresh Copy"); }
		public void actionPerformed(ActionEvent e)
		{
			try
			{
				String spec[] = DatabaseDialog.netLookup("Get New Copy From Remote", Prefs.getMergeHost()+"/"+Database.d.getCurrentSeries());
				Database.download(new RemoteHTTPConnection(spec[0]), spec[1], false);
				JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(), "Copy complete");
			} catch (CancelException ce) {
				return;
			} catch (Exception bige) {
				log.log(Level.SEVERE, "Copy failed: " + bige.getMessage(), bige);
			}
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

