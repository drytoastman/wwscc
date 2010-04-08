/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class DoubleLabel extends JComponent
{
	private String topLine;
	private String bottomLine;
	private Font lowerFont;
	
	public DoubleLabel(String top, String bottom)
	{
		super();
		topLine = top;
		bottomLine = bottom;
		
		setFont((Font)UIManager.get("Label.font"));
		setBackground((Color)UIManager.get("Label.background"));
		setForeground((Color)UIManager.get("Label.foreground"));
		setOpaque(true);
	}
	
	public void setTopLine(String s)
	{
		topLine = s;
		invalidate();
	}
	
	public void setBottomLine(String s)
	{
		bottomLine = s;
		invalidate();
	}

	@Override
	public void setFont(Font f)
	{
		super.setFont(f);
		lowerFont = f;
	}
	
	public void setFont(Font f1, Font f2)
	{
		super.setFont(f1);
		lowerFont = f2;
	}

	@Override
	public Dimension getMinimumSize()
	{
		return getPreferredSize();
	}
	
	@Override
	public Dimension getPreferredSize()
	{
		FontMetrics tm = getFontMetrics(getFont());
		FontMetrics bm = getFontMetrics(lowerFont);
										 
		int width = Math.max(SwingUtilities.computeStringWidth(tm, topLine), 
							SwingUtilities.computeStringWidth(bm, bottomLine));
		int height = tm.getHeight() + bm.getHeight();
		return new Dimension(width+5, height+5);
	}
	
	@Override
	public void paint(Graphics g1)
	{
		Graphics2D g = (Graphics2D)g1;
		Dimension size = getSize();
		
		if (isOpaque())
		{
			g.setColor(getBackground());
			g.fillRect(0, 0, size.width, size.height);
		}
		
		g.setColor(getForeground());
		Font topFont = getFont();
		
		int topHeight = g.getFontMetrics(topFont).getHeight();
		int botHeight = g.getFontMetrics(lowerFont).getHeight();
		
		if ((topLine != null) && (bottomLine != null))
		{
			g.setFont(topFont);
			g.drawString(topLine, 5, size.height/2 - 1);
			g.setFont(lowerFont);
			g.drawString(bottomLine, 5, size.height/2 + 1 + botHeight);
		}
		else if (topLine != null)
		{
			g.setFont(topFont);
			g.drawString(topLine, 5, size.height/2 + topHeight/2);
		}
		else if (bottomLine != null)
		{
			g.setFont(lowerFont);
			g.drawString(bottomLine, 5, size.height/2 + botHeight/2);
		}
	}
}