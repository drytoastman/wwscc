/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 */
public class ByteWriter extends ByteArrayOutputStream
{
	public ByteWriter()
	{
		super();
	}

	/**
	 * Allow clearing of buffered data.
	 */
	public void clear()
	{
		count = 0;
	}

	/**
	 * Return the array itself rather than a copy as this could be pretty big
	 * and we just write it out to stream.
	 * @return the internal byte buffer
	 */
	public byte[] getByteArray()
	{
		return buf;
	}

	/**
	 * Utility string write routine
	 * @param s the string to add to the byte buffer
	 * @throws java.io.IOException
	 */
	public void write(String s) throws IOException
	{
		write(s.getBytes());
	}

	public void print(PrintStream out)
	{
		out.println(new String(buf, 0, count));
	}
}
