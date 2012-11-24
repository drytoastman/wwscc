/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2010 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.components;

import javax.swing.JLabel;
import org.wwscc.storage.Database;
import org.wwscc.storage.WebDataSource;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;

/**
 *
 * @author bwilson
 */
public class CurrentDatabaseLabel extends JLabel implements MessageListener
{
	public CurrentDatabaseLabel()
	{
		Messenger.register(MT.DATABASE_CHANGED, this);
		setHorizontalAlignment(CENTER);
	}

	@Override
	public void event(MT type, Object o)
	{
		switch (type)
		{
			case DATABASE_CHANGED:
				if (Database.d instanceof WebDataSource)
					setText(Database.d.getCurrentHost()+"/"+Database.d.getCurrentSeries());
				else if (Database.file != null)
					setText(Database.file.getName());
				else
					setText("No database");
				break;
		}
	}

}


