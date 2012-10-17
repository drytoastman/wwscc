/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.protimer;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;


public class AuditLog implements MessageListener
{
	private static final Logger log = Logger.getLogger(AuditLog.class.getCanonicalName());

	public PrintWriter audit;
	public DateFormat dformat;

	public AuditLog()
	{
		try {
			audit = new PrintWriter(new FileWriter("audit.log", true));
		} catch (IOException ioe) {
			log.log(Level.WARNING, "Can't open audit log: " + ioe, ioe);
			return;
		}

		dformat = new SimpleDateFormat("MM/dd HH:mm:ss");

		Messenger.register(MT.FINISH_LEFT, this);
		Messenger.register(MT.FINISH_RIGHT, this);
		Messenger.register(MT.DELETE_FINISH_LEFT, this);
		Messenger.register(MT.DELETE_FINISH_RIGHT, this);
	}


	@Override
	public void event(MT type, Object o)
	{
		try 
		{
			String d = dformat.format(new Date());

			switch (type)
			{
				case FINISH_LEFT:			audit.printf("%15s:  %03.3f\n", d, ((ColorTime)((Object[])o)[0]).time); break;
				case FINISH_RIGHT:			audit.printf("%15s:          %03.3f\n", d, ((ColorTime)((Object[])o)[0]).time); break;
				case DELETE_FINISH_LEFT:	audit.write(d + " *** DELETE left finish\n"); break;
				case DELETE_FINISH_RIGHT:	audit.write(d + " *** DELETE right finish\n"); break;
			}

			audit.flush();
		}
		catch (Exception e)
		{
			log.log(Level.INFO, "audit log error: {0}", e);
		}
	}
}

