package org.wwscc.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import org.wwscc.storage.Database;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;

public class DatabaseCheckinAction extends AbstractAction implements MessageListener
{
	public DatabaseCheckinAction()
	{
		super("Upload and Unlock Database");
		Messenger.register(MT.DATABASE_CHANGED, this);
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		new Thread(new Runnable() {
			@Override
			public void run() {
				Database.upload();
			}
		}).start();
	}

	@Override
	public void event(MT type, Object data)
	{
		setEnabled(Database.file != null);
	}
}
