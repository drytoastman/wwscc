package org.wwscc.registration.changeviewer;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.wwscc.dialogs.DatabaseDialog;
import org.wwscc.storage.Change;
import org.wwscc.storage.ChangeTracker;
import org.wwscc.storage.Database;
import org.wwscc.storage.WebDataSource;
import org.wwscc.util.Prefs;

public class ChangeViewer extends JFrame
{
	private static Logger log = Logger.getLogger(ChangeViewer.class.getCanonicalName());
	
	ChangeTable table;
	JList<DatedChangeList> list;
	String dbname;
	JButton merge;
	
	class DatedChangeList
	{
		int index;
		Date date;
		List<Change> changes;
		
		public DatedChangeList(int ii, long mod, List<Change> list)
		{
			index = ii;
			date = new Date(mod);
			changes = list;
		}
		
		public String toString() 
		{
			if (index == 0) return "current";
			return String.format("%s - %s", index, date); 
		}
	}
	
	public ChangeViewer(String db)
	{
		super("Change Viewer (" + db + ")");
		setLayout(new MigLayout("fill", "[grow 0][fill]", "[fill][grow 0]"));
		
		dbname = db;
		list = new JList<DatedChangeList>();
		table = new ChangeTable();
		merge = new JButton(new MergeAction());
		loadFiles();

		list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				table.setData(list.getSelectedValue().changes);				
		}});

		add(new JScrollPane(list), "w 100!");
		add(new JScrollPane(table), "wrap");
		add(new JButton(new MergeAction()), "skip, al right");
		
		pack();
		setVisible(true);
	}

	
	class MergeAction extends AbstractAction
	{
		public MergeAction()
		{
			super("Merge Changes");
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			try
			{
				DatabaseDialog dd = new DatabaseDialog(null, Prefs.getMergeHost()+"/"+Database.d.getCurrentSeries(), true);
				String ret = (String)dd.getResult();
				String spec[] = ret.split("/");
				
				WebDataSource dest = new WebDataSource(spec[0], spec[1]);
				dest.mergeChanges(list.getSelectedValue().changes);
				if (list.getSelectedValue().index != 0)
					new ChangeTracker(dbname).archiveChanges();
			}
			catch (Exception bige)
			{
				log.log(Level.SEVERE, "Merge failed: " + bige.getMessage(), bige);
			}
		}
	}

	
	void loadFiles()
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
	}
}

