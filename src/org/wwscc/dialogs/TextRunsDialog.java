/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;
import net.miginfocom.swing.MigLayout;
import org.wwscc.storage.Run;

/**
 * Core functions for all dialogs.
 */
public class TextRunsDialog extends BaseDialog<List<Run>>
{
	JTextArea area;
	Logger log = Logger.getLogger(TextRunsDialog.class.getName());

	/**
	 */
    public TextRunsDialog()
	{
        super(new MigLayout(), true);
		area = new JTextArea();
		mainPanel.add(area, "hmin 200, wmin 500");
    }

	private Run txtToRun(String data) throws NumberFormatException
	{
		if (Character.isDigit(data.charAt(0)))
		{
			String p[] = data.split("[()]+");
			int cones = (p.length>1)?Integer.parseInt(p[1]):0;
			double raw = Double.parseDouble(p[0]) - (2.0 * cones);  // remove penalty from time
			return new Run(raw, cones, 0, "OK");
		}
		else
		{
			return new Run(0, 0, 0, data);
		}
	}

	/**
	 * Called after OK to verify data before closing.
	 */ 
	@Override
	public boolean verifyData()
	{
		try
		{
			result = new ArrayList<Run>();
			String lines[] = area.getText().split("\n");
			int course = 1;
			for (String line : lines)
			{
				int run = 1;
				for (String rdata : line.split("\\s+"))
				{
					Run r = txtToRun(rdata);
					r.updateTo(0, course, run, 0, 1.0);
					result.add(r);
					run++;
				}
				course++;
			}
			return true;
		}
		catch (Exception e)
		{
			errorMessage = "Can't parse textual run data: " + e;
			log.log(Level.INFO, "Can't parse textual run data: " + e, e);
			return false;
		}
	}


	/**
	 * Called after OK is pressed and before the dialog is closed.
	 */
	@Override
	public List<Run> getResult()
	{
		if (!valid) return null;
		return result;
	}

}

