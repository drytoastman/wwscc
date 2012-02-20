/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2010 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dataentry;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;
import org.wwscc.util.IconButton;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;
import org.wwscc.util.SearchTrigger;


public class FindEntry extends JPanel implements ActionListener, MessageListener
{
	JTextField entry;
	JButton clear;
	IconButton close;

	public FindEntry()
	{
		super(new MigLayout("fill, ins 1"));
		entry = new JTextField();
		clear = new JButton("clear");
		close = new IconButton();

		entry.getDocument().addDocumentListener(new SearchTrigger() {
			@Override
			public void search(String txt) {
				Messenger.sendEvent(MT.FIND_ENTRANT, txt);
			}
		} );
		clear.addActionListener(this);
		close.addActionListener(this);
		
		add(entry, "growx");
		add(clear, "hmax 18");
		add(close, "ay top");
		setVisible(false);

		registerKeyboardAction(
			this,
			"esc",
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			JComponent.WHEN_IN_FOCUSED_WINDOW
		);
		Messenger.register(MT.OPEN_FIND, this);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == clear)
		{
			entry.setText("");
		}
		else if (e.getSource() == close || e.getActionCommand() == "esc")
		{
			setVisible(false);
		}
	}

	@Override
	public void event(MT type, Object data)
	{
		if (type == MT.OPEN_FIND)
		{
			setVisible(true);
			entry.requestFocus();
		}
	}
}
