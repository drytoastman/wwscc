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
import java.util.List;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import org.wwscc.components.CurrentSeriesLabel;
import org.wwscc.storage.Challenge;
import org.wwscc.storage.Database;
import org.wwscc.storage.Event;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;
import org.wwscc.util.Prefs;


class SelectionBar extends JPanel implements ActionListener, MessageListener
{
	//private static final Logger log = Logger.getLogger(SelectionBar.class.getCanonicalName());

	JLabel connectLabel;
	CurrentSeriesLabel seriesLabel;
	JComboBox<Event> eventSelect;
	JComboBox<Challenge> challengeSelect;

	public SelectionBar()
	{
		super();

		Messenger.register(MT.TIMER_SERVICE_CONNECTION, this);
		Messenger.register(MT.SERIES_CHANGED, this);
		Messenger.register(MT.NEW_CHALLENGE, this);
		Messenger.register(MT.CHALLENGE_DELETED, this);

		setBorder(new BevelBorder(0));

		Font f = new Font(Font.DIALOG, Font.BOLD, 14);

		seriesLabel = new CurrentSeriesLabel();
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
		add(Box.createHorizontalStrut(15));
		
		add(createLabel("Timer:", f));
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
				{
					connectLabel.setText("Connected");
					connectLabel.setForeground(Color.BLACK);
				}
				else
				{
					connectLabel.setText("Not Connected");
					connectLabel.setForeground(Color.RED);
				}
				break;
				
			case SERIES_CHANGED:
				seriesLabel.setText(ChallengeGUI.state.getCurrentSeries());
				eventSelect.setModel(new DefaultComboBoxModel<Event>(Database.d.getEvents().toArray(new Event[0])));

				int select = Prefs.getEventId(0);
				if (select < eventSelect.getItemCount())
					eventSelect.setSelectedIndex(select);
				else
					eventSelect.setSelectedIndex(0);
				break;
				
			case NEW_CHALLENGE:
			case CHALLENGE_DELETED:
				List<Challenge> challenges = Database.d.getChallengesForEvent(ChallengeGUI.state.getCurrentEventId());
				challengeSelect.setModel(new DefaultComboBoxModel<Challenge>(challenges.toArray(new Challenge[0])));
				for (Challenge c : challenges) {
					if (c.getChallengeId() == ChallengeGUI.state.getCurrentChallengeId()) {
						challengeSelect.setSelectedItem(c);
						return;
					}
				}
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
				ChallengeGUI.state.setCurrentEvent((Event)o);
				Messenger.sendEvent(MT.EVENT_CHANGED, null);
				Prefs.setEventId(eventSelect.getSelectedIndex());
				challengeSelect.setModel(new DefaultComboBoxModel<Challenge>(Database.d.getChallengesForEvent(ChallengeGUI.state.getCurrentEventId()).toArray(new Challenge[0])));
				
				int select = Prefs.getChallengeId(0);
				if (select < challengeSelect.getItemCount())
					challengeSelect.setSelectedIndex(select);
				else if (challengeSelect.getItemCount() > 0)
					challengeSelect.setSelectedIndex(0);
				else
				{
					ChallengeGUI.state.setCurrentChallengeId(-1);
					Messenger.sendEvent(MT.CHALLENGE_CHANGED, null);
				}
			}
			else if (cmd.startsWith("challenge"))
			{
				ChallengeGUI.state.setCurrentChallengeId(((Challenge)o).getChallengeId());
				Messenger.sendEvent(MT.CHALLENGE_CHANGED, (Challenge)o);
				Prefs.setChallengeId(challengeSelect.getSelectedIndex());
			}
		}
	}
}

