/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */


package org.wwscc.util;

import java.util.List;
import java.util.logging.Logger;

import javax.swing.JTextField;
import javax.swing.text.*;


public class AutoTextField extends JTextField 
{
	private static final Logger log = Logger.getLogger(AutoTextField.class.getCanonicalName());
	private List<?> dataList;

	class AutoDocument extends PlainDocument 
	{
		public void replace(int i, int j, String s, AttributeSet attributeset) throws BadLocationException 
		{
			super.remove(i, j);
			insertString(i, s, attributeset);
		}

		public void insertString(int ii, String s, AttributeSet attributeset) throws BadLocationException 
		{
			if (s == null || "".equals(s))
				return;

			String base = getText(0, ii);
			String match = getMatch(base + s);

			if (match == null) 
			{
				super.insertString(ii, s, attributeset);
				return;
			}

			int jj = ii + s.length();

			super.remove(0, getLength());
			super.insertString(0, base + s + match.substring(jj), attributeset);
			setSelectionStart(jj);
			setSelectionEnd(getLength());
		}
	}

	public AutoTextField(List<?> list) 
	{
		dataList = list;
		setDocument(new AutoDocument());
	}

	private String getMatch(String s) 
	{
		if (dataList == null)
			return null;

		for (int i = 0; i < dataList.size(); i++) 
		{
			String s1 = dataList.get(i).toString();
			if ((s1 != null) && s1.toLowerCase().startsWith(s.toLowerCase()))
				return s1;
		}

		return null;
	}

	public void replaceSelection(String s) 
	{
		AutoDocument doc = (AutoDocument)getDocument();
		if (doc == null)
			return;

		try 
		{
			int i = Math.min(getCaret().getDot(), getCaret().getMark());
			int j = Math.max(getCaret().getDot(), getCaret().getMark());
			doc.replace(i, j - i, s, null);
		}
		catch (Exception e) 
		{
			log.info("Error replacing selection: " + e.getMessage());
		}
	}
}
