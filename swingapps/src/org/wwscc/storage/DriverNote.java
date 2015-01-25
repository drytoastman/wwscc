/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2014 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.io.Serializable;

public class DriverNote implements Serializable
{
	private static final long serialVersionUID = -7204547717798961852L;
	
	protected int driverid;
	protected String notes;

	public DriverNote() { driverid = -1; notes = ""; }
	public DriverNote(int id, String n) { driverid = id; notes = n; }
	public int getDriverId() { return driverid; }
	public String getNotes() { return notes; }
}

