/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2011 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dataentry;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.wwscc.storage.Run;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;

/**
 *
 * @author bwilson
 */
public class FakeUser implements MessageListener
{
	private static final Logger log = Logger.getLogger(FakeUser.class.getCanonicalName());
	
	TimeEntry time;
	RunsTable table;
	Run toset;
	UserThread thread;
	int waitTime = 2000;
        long all = 0;
        long min = 1000;
        long max = 0;
        long cnt = 0;

	public FakeUser(RunsTable t, TimeEntry e)
	{
		time = e;
		table = t;
		toset = new Run(0.501, 1.900, 28.735, 0, 0, "OK");

		Messenger.register(MT.START_FAKE_USER, this);
		Messenger.register(MT.STOP_FAKE_USER, this);
		Messenger.register(MT.CONFIGURE_FAKE_USER, this);
	}

	@Override
	public void event(MT type, Object data) {
		switch (type)
		{
			case START_FAKE_USER:
				if (thread == null)
				{
					thread = new UserThread();
					new Thread(thread).start();
				}
				thread.running = true;
				break;
			case STOP_FAKE_USER:
				thread.running = false;
				break;
			case CONFIGURE_FAKE_USER:
				waitTime = Integer.parseInt(JOptionPane.showInputDialog(null, "Enter interval time in ms", "Fake User Wait Time", JOptionPane.INFORMATION_MESSAGE));
                                min = 1000;
                                max = 0;
                                all = 0;
                                cnt = 0;
				break;
		}
	}

	class UserThread implements Runnable
	{
		boolean running = false;
		@Override
		public void run()
		{	while (true) { try {

			if (running)
			{
				long a = System.currentTimeMillis();
				// get the current table selection
				int col = table.getSelectedColumn();
				int row = table.getSelectedRow();

				// Initiate set via time entry
				time.setValues(toset);
				time.getEnterButton().doClick();

				// change back to previous selection
				table.changeSelection(row, col, false, false);

                                long t = System.currentTimeMillis() - a;
                                if (t > max) max = t;
                                if (t < min) min = t;
                                all += t;
                                cnt++;
				log.info(String.format("%d (%d, %d, %d)", t, min, all/cnt, max));
			}

			Thread.sleep(waitTime);

			} catch (Exception x) {
				log.log(Level.SEVERE, "err: {0}", x.getMessage());
			}}
		}
	}

}
