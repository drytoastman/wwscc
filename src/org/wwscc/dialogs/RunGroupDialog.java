/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2010 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dialogs;

import java.awt.Font;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import net.miginfocom.swing.MigLayout;
import org.wwscc.storage.Database;

/**
 */
public class RunGroupDialog extends BaseDialog<Map<String,Integer>>
{
	Map<String, ButtonGroup> selections;

	/**
	 * Create the dialog.
	 *
	 * @param current
	 */
    public RunGroupDialog(Map<String,Integer> current)
	{
        super(new MigLayout(""), false);

		mainPanel.add(boldLabel("Class"));
		mainPanel.add(boldLabel("Group 1"));
		mainPanel.add(boldLabel("Group 2"), "wrap");

		selections = new HashMap<String, ButtonGroup>();
		List<String> codes = Database.d.getClassData().getClassCodes();
		Collections.sort(codes);
		for (String code : codes)
		{
			JRadioButton b1 = new JRadioButton();
			b1.setActionCommand("1");
			JRadioButton b2 = new JRadioButton();
			b2.setActionCommand("2");
			ButtonGroup g = new ButtonGroup();
			g.add(b1);
			g.add(b2);
			Integer rg = current.get(code);
			if ((rg != null) && (rg == 2))
				b2.setSelected(true);
			else
				b1.setSelected(true);
			selections.put(code, g);
			mainPanel.add(new JLabel(code), "al right");
			mainPanel.add(b1, "al center");
			mainPanel.add(b2, "al center, wrap");
		}
    }

	private JLabel boldLabel(String s)
	{
		JLabel l = new JLabel(s);
		l.setFont(l.getFont().deriveFont(Font.BOLD));
		return l;
	}
	
	/**
	 * Called after OK to verify data before closing.
	 * @return
	 */ 
	@Override
	public boolean verifyData()
	{
		return true;
	}

	/**
	 * OK was pressed, data was verified, now return it.
	 * @return
	 */
	@Override
	public Map<String,Integer> getResult()
	{
		result = new HashMap<String, Integer>();
		for (String code : selections.keySet())
			result.put(code, new Integer(selections.get(code).getSelection().getActionCommand()));
		return result;
	}
}
