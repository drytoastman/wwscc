/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */


package org.wwscc.util;

import java.text.NumberFormat;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;

public class IntTextField extends JTextField
{
	public IntTextField(String initial, int cols)
	{
		super(initial, cols);
		setHorizontalAlignment(JTextField.LEADING);
		AbstractDocument doc = (AbstractDocument)getDocument();
		doc.setDocumentFilter(new EasyNumFilter(6));
	}

	public int getInt()
	{
		try { return Integer.parseInt(getText()); }
		catch (NumberFormatException nfe) {}
		return 0;	
	}

	public void setInt(int val)
	{
		setText(""+val);
	}
}

