/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dataentry.tables;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.JTableHeader;
import org.wwscc.dataentry.Sounds;
import org.wwscc.storage.Database;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;

/**
 * Wrapper that holds a separate driver and runs table to provide separate horizontal
 * scrolling as well as processing of events that can apply to both.
 */
public class DoubleTableOrderedScroller extends JScrollPane implements MessageListener
{
	EntryModel dataModel;
	DriverTable driverTable;
	RunsTable runsTable;
	
	public DoubleTableOrderedScroller()
	{
		dataModel = new EntryModel();
		driverTable = new DriverTable(dataModel);
		runsTable = new RunsTable(dataModel);
		
		setViewportView(runsTable);
		setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
		setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_ALWAYS);
		
		driverTable.setPreferredScrollableViewportSize(new Dimension(240, Integer.MAX_VALUE));
		setRowHeaderView( driverTable );
		setCorner(UPPER_LEFT_CORNER, driverTable.getTableHeader());
		getRowHeader().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JViewport viewport = (JViewport) e.getSource();
				getVerticalScrollBar().setValue(viewport.getViewPosition().y);
			}
		});
				
		Messenger.register(MT.CAR_ADD, this);
		Messenger.register(MT.CAR_CHANGE, this);
		Messenger.register(MT.FIND_ENTRANT, this);
		Messenger.register(MT.COURSE_CHANGED, this);
	}
	
	public RunsTable getRunsTable() { return runsTable; }
	public DriverTable getDriverTable() { return driverTable; }

	@Override
	public void event(MT type, Object o)
	{
		switch (type)
		{
			case CAR_ADD:
				Sounds.playBlocked();
				dataModel.addCar((Integer)o);
				driverTable.scrollTable(dataModel.getRowCount(), 0);
				driverTable.repaint();
				runsTable.repaint();
				break;

			case CAR_CHANGE:
				int row = runsTable.getSelectedRow();
				if ((row >= 0) && (row < runsTable.getRowCount()))
					dataModel.replaceCar((Integer)o, row);
				break;

			case FIND_ENTRANT:
				driverTable.activeSearch = (String)o;
				repaint();
				break;
				
			case COURSE_CHANGED:
				JTableHeader dh = driverTable.getTableHeader();
				JTableHeader rh = runsTable.getTableHeader();
				if (Database.d.getCurrentCourse() > 1)
				{
					dh.setForeground(Color.BLUE);
					dh.setBorder(new LineBorder(Color.BLUE));
					rh.setBorder(new LineBorder(Color.BLUE));
				}
				else
				{
					dh.setForeground(Color.BLACK);
					dh.setBorder(new LineBorder(Color.GRAY, 1));
					rh.setBorder(new LineBorder(Color.GRAY, 1));
				}
				break;
		}
	}
}
