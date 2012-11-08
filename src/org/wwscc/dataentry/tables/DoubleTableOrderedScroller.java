/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dataentry.tables;

import java.awt.Dimension;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author bwilson
 */
public class DoubleTableOrderedScroller extends JScrollPane 
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
	}
}
