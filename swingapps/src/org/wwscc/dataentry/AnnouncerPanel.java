/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dataentry;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import net.miginfocom.swing.MigLayout;

import org.wwscc.storage.Entrant;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;

/**
 * Encompasses all the components in the announcer tab and handles the run change
 * events, keeping track of last to finish as well as next to finish result panels.
 */
public class AnnouncerPanel extends JPanel implements MessageListener, ActionListener
{
	//private static final Logger log = Logger.getLogger(AnnouncerPanel.class.getCanonicalName());

	JToggleButton lastToFinish;
	JToggleButton nextToFinish;
	
	Entrant next;
	Entrant last;
		
	public AnnouncerPanel()
	{
		super(new MigLayout("ins 10 2", "[grow 50, fill][grow 50, fill]", "[][grow,fill]"));
		
		next = null;
		last = null;
		
		createButtons();
		
		add(lastToFinish, "split 2");
		add(nextToFinish, "wrap");
		/*  This will become a webview 
		JScrollPane scroller = new JScrollPane(classTable);
		scroller.setColumnHeader(null);
		scroller.setColumnHeaderView(null);
		add(scroller, "grow"); */		
		setOpaque(true); 
		setBackground(Color.WHITE);
	
		Messenger.register(MT.RUN_CHANGED, this);
		Messenger.register(MT.NEXT_TO_FINISH, this);
	}
	

	private void createButtons()
	{
		lastToFinish = new JToggleButton("Last To Finish");
		nextToFinish = new JToggleButton("Next To Finish");
		
		ButtonGroup b = new ButtonGroup();
		b.add(lastToFinish);
		b.add(nextToFinish);
		lastToFinish.addActionListener(this);
		nextToFinish.addActionListener(this);
		lastToFinish.setSelected(true);
	}
	

	@Override
	public void event(MT type, Object o)
	{
		switch (type)
		{
			case RUN_CHANGED:
				last = (Entrant)o;
				updateDisplayData(last, true);
				lastToFinish.setSelected(true);
				break;
				
			case NEXT_TO_FINISH:
				next = (Entrant)o;
				break;
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) 
	{
		String cmd = e.getActionCommand();
		if (cmd.equals("Last To Finish"))
		{
			updateDisplayData(last, true);
		}
		else if (cmd.equals("Next To Finish"))
		{
			updateDisplayData(next, false);
		}
	}


	public void updateDisplayData(Entrant entrant, boolean showLast)
	{
		if (entrant != null) {
			// FINISH ME May request to web server
		}
	}
}
