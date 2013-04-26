/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2013 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.FocusManager;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.wwscc.components.UnderlineBorder;

import net.miginfocom.swing.MigLayout;

public class MergeDialog extends JDialog implements ActionListener
{
	private static Logger log = Logger.getLogger(MergeDialog.class.getCanonicalName());
	
	JPanel content;
	
	JLabel connect;
	JLabel merge;
	JLabel wait;
	JLabel copy;
	JLabel done;
	JLabel error;
	
	JButton ok;
	
	JLabel steps[];
	
	public MergeDialog()
	{
		super(FocusManager.getCurrentManager().getActiveWindow(), "Merge Process");
		content = new JPanel(new MigLayout("gap 15"));
		content.setBorder(new EmptyBorder(0, 10, 0, 10));
		setContentPane(content);
		
		connect = new JLabel("1. Connecting to remote host");
		merge = new JLabel("2. Merging data");
		wait = new JLabel("3. Merge complete.  Wait for other merges, then continue");
		copy = new JLabel("4. Downloading updated copy");
		done = new JLabel("5. Complete");
		
		steps = new JLabel[] { connect, merge, wait, copy, done };
		Font base = connect.getFont().deriveFont(14f);
		for (JLabel l : steps) {
			l.setFont(base);			
			l.setEnabled(false);
		}
		
		error = new JLabel("Error string here");
		error.setForeground(Color.RED);
		error.setVisible(false);
		error.setFont(base.deriveFont(Font.BOLD));

		ok = new JButton("Ok");
		ok.setEnabled(false);		
		ok.addActionListener(this);

		JLabel header = new JLabel("Merge Process");
		header.setFont(base.deriveFont(Font.BOLD, 20));
		header.setHorizontalAlignment(JLabel.CENTER);
		header.setBorder(new UnderlineBorder());
		
		content.add(header, "grow, wrap");
		content.add(connect, "wrap");
		content.add(merge, "wrap");
		content.add(wait, "wrap");
		content.add(copy, "wrap");
		content.add(done, "wrap");
		content.add(error, "wrap, hidemode 3");
		content.add(ok, "al right, wrap");

		pack();
		setLocationRelativeTo(FocusManager.getCurrentManager().getActiveWindow());
		setVisible(true);
	}

	public void setError(String str)
	{
		log.info(str);
		error.setText(str);
		error.setVisible(true);
		pack();
		complete();
	}
	
	public synchronized void goNow()
	{
		notify();
	}
	
	public synchronized void waitStep(int stepX) throws InterruptedException
	{
		step(stepX);
		ok.setText("Continue");
		ok.setEnabled(true);
		wait();
		ok.setEnabled(false);
		ok.setText("Ok");
	}
	
	public void step(int stepX)
	{
		steps[stepX-1].setForeground(Color.BLACK);
		steps[stepX-1].setEnabled(true);
	}
	
	public void complete()
	{
		ok.setEnabled(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("Continue"))
		{
			goNow();
			return;
		}
		
		setVisible(false);
		dispose();
	}
	
	
	/**
	 * Merge the current open database to a remote host and then download the merged copy.
	 * @param host the hostname
	 * @param name the series name on the remote host
	 * @return true if success, false for failure
	 */
	public static boolean mergeTo(String host, String name)
	{
		MergeDialog dialog = new MergeDialog();
		
		WebDataSource dest;
		try
		{
			dialog.step(1);
			dest = new WebDataSource(host, name);
			dialog.step(2);
			
			ChangeTracker tracker = new ChangeTracker(name);
			dest.mergeChanges(tracker.getChanges());
			tracker.archiveChanges();
		}
		catch (Exception ex)
		{
			dialog.setError("Merge failure: " + ex.getMessage());
			return false;
		} 
		
		/* download the merged version from the server so we are completely synced again */
		try  
		{
			File db = Database.file;
			Database.d.close();
			
			try {
				System.gc();
				Thread.sleep(500);
			} catch (Exception e) {
				log.log(Level.INFO, "gc/sleep threw up: {0}", e.getMessage());
			}
			
			if (!db.delete())
				throw new IOException("Can't delete "+db.getPath()+" before downloading new copy.  Try rerunning merge.");

			dialog.waitStep(3);
			dialog.step(4);
			dest.getConnection().downloadDatabase(db, dest.getCurrentSeries(), false);
			Database.openDatabaseFile(db);
			
			Database.d.setChangeTracker(new ChangeTracker(Database.d.getCurrentSeries()));
			Database.d.setCurrentEvent(Database.d.getCurrentEvent());

			dialog.step(5);
			dialog.complete();
			return true;
		}
		catch (Exception e)
		{
			dialog.setError("Merge complete, but error getting new copy: " + e.getMessage());
			return false;
		}
	}
}


