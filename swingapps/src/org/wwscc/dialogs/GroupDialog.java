/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dialogs;

import java.awt.GridLayout;
import javax.swing.JCheckBox;


/**
 * Core functions for all dialogs.
 */
public class GroupDialog extends BaseDialog<int[]>
{
	//private static Logger log = Logger.getLogger("org.wwscc.dialogs.GroupDialog");

	private static final int groupCount = 6;
	JCheckBox groupchecks[];

	/**
	 * Create the dialog.
	 * @param parent	the parent Frame if any
	 * @param d			the driver data to source initially
	 */
    public GroupDialog()
	{
        super(new GridLayout(1,groupCount), true);

		groupchecks = new JCheckBox[groupCount];
		for (int ii = 0; ii < groupCount; ii++)
		{
			groupchecks[ii] = new JCheckBox(""+(ii+1));
			mainPanel.add(groupchecks[ii]);
		}
    }
	
	/**
	 * Called after OK to verify data before closing.
	 */ 
	@Override
	public boolean verifyData()
	{
		return true;
	}

	/**
	 * OK was pressed, data was verified, now return it.
	 */
	@Override
	public int[] getResult()
	{
		int size = 0;
		for (int ii = 0; ii < groupCount; ii++)
			if (groupchecks[ii].isSelected())
				size++;

		result = new int[size];
		size = 0;

		for (int ii = 0; ii < groupCount; ii++)
			if (groupchecks[ii].isSelected())
				result[size++] = (ii+1);
		
		return result;
	}
}
