/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.challenge;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import org.wwscc.storage.Challenge;
import org.wwscc.storage.Database;
import org.wwscc.storage.Event;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;
import org.wwscc.util.Prefs;


class SelectionBar extends JPanel implements ActionListener, MessageListener
{
	private static Logger log = Logger.getLogger(SelectionBar.class.getCanonicalName());

	JLabel seriesLabel, connectLabel;
	JComboBox<Event> eventSelect;
	JComboBox<Challenge> challengeSelect;

	public SelectionBar()
	{
		super();

		Messenger.register(MT.TIMER_SERVICE_CONNECTION, this);
		Messenger.register(MT.DATABASE_CHANGED, this);
		Messenger.register(MT.NEW_CHALLENGE, this);
		Messenger.register(MT.CHALLENGE_DELETED, this);

		setBorder(new BevelBorder(0));

		Font f = new Font(Font.DIALOG, Font.BOLD, 14);

		seriesLabel = new JLabel("");
		seriesLabel.setFont(f.deriveFont(Font.PLAIN));
		challengeSelect  = createCombo("challengeChange");
		eventSelect = createCombo("eventChange");
		
		connectLabel = new JLabel("Not Connected");
		connectLabel.setForeground(Color.RED);
		connectLabel.setFont(f);

		add(createLabel("Series:", f));
		add(seriesLabel);
		add(Box.createHorizontalStrut(40));

		add(createLabel("Event:", f));
		add(eventSelect);
		add(Box.createHorizontalStrut(40));

		add(createLabel("Challenge:", f));
		add(challengeSelect);
		add(Box.createHorizontalStrut(10));
		
		add(connectLabel);
	}


	private JLabel createLabel(String txt, Font f)
	{
		JLabel l = new JLabel(txt);
		l.setFont(f);
		return l;
	}

	private <E> JComboBox<E> createCombo(String name)
	{
		JComboBox<E> combo = new JComboBox<E>();
		combo.setActionCommand(name);
		combo.addActionListener(this);
		return combo;
	}

	@Override
	public void event(MT type, Object o)
	{
		switch (type)
		{
			case TIMER_SERVICE_CONNECTION:
				Object a[] = (Object[])o;
				if ((Boolean)a[1])
					connectLabel.setVisible(false);
				else
					connectLabel.setVisible(true);
				break;
				
			case DATABASE_CHANGED:
				seriesLabel.setText(Database.d.getCurrentSeries());
				eventSelect.setModel(new DefaultComboBoxModel<Event>(Database.d.getEvents().toArray(new Event[0])));

				int select = Prefs.getEventId(0);
				if (select < eventSelect.getItemCount())
					eventSelect.setSelectedIndex(select);
				else
					eventSelect.setSelectedIndex(0);
				break;
				
			case NEW_CHALLENGE:
			case CHALLENGE_DELETED:
				challengeSelect.setModel(new DefaultComboBoxModel<Challenge>(Database.d.getChallengesForEvent().toArray(new Challenge[0])));
				challengeSelect.setSelectedIndex(challengeSelect.getItemCount() - 1);
				break;				
		}
	}

	/**
	 *
	 */
	@Override
	public void actionPerformed(ActionEvent e)
	{
		String cmd = e.getActionCommand();

		if (cmd.endsWith("Change"))
		{
			JComboBox<?> cb = (JComboBox<?>)e.getSource();
			Object o = cb.getSelectedItem();

			if (cmd.startsWith("event"))
			{
				Database.d.setCurrentEvent((Event)o);
				Messenger.sendEvent(MT.EVENT_CHANGED, null);
				Prefs.setEventId(eventSelect.getSelectedIndex());
				challengeSelect.setModel(new DefaultComboBoxModel<Challenge>(Database.d.getChallengesForEvent().toArray(new Challenge[0])));
				
				int select = Prefs.getChallengeId(0);
				if (select < challengeSelect.getItemCount())
					challengeSelect.setSelectedIndex(select);
				else if (challengeSelect.getItemCount() > 0)
					challengeSelect.setSelectedIndex(0);
				else
					Messenger.sendEvent(MT.CHALLENGE_CHANGED, null);
			}
			else if (cmd.startsWith("challenge"))
			{
				Messenger.sendEvent(MT.CHALLENGE_CHANGED, (Challenge)o);
				Prefs.setChallengeId(challengeSelect.getSelectedIndex());
			}
		}
	}
}

