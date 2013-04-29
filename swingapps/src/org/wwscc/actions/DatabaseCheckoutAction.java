package org.wwscc.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import org.wwscc.storage.Database;

public class DatabaseCheckoutAction extends AbstractAction
{
	public DatabaseCheckoutAction()
	{
		super("Download and Lock Database");
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		new Thread(new Runnable() {
			@Override
			public void run() {
				Database.download(true);
			}
		}).start();
	}
}
