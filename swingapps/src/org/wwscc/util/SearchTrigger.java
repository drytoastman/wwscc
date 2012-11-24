/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.wwscc.util;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

/**
 *
 * @author bwilson
 */
public abstract class SearchTrigger implements DocumentListener
{
	private boolean enabled = true;

	@Override
	public void changedUpdate(DocumentEvent e) { if (enabled) search(e); }
	@Override
	public void insertUpdate(DocumentEvent e) { if (enabled) search(e); }
	@Override
	public void removeUpdate(DocumentEvent e) { if (enabled) search(e); }

	public void enable(boolean e)
	{
		enabled = e;
	}

	public void search(DocumentEvent e)
	{
		Document d = e.getDocument();
		try { search(d.getText(0, d.getLength())); }
		catch (Exception ex) {}
	}

	public abstract void search(String txt);
}