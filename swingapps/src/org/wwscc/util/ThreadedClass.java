/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */
package org.wwscc.util;

/**
 * Notes classes that have internal threads running to perform activities
 */
public interface ThreadedClass 
{
	/** start the internal activity */
	public void start();
	/** stop the internal activity */
	public void stop();
}
