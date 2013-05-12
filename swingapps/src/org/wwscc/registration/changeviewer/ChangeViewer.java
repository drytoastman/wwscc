package org.wwscc.registration.changeviewer;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.FocusManager;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.wwscc.dialogs.DatabaseDialog;
import org.wwscc.storage.ChangeTracker;
import org.wwscc.storage.Database;
import org.wwscc.storage.WebDataSource;
import org.wwscc.util.CancelException;
import org.wwscc.util.Prefs;

public class ChangeViewer extends JFrame
{
	private static Logger log = Logger.getLogger(ChangeViewer.class.getCanonicalName());
	
	ChangeTable table;
	JList<DatedChangeList> list;
	String dbname;
	JButton merge;
	JButton copy;
	
	public ChangeViewer(String db)
	{
		super("Change Viewer (" + db + ")");
		setLayout(new MigLayout("fill", "[grow 0][fill, 600]", "[fill][grow 0]"));
		
		dbname = db;
		list = new JList<DatedChangeList>();
		table = new ChangeTable();
		merge = new JButton(new MergeAction());
		list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				DatedChangeList active = list.getSelectedValue();
				if (active != null) table.setData(active.changes);				
		}});

		add(new JScrollPane(list), "w 180!");
		add(new JScrollPane(table), "wrap");
		add(new JButton(new MergeAction()), "skip, split 2");
		
		pack();
		loadFiles();
		setLocationRelativeTo(FocusManager.getCurrentManager().getActiveWindow());
		setVisible(true);
	}

	
	protected void loadFiles()
	{
		DefaultListModel<DatedChangeList> model = new DefaultListModel<DatedChangeList>();
		for (int ii = 0; ii < ChangeTracker.HISTORYLENGTH; ii++)
		{
			File f = ChangeTracker.generateFileName(dbname, ii);
			try {
				if (f.exists())
					model.addElement(new DatedChangeList(ii, f.lastModified(), ChangeTracker.readInFile(f)));
			} catch (Exception e) {
				log.log(Level.WARNING, "Unable to load changes from {0}: {1}", new Object[] { f, e.getMessage() });
			}
		}
		list.setModel(model);
		list.setSelectedIndex(0);
	}
	
	protected void archive()
	{
		if (list.getSelectedValue().index == 0) // if merging active list, archive it now
		{
			try {
				new ChangeTracker(dbname).archiveChanges();
				loadFiles();
			} catch (Exception e2) {
				log.warning("Archive failed: " + e2.getMessage());
			}
		}
	}
	
	class MergeAction extends AbstractAction
	{
		public MergeAction() { super("Merge Changes"); }
		public void actionPerformed(ActionEvent e)
		{
			try
			{
				String spec[] = DatabaseDialog.netLookup("Merge Copy To Remote", Prefs.getMergeHost()+"/"+Database.d.getCurrentSeries());				
				final WebDataSource dest = new WebDataSource(spec[0], spec[1]);
				dest.sendEvents(false); // don't want to know about inserts, etc
				new MergeProcess(ChangeViewer.this, list.getSelectedValue().changes, dest).execute();
			} catch (CancelException ce) {
				return;
			} catch (IOException ioe) {
				log.log(Level.SEVERE, "Merge host select failed: " + ioe.getMessage(), ioe);
			}
		}
	}
}

