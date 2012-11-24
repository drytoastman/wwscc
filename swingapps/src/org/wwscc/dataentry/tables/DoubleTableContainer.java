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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.JTableHeader;
import org.wwscc.dataentry.Sounds;
import org.wwscc.storage.Car;
import org.wwscc.storage.Database;
import org.wwscc.storage.Entrant;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;

/**
 * Wrapper that holds a separate driver and runs table to provide separate horizontal
 * scrolling as well as processing of events that can apply to both.
 */
public class DoubleTableContainer extends JScrollPane implements MessageListener
{
	private static final Logger log = Logger.getLogger(DoubleTableContainer.class.getCanonicalName());
	EntryModel dataModel;
	DriverTable driverTable;
	RunsTable runsTable;
	
	public DoubleTableContainer()
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
				JViewport _viewport = (JViewport) e.getSource();
				getVerticalScrollBar().setValue(_viewport.getViewPosition().y);
			}
		});
				
		Messenger.register(MT.CAR_ADD, this);
		Messenger.register(MT.CAR_CHANGE, this);
		Messenger.register(MT.FIND_ENTRANT, this);
		Messenger.register(MT.COURSE_CHANGED, this);
		Messenger.register(MT.BARCODE_SCANNED, this);
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
				int savecol = runsTable.getSelectedColumn();
				Entrant selected = (Entrant)dataModel.getValueAt(runsTable.getSelectedRow(), 0);
				dataModel.addCar((Integer)o);
				driverTable.scrollTable(dataModel.getRowCount(), 0);
				if ((savecol >= 0) && (selected != null))
				{ // update selection
					int newrow = dataModel.getRowForEntrant(selected);
					runsTable.setRowSelectionInterval(newrow, newrow);
				}
				driverTable.repaint();
				runsTable.repaint();
				break;
				
			case BARCODE_SCANNED:
				int driverid = (Integer)o;
				List<Car> available = Database.d.getRegisteredCars(driverid);
				for (Car c : available) {  // first look for something in current runorder
					if (Database.d.isInOrder(c.getId())) {
						event(MT.CAR_ADD, c.getId());
						return;
					}
					if (Database.d.hasRuns(c.getId()))
						available.remove(c); // remove not in order and has runs at same time
				}
				
				if (available.size() == 1) { // pick only one available
					event(MT.CAR_ADD, available.get(0).getId());
					return;
				}

				for (Car c : available) {  // skip TOPM, pick whatever else
					if (c.getClassCode().equals("TOPM")) continue;
					event(MT.CAR_ADD, c.getId());
					return;
				}

				log.log(Level.WARNING, "Unable to locate a registed car without runs for driver {0}", Integer.toString(driverid));
				break;
				
			case CAR_CHANGE:
				int row = driverTable.getSelectedRow();
				if ((row >= 0) && (row < driverTable.getRowCount()))
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
