/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.barcodes;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;

/**
 */
public class Code39 extends JComponent implements Printable
{
	private static final Logger log = Logger.getLogger(Code39.class.getCanonicalName());
	static public final Map<Character, String> barChar = new HashMap<Character, String>();
	static 
	{
		barChar.put('0', "nnnwwnwnn");
		barChar.put('1', "wnnwnnnnw");
		barChar.put('2', "nnwwnnnnw");
		barChar.put('3', "wnwwnnnnn");
		barChar.put('4', "nnnwwnnnw");
		barChar.put('5', "wnnwwnnnn");
		barChar.put('6', "nnwwwnnnn");
		barChar.put('7', "nnnwnnwnw");
		barChar.put('8', "wnnwnnwnn");
		barChar.put('9', "nnwwnnwnn");
		barChar.put('*', "nwnnwnwnn");
		barChar.put('-', "nwnnnnwnw");
	}

	public static final int NARROW = 1;
	public static final int WIDE = 3;
	public static final int SYMBOLWIDTH = (16 * NARROW);
	public static final int FULLWIDTH = (8 * SYMBOLWIDTH) + 1;
	
	protected String code = "";
	protected String label = "";
	
	public Code39()
	{
		setMinimumSize(new Dimension(FULLWIDTH, 50));
		setValue("", "");
	}
	
	public void setValue(String code, String label)
	{
		this.code = code;
		this.label = label;
	}
		
	@Override
	public void paintComponent(Graphics g)
	{
		int codeheight = (int)(getHeight() * 0.75);				
		if (code.isEmpty())
			return;

		String codestr = String.format("*%s*", code.toUpperCase());
		g.setColor(Color.BLACK);
		
		int xpos = ((8 - codestr.length())*SYMBOLWIDTH)/2;
		for (Character c : codestr.toCharArray())
		{
			if (!barChar.containsKey(c))
			{
				continue;
			}
			String seq = barChar.get(c);				
			boolean draw = true;
			for (Character bar : seq.toCharArray())
			{				
				int width = (int)((bar == 'n') ? NARROW : WIDE);	
				if (draw) g.fillRect(xpos, 0, width, codeheight);
				xpos += width;
				draw = !draw;
			}
			xpos += NARROW; // inter character space
		}

		Rectangle2D metrics = g.getFontMetrics(getFont()).getStringBounds(label, g);
		g.drawString(label, (int)Math.max(0, (FULLWIDTH-metrics.getWidth())/2), getHeight()-1);
	}	

	@Override
	public int print(Graphics g, PageFormat pf, int i) throws PrinterException 
	{
		if (i > 0) return NO_SUCH_PAGE;

		Graphics2D g2 = (Graphics2D)g;
		g2.translate(pf.getImageableX(), pf.getImageableY());
		double scale = Math.min(pf.getImageableWidth() / getWidth(), pf.getImageableHeight() / getHeight());
		g2.scale(scale, scale);

		paint(g);
		return PAGE_EXISTS;
	}
}