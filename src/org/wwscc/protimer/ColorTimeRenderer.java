/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */


package org.wwscc.protimer;

import java.awt.Color;
import java.awt.Component;
import java.util.logging.Logger;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import org.wwscc.util.NF;


/**
 * Cell renderer for a Double that always creates three decimal places (123.000)
 */
public class ColorTimeRenderer extends DefaultTableCellRenderer
{
	private static final Logger log = Logger.getLogger(ColorTimeRenderer.class.getCanonicalName());
	private int size;

	/**
	 * Create the renderer using a particular font, if given.
	 */
	public ColorTimeRenderer(int fsize)
	{
		super();
		setHorizontalAlignment(TRAILING);
		size = fsize;
	}

	@Override
	public Component getTableCellRendererComponent(JTable tbl, Object o, boolean is, boolean hf, int r, int c)
	{
		setBackground(Color.WHITE);
		ResultsModel m = (ResultsModel)tbl.getModel();
		if (((c == 2) && (r == m.getLastLeftFinish())) ||
			((c == 6) && (r == m.getLastRightFinish())))
		{
			setBackground(Color.LIGHT_GRAY);
		}

		if (o instanceof ColorTime)
		{
			ColorTime bt = (ColorTime)o;
			if (Double.isNaN(bt.time))
			{
				setText("");
			}
			else
			{
				String color = bt.getColorString();
				String time = NF.format(bt.time);
				String msg = bt.getColorMsg();

				if ((bt.state == ColorTime.NORMAL) && !Double.isNaN(bt.dial))
				{
					setText("<HTML><FONT face=fixed size=+" + size + " color=" + color + ">" + time + " </FONT></HTML>");
					setText("<HTML><FONT face=fixed size=+"+size+" color="+color+">"+time+" </FONT>" +
							"<br><center><FONT size=3 color="+color+">dial "+NF.format(bt.dial)+"</FONT></center></HTML>");
				}
				else if (bt.state == ColorTime.NORMAL)
				{
					setText("<HTML><FONT face=fixed size=+" + size + " color=" + color + ">" + time + " </FONT></HTML>");
				}
				else
				{
					setText("<HTML><FONT face=fixed size=+"+size+" color="+color+">"+time+" </FONT>" +
							"<br><center><FONT size=3 color="+color+">"+msg+"</FONT></center></HTML>");
				}
			}
		}
		else
			setText("");

		return this;
	}
}

