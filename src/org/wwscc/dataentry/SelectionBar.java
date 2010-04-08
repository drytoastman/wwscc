/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */


package org.wwscc.dataentry;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import net.miginfocom.swing.MigLayout;
import org.wwscc.storage.Database;
import org.wwscc.storage.Event;
import org.wwscc.util.BrowserControl;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;
import org.wwscc.util.Prefs;


class SelectionBar extends JPanel implements ActionListener, MessageListener
{
	private static Logger log = Logger.getLogger(SelectionBar.class.getCanonicalName());

	JButton resultsButton;

	JComboBox eventSelect;
	JComboBox courseSelect;
	JComboBox groupSelect;
	JCheckBox timeGrabsFocus;

	public SelectionBar()
	{
		super(new MigLayout("fill, ins 3", "[grow 0][grow 0][grow 0][grow 0][grow 0][grow 0][grow][grow 0][grow 0]"));

		Messenger.register(MT.DATABASE_CHANGED, this);
		setBorder(new BevelBorder(0));

		Font f = new Font(Font.DIALOG, Font.BOLD, 14);

		resultsButton = new JButton("Current Group Results");
		resultsButton.addActionListener(this);
		resultsButton.setActionCommand("resultsPrint");

		courseSelect = createCombo("courseChange");
		groupSelect  = createCombo("groupChange");
		eventSelect = createCombo("eventChange");
		timeGrabsFocus = new JCheckBox("New Time Grabs Focus");
		timeGrabsFocus.addActionListener(this);

		groupSelect.setModel(new DefaultComboBoxModel(new Integer[] { 1, 2, 3, 4, 5, 6 }));

		add(createLabel("Event:", f), "gapleft 10");
		add(eventSelect, "gapright 20");

		add(createLabel("Course:", f));
		add(courseSelect, "gapright 10");

		add(createLabel("RunGroup:", f));
		add(groupSelect, "");

		add(new JLabel(""), "");

		add(resultsButton, "gapright 20");
		add(timeGrabsFocus, "al right");

		timeGrabsFocus.setSelected(true);
	}


	private JLabel createLabel(String txt, Font f)
	{
		JLabel l = new JLabel(txt);
		l.setFont(f);
		return l;
	}

	private JComboBox createCombo(String name)
	{
		JComboBox combo = new JComboBox();
		combo.setActionCommand(name);
		combo.addActionListener(this);
		//((BasicComboBoxRenderer)combo.getRenderer()).setBorder(new EmptyBorder(1,8,1,8));
		return combo;
	}


	public void setCourseList(int count)
	{
		Integer list[] = new Integer[count];
		for (int ii = 0; ii < count; ii++)
			list[ii] = (ii+1);

		courseSelect.setModel(new DefaultComboBoxModel(list));
	}
	

	@Override
	public void event(MT type, Object o)
	{
		switch (type)
		{
			case DATABASE_CHANGED:
				eventSelect.setModel(new DefaultComboBoxModel(new Vector<Event>(Database.d.getEvents())));
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
	public void actionPerformed(ActionEvent e)
	{
		String cmd = e.getActionCommand();

		if (cmd.endsWith("Change"))
		{
			JComboBox cb = (JComboBox)e.getSource();
			Object o = cb.getSelectedItem();

			if (cmd.startsWith("event"))
			{
				Database.d.setCurrentEvent((Event)o);
				Messenger.sendEvent(MT.EVENT_CHANGED, null);
				Prefs.setEventId(eventSelect.getSelectedIndex());
				setCourseList(Database.d.getCurrentEvent().getCourses());
				courseSelect.setSelectedIndex(0);
			}
			else if (cmd.startsWith("course"))
			{
				Database.d.setCurrentCourse((Integer)o);
				Messenger.sendEvent(MT.COURSE_CHANGED, null);
				groupSelect.setSelectedIndex(groupSelect.getSelectedIndex());
			}
			else if (cmd.startsWith("group"))
			{
				Database.d.setCurrentRunGroup((Integer)o);
				Messenger.sendEvent(MT.RUNGROUP_CHANGED, null);
			}
		}
		else if (cmd.endsWith("Print"))
		{
			if (cmd.startsWith("results"))
				BrowserControl.openGroupResults(new int[] {Database.d.getCurrentRunGroup()});
		}
		else if (cmd.equals("New Time Grabs Focus"))
		{
			Messenger.sendEvent(MT.TIMER_TAKES_FOCUS, timeGrabsFocus.isSelected());
		}
	}
}

