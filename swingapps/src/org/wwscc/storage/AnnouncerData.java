/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

public class AnnouncerData
{
	protected int id;
	protected int eventid;
	protected int carid;
	protected double rawdiff;
	protected double netdiff;
	protected double oldsum;
	protected double potentialsum;
	protected double olddiffpoints;
	protected double potentialdiffpoints;
	protected int oldpospoints;
	protected int potentialpospoints;
	protected SADateTime updated;
	
	public double getRawDiff() { return rawdiff; }
	public double getNetDiff() { return netdiff; }
	public double getOldSum() { return oldsum; }
	public double getPotentialSum() { return potentialsum; }
}

