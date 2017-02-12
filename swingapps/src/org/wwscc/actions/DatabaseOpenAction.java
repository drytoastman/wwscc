package org.wwscc.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.wwscc.storage.Database;

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
		Database.openDefault();
	}
}
