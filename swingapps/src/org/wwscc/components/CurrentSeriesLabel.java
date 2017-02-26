/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2010 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.components;

import javax.swing.JLabel;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;

/**
 *
 * @author bwilson
 */
public class CurrentSeriesLabel extends JLabel implements MessageListener
{
	public CurrentSeriesLabel()
	{
		Messenger.register(MT.SERIES_CHANGED, this);
		setHorizontalAlignment(CENTER);
	}

	@Override
	public void event(MT type, Object o)
	{
		switch (type)
		{
			case SERIES_CHANGED:
				setText((String)o);
				break;
		}
	}

}


