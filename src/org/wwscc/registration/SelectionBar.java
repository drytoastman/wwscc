/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2010 Brett Wilson.
 * All rights reserved.
 */


package org.wwscc.registration;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import net.miginfocom.swing.MigLayout;
import org.wwscc.storage.Database;
import org.wwscc.storage.Event;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;
import org.wwscc.util.Prefs;


class SelectionBar extends JPanel implements ActionListener, MessageListener
{
	private static Logger log = Logger.getLogger(SelectionBar.class.getCanonicalName());

	JComboBox<Event> eventSelect;

	public SelectionBar()
	{
		super();

		Messenger.register(MT.DATABASE_CHANGED, this);
		setLayout(new MigLayout("ins 2, fill"));
		setBorder(new BevelBorder(0));

		Font f = new Font(Font.DIALOG, Font.BOLD, 14);
		eventSelect = new JComboBox<Event>();
		eventSelect.setActionCommand("eventChange");
		eventSelect.addActionListener(this);

		JLabel l = new JLabel("Event:");
		l.setFont(f);

		add(l, "al center, split");
		add(eventSelect);
		add(Box.createHorizontalStrut(40));
	}

	
	@Override
	public void event(MT type, Object o)
	{
		switch (type)
		{
			case DATABASE_CHANGED:
				eventSelect.setModel(new DefaultComboBoxModel<Event>(Database.d.getEvents().toArray(new Event[0])));
				int select = Prefs.getEventId(0);
				if (select < eventSelect.getItemCount())
					eventSelect.setSelectedIndex(select);
				else if (eventSelect.getItemCount() > 0)
					eventSelect.setSelectedIndex(0);
				break;
		}
	}

	/**
	 *
	 */
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand().equals("eventChange"))
		{
			JComboBox cb = (JComboBox)e.getSource();
			Database.d.setCurrentEvent((Event)cb.getSelectedItem());
			Database.d.setCurrentCourse(1);
			Messenger.sendEvent(MT.EVENT_CHANGED, null);
			Prefs.setEventId(eventSelect.getSelectedIndex());
		}
	}
}

