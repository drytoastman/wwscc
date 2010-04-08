/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2010 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

/**
 */
public class Change
{
	protected int id;
	protected String type;
	protected Object arg;

	public String getType() { return type; }
	public Object getArg() { return arg; }
}
