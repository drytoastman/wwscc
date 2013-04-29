package org.wwscc.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import org.wwscc.storage.Database;

public class DatabaseCopyAction extends AbstractAction
{
	public DatabaseCopyAction()
	{
		super("Download Database Copy");
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		new Thread(new Runnable() {
			public void run() {
				Database.download(false);
			}
		}).start();
	}
}
