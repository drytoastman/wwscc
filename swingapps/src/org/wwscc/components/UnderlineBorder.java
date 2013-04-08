/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.components;

import java.awt.*;
import javax.swing.border.*;

/**
 * Construct a 'border' which just draws the bottom line.
 */
public class UnderlineBorder extends EmptyBorder
{
	Color color;
	
	public UnderlineBorder()
	{
		super(0, 0, 0, 0);
		color = Color.BLACK;
	}
	
	public UnderlineBorder(Color c)
	{
		super(0, 0, 0, 0);
		color = c;
	}

	public UnderlineBorder(int top, int left, int bottom, int right)
	{
		super(top, left, bottom, right);
	}

	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
	{
		Color oldColor = g.getColor();
		int h = y+height-1;

		g.setColor(color);
		g.drawLine(x, h, x+width-1, h);
		g.setColor(oldColor);
	}
}



