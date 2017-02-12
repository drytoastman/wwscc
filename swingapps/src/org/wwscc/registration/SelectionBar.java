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
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import net.miginfocom.swing.MigLayout;

import org.wwscc.components.CurrentDatabaseLabel;
import org.wwscc.storage.Database;
import org.wwscc.storage.Event;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;
import org.wwscc.util.Prefs;


class SelectionBar extends JPanel implements ActionListener, MessageListener
{
	JComboBox<Event> eventSelect;
	JCheckBox lock;
	JLabel count;
	
	public SelectionBar()
	{
		super();

		Messenger.register(MT.DATABASE_CHANGED, this);
		
		setLayout(new MigLayout("ins 2, center, gap 4"));
		setBorder(new BevelBorder(0));

		Font f = new Font(Font.DIALOG, Font.BOLD, 14);
		eventSelect = new JComboBox<Event>();
		eventSelect.setActionCommand("eventChange");
		eventSelect.addActionListener(this);
		
		lock = new JCheckBox("Lock");
		lock.setActionCommand("lockEvent");
		lock.addActionListener(this);

		count = new JLabel("");
		count.setFont(new Font(Font.DIALOG, Font.BOLD, 14));

		JLabel dl = new JLabel("Database:");
		dl.setFont(f);
		JLabel el = new JLabel("Event:");
		el.setFont(f);
		JLabel cl = new JLabel("Changes:");
		cl.setFont(f);

		add(dl, "");
		add(new CurrentDatabaseLabel(), "");
		add(el, "gap left 25");
		add(eventSelect, "");
		add(lock, "");
		add(cl, "gap left 25");
		add(count, "");
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
			JComboBox<?> cb = (JComboBox<?>)e.getSource();
			Registration.state.setCurrentEvent((Event)cb.getSelectedItem());
			Registration.state.setCurrentCourse(1);
			Messenger.sendEvent(MT.EVENT_CHANGED, null);
			Prefs.setEventId(eventSelect.getSelectedIndex());
		}
		else if (e.getActionCommand().equals("lockEvent"))
		{
			JCheckBox cb = (JCheckBox)e.getSource();
			eventSelect.setEnabled(!cb.isSelected());
		}
	}
}

