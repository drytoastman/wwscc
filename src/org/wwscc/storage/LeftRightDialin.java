/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.wwscc.storage;

import java.text.NumberFormat;

/**
 *
 * @author bwilson
 */
public class LeftRightDialin implements Serial
{
	static NumberFormat df;
	static
	{
		df = NumberFormat.getNumberInstance();
		df.setMinimumFractionDigits(3);
		df.setMaximumFractionDigits(3);
	}
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
		return df.format(left) + " " + df.format(right);
	}

	@Override
	public void decode(String s)
	{
		String bits[] = s.split("\\s+");
		left = new Double(bits[0]);
		right = new Double(bits[1]);
	}
}