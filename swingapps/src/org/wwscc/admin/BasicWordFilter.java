/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.wwscc.admin;

import javax.swing.RowFilter;

public class BasicWordFilter extends RowFilter<Object,Object>
{
	String words[];
	public BasicWordFilter(String txt)
	{
		words = txt.split("\\s+");
		for (int ii = 0; ii < words.length; ii++)
			words[ii] = words[ii].toLowerCase();
	}

	@Override
	public boolean include(Entry<? extends Object,? extends Object> value)
	{
		for (int ii = 0; ii < value.getValueCount(); ii++)
		{
			String v = value.getStringValue(ii).toLowerCase();
			for (String w : words)
				if (v.contains(w))
					return true;
		}
		return false;
	}
}