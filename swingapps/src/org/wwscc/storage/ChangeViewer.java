package org.wwscc.storage;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.wwscc.util.Logging;
import org.wwscc.util.TableUtilities;

public class ChangeViewer extends JTable
{

	public static void main(String args[]) throws Exception
	{
		JFrame f = new JFrame("Change Viewer");
		List<Change> changes = ChangeTracker.readInFile(new File(Logging.getLogDir(), "nwr2013changes.log.0"));
		DefaultTableModel m = new DefaultTableModel(new Object[] { "Type", "Arg1", "Arg2" }, 0);
		for (Change c : changes) {
			Serializable s[] = c.getArgs();
			m.addRow(new Object[] { c.getType(), s[0], (s.length > 1) ? s[1] : "" });
		}

		JTable tbl = new JTable(m);
		TableUtilities.setPreferredRowHeights(tbl);
		f.add(tbl);
		f.pack();
		f.setVisible(true);
	}
}
