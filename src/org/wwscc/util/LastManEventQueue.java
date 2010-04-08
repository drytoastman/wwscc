/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.util;

import java.awt.*;
import java.awt.event.*;
  
public class LastManEventQueue extends EventQueue 
{
	AWTEvent lastman;

	public LastManEventQueue()
	{
		lastman = null;
	}

	public synchronized void postLastMan(Runnable runnable)
	{
		lastman = new InvocationEvent(Toolkit.getDefaultToolkit(), runnable);
	}

	@Override
	protected void dispatchEvent(AWTEvent event)
	{
		synchronized (this) 
		{
			if ((peekEvent() == null) && (lastman != null))
			{
				postEvent(lastman);
				lastman = null;
			}
		}

		super.dispatchEvent(event);
	}
}
