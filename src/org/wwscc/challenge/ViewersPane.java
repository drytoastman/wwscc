/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.challenge;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.Scrollable;
import net.miginfocom.swing.MigLayout;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;

/**
 *
 */
public class ViewersPane extends JComponent implements MessageListener, Scrollable
{
	private static Logger log = Logger.getLogger(ViewersPane.class.getCanonicalName());
	
	GridBagConstraints constraints;
	ChallengeModel model;
	Map<Id.Round, RoundViewer> panes;
	
	public ViewersPane(ChallengeModel m)
	{
		Messenger.register(MT.OPEN_ROUND, this);
		Messenger.register(MT.CLOSE_ROUND, this);
		model = m;
		
		panes = new HashMap<Id.Round, RoundViewer>();
		setLayout(new MigLayout("ins 0,flowy"));
	}

	public void addRound(Id.Round rid)
	{
		try
		{
			RoundViewer v = new RoundViewer(model, rid);
			add(v, "growx");
			panes.put(rid, v);
			revalidate();
		}
		catch (Exception e)
		{
			log.log(Level.WARNING, "Failed to open round: " + e, e);
		}
	}
	
	public void removeRound(Id.Round rid)
	{
		RoundViewer v = panes.get(rid);
		if (v != null)
		{
			remove(v);
			v.close();
			revalidate();
			repaint();
		}
	}

	@Override
	public void event(MT type, Object data)
	{
		switch (type)
		{
			case OPEN_ROUND:
				addRound((Id.Round)data);
				break;
				
			case CLOSE_ROUND:
				removeRound((Id.Round)data);
				break;
		}
	}

	@Override
	public Dimension getPreferredScrollableViewportSize()
	{
		return getPreferredSize();
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		return 20;
	}

	@Override
	public boolean getScrollableTracksViewportHeight()
	{
		return false;
	}

	@Override
	public boolean getScrollableTracksViewportWidth()
	{
		return true;
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		return 20;
	}

}
