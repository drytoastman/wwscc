/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.io.Serializable;

public class DriverField implements Serializable
{
	private static final long serialVersionUID = -7204547717798961851L;
	
	protected int id;
	protected String name;
	protected String title;
	protected String type;

	public DriverField() {}
	public int getId() { return id; }
	public String getName() { return name; }
	public String getTitle() { return title; }
	public String getType() { return type; }
}

