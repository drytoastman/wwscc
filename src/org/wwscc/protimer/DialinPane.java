/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */


package org.wwscc.protimer;

import java.text.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

import org.wwscc.storage.LeftRightDialin;
import org.wwscc.util.*;

public class DialinPane extends JPanel implements ActionListener, MessageListener
{
	private JLabel leftDial;
	private JLabel rightDial;
	private JTextField leftField;
	private JTextField rightField;
	private JButton set;
	private NumberFormat df;

	public DialinPane()
	{
		super(new GridBagLayout());

		df = NumberFormat.getNumberInstance();
		df.setMinimumFractionDigits(3);
		df.setMaximumFractionDigits(3);

		GridBagConstraints c = new GridBagConstraints();
		Font f = new Font("serif", Font.BOLD, 20);

		Messenger.register(MT.DIALIN_LEFT, this);
		Messenger.register(MT.DIALIN_RIGHT, this);
		Messenger.register(MT.INPUT_RESET_SOFT, this);
		Messenger.register(MT.INPUT_RESET_HARD, this);
		Messenger.register(MT.SIXTY_LEFT, this);
		Messenger.register(MT.SIXTY_RIGHT, this);

		JLabel leftLbl = new JLabel("Left Dial-in");
		JLabel rightLbl = new JLabel("Right Dial-in");
		
		leftDial = new JLabel("      ");
		leftField = new JTextField(6);
		((AbstractDocument)leftField.getDocument()).setDocumentFilter(new EasyNumFilter(3, 3));

		rightDial = new JLabel("      ");
		rightField = new JTextField(6);
		((AbstractDocument)rightField.getDocument()).setDocumentFilter(new EasyNumFilter(3, 3));

		set = new JButton("Set");
		set.addActionListener(this);
	
		leftLbl.setFont(f);
		leftDial.setFont(f);

		rightLbl.setFont(f);
		rightDial.setFont(f);

		c.gridx = 0; add(leftLbl, c);
		c.insets = new Insets(0, 10, 0,  0); c.gridx = 1; add(leftDial, c);
		c.insets = new Insets(0, 20, 0,  0); c.gridx = 2; add(leftField, c);
		c.insets = new Insets(0,  3, 0,  3); c.gridx = 3; add(set, c);
		c.insets = new Insets(0,  0, 0, 20); c.gridx = 4; add(rightField, c);
		c.insets = new Insets(0,  0, 0, 10); c.gridx = 5; add(rightDial, c);
		c.gridx = 6; add(rightLbl, c);

	}

	public void doFocus(JFrame f)
	{
		f.setFocusTraversalPolicy(new DialinFocus());
	}

	class DialinFocus extends FocusTraversalPolicy
	{
		public Component getComponentAfter(Container focusCycleRoot, Component aComponent) 
		{
			if (aComponent.equals(leftField))
				return rightField;
			else if (aComponent.equals(rightField)) 
				return set;
			
			return leftField;
		}
		
		public Component getComponentBefore(Container focusCycleRoot, Component aComponent) 
		{
			if (aComponent.equals(leftField))
				return set;
			else if (aComponent.equals(set)) 
				return rightField;
			
			return leftField;
		}
		
		public Component getDefaultComponent(Container focusCycleRoot) { return leftField; }
		public Component getLastComponent(Container focusCycleRoot) { return set; }
		public Component getFirstComponent(Container focusCycleRoot) { return leftField; }
	}


	@Override
	public void event(MT type, Object o)
	{
		switch (type)
		{
			case DIALIN_LEFT:
				leftDial.setText(df.format((Double)o));
				break;
			case DIALIN_RIGHT:
				rightDial.setText(df.format((Double)o));
				break;

			case INPUT_RESET_SOFT:
			case INPUT_RESET_HARD:
			case SIXTY_LEFT:
			case SIXTY_RIGHT:
				leftDial.setText("      ");
				rightDial.setText("      ");
				break;
		}
	}
	

	@Override
	public void actionPerformed(ActionEvent e)
	{
		try
		{
			LeftRightDialin d = new LeftRightDialin(new Double(leftField.getText()), new Double(rightField.getText()));
			Messenger.sendEvent(MT.INPUT_SET_DIALIN, d);
			leftField.setText("");
			rightField.setText("");
			leftField.requestFocus();
		}
		catch (NumberFormatException nfe)
		{
		}
	}
}

