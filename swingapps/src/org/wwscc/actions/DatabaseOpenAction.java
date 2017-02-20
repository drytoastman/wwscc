package org.wwscc.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.wwscc.storage.Database;
import org.wwscc.storage.PostgresqlDatabase;

public class DatabaseOpenAction extends AbstractAction
{
	public DatabaseOpenAction()
	{
		super("Open Database");
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		String options[] = PostgresqlDatabase.getSeriesList().toArray(new String[0]);
		String series = (String)JOptionPane.showInputDialog(null, "Select the series", "Series Selection", JOptionPane.QUESTION_MESSAGE, null, options, null);
		Database.openDatabase(series, series);
	}
}
