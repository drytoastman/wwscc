/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */


package org.wwscc.protimer;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

import org.wwscc.util.*;

public class OpenStatus extends JPanel implements MessageListener
{
	JLabel lsLabel;
	JLabel lfLabel;
	JLabel rsLabel;
	JLabel rfLabel;

	Color off;
	Color on;


    public OpenStatus()
	{
		super(new GridLayout(2, 2));

		MouseClear mc = new MouseClear();

		on = Color.RED;
		off = Color.LIGHT_GRAY;

		Messenger.register(MT.OPEN_SENSOR, this);
		Messenger.register(MT.INPUT_RESET_SOFT, this);
		Messenger.register(MT.INPUT_RESET_HARD, this);
		setBorder(new EmptyBorder(8,8,8,8));

		lsLabel = new JLabel("Left Start Open", JLabel.CENTER);
		rsLabel = new JLabel("Right Start Open", JLabel.CENTER);
		lfLabel = new JLabel("Left Finish Open", JLabel.CENTER);
		rfLabel = new JLabel("Right Finish Open", JLabel.CENTER);

		lsLabel.setFont(new Font("serif", Font.BOLD, 14));
		rsLabel.setFont(new Font("serif", Font.BOLD, 14));
		lfLabel.setFont(new Font("serif", Font.BOLD, 14));
		rfLabel.setFont(new Font("serif", Font.BOLD, 14));

		lsLabel.setForeground(off);
		rsLabel.setForeground(off);
		lfLabel.setForeground(off);
		rfLabel.setForeground(off);

		lsLabel.addMouseListener(mc);
		rsLabel.addMouseListener(mc);
		lfLabel.addMouseListener(mc);
		rfLabel.addMouseListener(mc);

		add(lsLabel);
		add(rsLabel);
		add(lfLabel);
		add(rfLabel);
	}

	@Override
	public void event(MT type, Object o)
	{
		switch (type)
		{
			case INPUT_RESET_SOFT:
			case INPUT_RESET_HARD:
				lsLabel.setForeground(off);
				lfLabel.setForeground(off);
				rsLabel.setForeground(off);
				rfLabel.setForeground(off);
				break;

			case OPEN_SENSOR:
				Object arr[] = (Object[])o;
				boolean left = (Boolean)arr[0];
				String sensor = (String)arr[1];
	
				if (left)
				{
					if (sensor.equals("START"))
						lsLabel.setForeground(on);
					else if (sensor.equals("FIN"))
						lfLabel.setForeground(on);
				}
				else
				{
					if (sensor.equals("START"))
						rsLabel.setForeground(on);
					else if (sensor.equals("FIN"))
						rfLabel.setForeground(on);
				}

				break;
		}
	}


	class MouseClear extends MouseAdapter
	{
		public void mouseClicked(MouseEvent me)
		{
			JLabel src = (JLabel)me.getSource();
			src.setForeground(off);
		}
	}

}

