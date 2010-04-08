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

public class TimingButtons extends JPanel implements ActionListener
{
	protected boolean left;

    public TimingButtons(boolean l)
	{
		super();
		left = l;

		//setBorder(new CompoundBorder(new LineBorder(Color.BLACK), new EmptyBorder(8,8,8,8)));
		setBorder(new EmptyBorder(8,8,8,8));
		setLayout(new GridLayout(2,2,10,15));

		JButton dstart = new JButton("Delete Start");
		JButton dfinish = new JButton("Delete Finish");
		JButton start = new JButton("Start");
		JButton finish = new JButton("Finish");

		Font f = new Font("Serif", Font.BOLD, 16);

		dstart.setFont(f);
		dfinish.setFont(f);
		dstart.setFont(f);
		dfinish.setFont(f);

		add(dstart);
		add(dfinish);
		add(start);
		add(finish);

		dstart.addActionListener(this);
		dfinish.addActionListener(this);
		start.addActionListener(this);
		finish.addActionListener(this);
	}


	@Override
	public void actionPerformed(ActionEvent e)
	{
		String com = e.getActionCommand();

		if (com.equals("Delete Finish")) { Messenger.sendEvent(left?MT.INPUT_DELETE_FINISH_LEFT:MT.INPUT_DELETE_FINISH_RIGHT, null); }
		if (com.equals("Delete Start")) { Messenger.sendEvent(left?MT.INPUT_DELETE_START_LEFT:MT.INPUT_DELETE_START_RIGHT, null); }
		if (com.equals("Start")) { Messenger.sendEvent(left?MT.INPUT_START_LEFT:MT.INPUT_START_RIGHT, null); }
		if (com.equals("Finish")) { Messenger.sendEvent(left?MT.INPUT_FINISH_LEFT:MT.INPUT_FINISH_RIGHT, null); }
	}

}

