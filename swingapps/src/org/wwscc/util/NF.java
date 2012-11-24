/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wwscc.util;

import java.text.NumberFormat;

/**
 *
 * @author bwilson
 */
public class NF 
{
	static final NumberFormat df;
	static
	{
		df = NumberFormat.getNumberInstance();
		df.setMinimumFractionDigits(3);
		df.setMaximumFractionDigits(3);
	}
	
	static public String format(Double d)
	{
		return df.format(d);
	}
}
