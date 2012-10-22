/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.challenge.viewer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import org.wwscc.challenge.ActivationRequest;
import org.wwscc.challenge.Id;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;

class FocusButton extends JButton 
{
	static ImageIcon timerIcon = null;
	static Colors next2 = new Colors(Color.BLACK, new Color(100, 100, 200), new Color(100, 0, 255), new Color(200, 200, 200));
	static Colors next = new Colors(Color.BLACK, new Color(0, 200, 0), new Color(0, 255, 0), new Color(200, 200, 200));
	static Colors none = new Colors(Color.BLACK, new Color(255, 255, 255), new Color(200, 200, 200), new Color(240, 240, 240));

	/** 
	 * structure class for holding elements together
	 */
	static class Colors 
	{
		Color pressed;
		Color hover;
		Color background;
		Color foreground;

		public Colors(Color f, Color b, Color h, Color p) {
			foreground = f;
			background = b;
			hover = h;
			pressed = p;
		}
	}
	
	/**
	 * The responder for button clicks
	 */
	class FocusResponse implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e) {
			Boolean response = (Boolean)JOptionPane.showInputDialog(null, "Make first target or deactivate?", 
					"ReFocus", JOptionPane.QUESTION_MESSAGE, null, new Object[] {true, false}, currentState >= 0);
			if (response != null)
			{
				Messenger.sendEvent(MT.ACTIVE_CHANGE_REQUEST, new ActivationRequest(runid, response));
			}
		}
	}
	
	int currentState;
	Colors colors;
	JLabel stamp;
	Id.Run runid;

	public FocusButton(Id.Run rid) 
	{
		super("");
		try 
		{ // lazy loading of timerIcon if we don't have one
			if (timerIcon == null)
				timerIcon = new ImageIcon(FocusButton.class.getResource("/org/wwscc/images/smalltimer.png"));
			setIcon(timerIcon);
		} catch (Exception e) {} // try our best but failure is just fine
		
		
		runid = rid;
		stamp = new JLabel();
		stamp.setHorizontalAlignment(JLabel.CENTER);
		setText(runid.isLeft() ? "L" : "R");
		setStage(-1);
		addActionListener(new FocusResponse());
	}

	public final void setStage(int state) 
	{
		currentState = state;
		switch (currentState)
		{
			case 0: colors = next; break;
			case 1: colors = next2; break;
			default: colors = none; break;
		}
	}

	@Override
	public void paint(Graphics g1) 
	{
		Graphics2D g = (Graphics2D) g1;
		if (model.isArmed() && model.isPressed())
			g.setColor(colors.pressed);
		else if (model.isRollover())
			g.setColor(colors.hover);
		else
			g.setColor(colors.background);
		
		g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 5, 5);
		g.setColor(Color.BLACK);
		g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 5, 5);
		
		// cheap way to draw icon/text with nice spacing
		stamp.setText(currentState + " " + getText());
		stamp.setForeground(colors.foreground);
		stamp.setFont(getFont());
		stamp.setIcon(getIcon());
		stamp.setBounds(getBounds());
		stamp.paint(g1);
	}
}
