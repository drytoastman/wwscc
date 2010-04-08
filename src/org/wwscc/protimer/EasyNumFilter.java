/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */


package org.wwscc.protimer;

import java.text.*;
import java.awt.*;
import javax.swing.text.*;


/* Regular Formats and Formatters couldn't reproduce the exact restrictions *and* allowances we want */
public class EasyNumFilter extends DocumentFilter
{
	boolean isDouble;
	int beforeDotAllowance = 3;
	int afterDotAllowance = 3;

	
    public EasyNumFilter(int intAllowance)
	{
		this.isDouble = false;
		this.afterDotAllowance = intAllowance;
	}

    public EasyNumFilter(int intAllowance, int decimalAllowance)
	{
		this.isDouble = true;
		this.beforeDotAllowance = intAllowance;
		this.afterDotAllowance = decimalAllowance;
	}

    public EasyNumFilter(boolean isDouble)
	{
		this.isDouble = isDouble;
    }

	/*	restricts to:
			isDouble = true:  ###.###
			isDouble = false: ###
		but does allow empty string.
	*/
	private boolean verifyString(String s)
	{
		char buf[] = s.toCharArray();
		boolean dot = !isDouble; /* pretend we already found decimal */
		int beforedot = 0;
		int afterdot = 0;

		for (int ii = 0; ii < buf.length; ii++)
		{
			if (buf[ii] == '.')
			{
				if (dot) return false;
				dot = true;
				continue;
			}

			if ((buf[ii] < '0') || (buf[ii] > '9'))
				return false;

			if (dot)
			{
				afterdot++;
				if (afterdot > afterDotAllowance) return false;
			}
			else
			{
				beforedot++;
				if (beforedot > beforeDotAllowance) return false;
			}
		}

		return true;
	}

    public void insertString(FilterBypass fb, int offs, String str, AttributeSet a) throws BadLocationException
	{
		Document doc = fb.getDocument();
		StringBuffer s = new StringBuffer(doc.getText(0, doc.getLength()));
		s.insert(offs, str);

        if (verifyString(s.toString()))
            super.insertString(fb, offs, str, a);
        else
            Toolkit.getDefaultToolkit().beep();
    }
    
    public void replace(FilterBypass fb, int offs, int length, String str, AttributeSet a) throws BadLocationException 
	{
		Document doc = fb.getDocument();
		StringBuffer s = new StringBuffer(doc.getText(0, doc.getLength()));
		s.replace(offs, (offs+length), str);

        if (verifyString(s.toString()))
            super.replace(fb, offs, length, str, a);
        else
            Toolkit.getDefaultToolkit().beep();
    }
}

