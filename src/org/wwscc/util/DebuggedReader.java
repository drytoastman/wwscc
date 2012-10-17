/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 */
public class DebuggedReader extends BufferedReader
{
	private boolean debug;
	public DebuggedReader(Reader in, boolean d)
	{
		super(in);
		debug = d;
	}

	public void setDebug(boolean d)
	{
		debug = d;
	}

	@Override
	public String readLine() throws IOException
	{
		String s = super.readLine();
		if (debug && s != null)
			System.out.println(s);
		return s;
	}
}

