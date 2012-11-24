/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dataentry;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.metal.MetalLookAndFeel;
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
	ResultsPane results;
	JToggleButton lastToFinish;
	JToggleButton nextToFinish;
	Entrant next;
	Entrant last;
	
	public AnnouncerPanel()
	{
		super(new MigLayout("ins 10 2", "[grow 50, fill][grow 50, fill]", "[][grow,fill]"));
		Color myblue = new Color(210, 210, 225);
		Font myfont = ((Font)UIManager.get("Label.font")).deriveFont(12.0f);
		
		results = new ResultsPane();
		next = null;
		last = null;
		
		lastToFinish = new JToggleButton("Last To Finish");
		nextToFinish = new JToggleButton("Next To Finish");
		MetalLookAndFeel m = new MetalLookAndFeel();
		lastToFinish.setUI((ButtonUI)m.getDefaults().getUI(lastToFinish));
		nextToFinish.setUI((ButtonUI)m.getDefaults().getUI(lastToFinish));
		lastToFinish.setBackground(myblue);
		nextToFinish.setBackground(myblue);
		lastToFinish.setFont(myfont);
		nextToFinish.setFont(myfont);
		
		ButtonGroup b = new ButtonGroup();
		b.add(lastToFinish);
		b.add(nextToFinish);
		lastToFinish.addActionListener(this);
		nextToFinish.addActionListener(this);
		lastToFinish.setSelected(true);
		
		add(lastToFinish, "");
		add(nextToFinish, "wrap");
		add(results, "spanx 2");
		
		Messenger.register(MT.RUN_CHANGED, this);
		Messenger.register(MT.NEXT_TO_FINISH, this);
	}
	

	@Override
	public void event(MT type, Object o)
	{
		switch (type)
		{
			case RUN_CHANGED:
				last = (Entrant)o;
				results.updateDisplayData(last, true);
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
			if (last != null)
				results.updateDisplayData(last, true);
		}
		else if (cmd.equals("Next To Finish"))
		{
			if (next != null)
				results.updateDisplayData(next, false);
		}
	}
	
}
