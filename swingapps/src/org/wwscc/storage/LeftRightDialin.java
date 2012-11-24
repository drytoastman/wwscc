/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import org.wwscc.util.NF;

/**
 *
 * @author bwilson
 */
public class LeftRightDialin implements Serial
{
	public double left;
	public double right;

	public LeftRightDialin()
	{
	}

	public LeftRightDialin(double l, double r)
	{
		left = l;
		right = r;
	}

	@Override
	public String encode()
	{
		return NF.format(left) + " " + NF.format(right);
	}

	@Override
	public void decode(String s)
	{
		String bits[] = s.split("\\s+");
		left = new Double(bits[0]);
		right = new Double(bits[1]);
	}
}