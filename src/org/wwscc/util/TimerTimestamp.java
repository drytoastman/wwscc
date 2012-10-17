/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.util;

/**
 */
public class TimerTimestamp {

	int sensor;
	long timestamp;

	public TimerTimestamp(int s, long t)
	{
		sensor = s;
		timestamp = t;
	}

	public int getSensor() { return sensor; }
	public long getTimestamp() { return timestamp; }
	@Override
	public String toString() { return new String("TimerTimestamp("+sensor+") = " + timestamp); }
}
