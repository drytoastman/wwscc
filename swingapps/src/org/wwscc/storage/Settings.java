/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.util.Properties;

/**
 * An abstraction of the Properties class that stores itself in the database so settings go with it.
 */
public class Settings extends Properties
{
	private static final long serialVersionUID = -7290068895174856883L;

	public Settings()
	{
	}
}

