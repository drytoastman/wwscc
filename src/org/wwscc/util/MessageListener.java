/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */


package org.wwscc.util;

/**
 * Interface that all message listeners must implement.
 */
public interface MessageListener
{
	/**
	 * Process an event that the object registered for.
	 * @param type the event type
	 * @param data the event data, dependent on type
	 */
	public void event(MT type, Object data);
}

