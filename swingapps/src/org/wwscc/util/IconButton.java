/*
 * Copyright (C) 2007 SPARTA, Inc.
 * This software is licensed under the GPLv3 license, included in
 * ./GPLv3-LICENSE.txt in the source distribution
 */

package org.wwscc.util;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

/**
 * @author bwilson
 */
public class IconButton extends JButton
{
	static ImageIcon buttonIcon;
	static ImageIcon rolloverIcon;
	static ImageIcon pressedIcon;

	public IconButton(Icon regular, Icon rollover, Icon pressed)
	{
		super();
		setBorder(new EmptyBorder(0,0,0,0));
		setContentAreaFilled(false);
		setIcon(regular);
		setRolloverIcon(rollover);
		setPressedIcon(pressed);
	}

	public IconButton()
	{
		super();
		if (buttonIcon == null)
		{
			buttonIcon = new ImageIcon(getClass().getResource("/org/wwscc/images/close.png"));
			rolloverIcon = new ImageIcon(getClass().getResource("/org/wwscc/images/close-hover.png"));
			pressedIcon = new ImageIcon(getClass().getResource("/org/wwscc/images/close-press.png"));
		}
		setBorder(new EmptyBorder(0,0,0,0));
		setContentAreaFilled(false);
		setIcon(buttonIcon);
		setRolloverIcon(rolloverIcon);
		setPressedIcon(pressedIcon);
	}
}
