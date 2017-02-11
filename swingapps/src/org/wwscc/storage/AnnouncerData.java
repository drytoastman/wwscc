/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.sql.Timestamp;
import java.util.UUID;

public class AnnouncerData
{
	//protected int id;
	protected UUID eventid;
	protected UUID carid;
	protected String classcode;
	protected int lastcourse;
	protected double rawdiff;
	protected double netdiff;
	protected double oldsum;
	protected double potentialsum;
	protected double olddiffpoints;
	protected double potentialdiffpoints;
	protected int oldpospoints;
	protected int potentialpospoints;
	protected Timestamp updated;
	
	public AnnouncerData()
	{
		rawdiff = netdiff = 0;
		oldsum = potentialsum = olddiffpoints = potentialdiffpoints = -1;
		oldpospoints = potentialpospoints = -1;
	}
	
	public double getRawDiff() { return rawdiff; }
	public double getNetDiff() { return netdiff; }
	public double getOldSum() { return oldsum; }
	public double getPotentialSum() { return potentialsum; }
}

