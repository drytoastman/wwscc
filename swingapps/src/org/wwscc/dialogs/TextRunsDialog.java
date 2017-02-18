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
import org.wwscc.util.IdGenerator;

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
			int cones = 0, gates = 0;
			if (p.length > 1)
			{
				String pen[] = p[1].split(",");
				cones = Integer.parseInt(pen[0]);
				if (pen.length > 1)
					gates = Integer.parseInt(pen[1]);
			}
			double raw = Double.parseDouble(p[0]) - (2.0 * cones) - (10.0 * gates);  // remove penalty from time
			return new Run(raw, cones, gates, "OK");
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
					r.updateTo(-1, IdGenerator.nullid, course, run);
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

