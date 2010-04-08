/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.util.logging.Logger;

public class EventResult
{
	private static Logger log = Logger.getLogger("org.wwscc.storage.EventResults");

	protected int id;
	protected String firstname;
	protected String lastname;
	protected int position;
	protected int courses; /* How many courses in the calculation */
	protected double sum;
	protected double diff;
	protected double points;
	protected int ppoints;
	protected SADateTime updated;

	public String getFullName() { return firstname + " " + lastname; }
	public double getSum() { return sum; }
	public double getDiff() { return diff; }
}

