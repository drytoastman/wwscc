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
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.JTableHeader;
import org.wwscc.dataentry.Sounds;
import org.wwscc.storage.Car;
import org.wwscc.storage.ClassData;
import org.wwscc.storage.Database;
import org.wwscc.storage.Driver;
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

	class BarcodeException extends IOException {
		public BarcodeException(String message) {
			super(message);
		}
	}
	
	public Driver findDriverByBarcode(String barcode) throws IOException
	{
		if (barcode.startsWith("D"))
		{
			Driver byid = Database.d.getDriver(Integer.parseInt(barcode.substring(1)));
			if (byid == null)
				throw new BarcodeException("Unable to located a driver with ID=" + barcode.substring(1));
			return byid;
		}
		
		if (barcode.length() < 4)
			throw new BarcodeException(barcode + " is too short to be a valid barcode value, ignoring");
		
		List<Driver> found = Database.d.findDriverByMembership(barcode);
		if (found.size() == 0)
		{
			if (JOptionPane.showConfirmDialog(this, "Unable to locate a driver using barcode " + barcode + 
											".  Do you want to create a placeholder with this membership value?",
											"Missing Driver",
											JOptionPane.YES_NO_OPTION) == JOptionPane.YES_NO_OPTION)
			{
				Driver d = new Driver("Placeholder", barcode);
				d.setMembership(barcode);
				Database.d.newDriver(d);
				Car c = new Car();
				c.setDriverId(d.getId());
				c.setModel("Placeholder " + barcode);
				c.setClassCode(ClassData.getMissing().getCode());
				c.setNumber(0);
				Database.d.newCar(c);
				Database.d.registerCar(c.getId(), false, false);
				return d;
			}

			throw new BarcodeException("Unable to locate a driver using barcode " + barcode);
		}
		
		if (found.size() > 1)
			log.log(Level.WARNING, "{0} drivers exist with the membership value {1}, using the first", new Object[] {found.size(), barcode});
		
		return found.get(0);
	}
	
	public void processBarcode(String barcode) throws IOException
	{
		if (barcode.startsWith("C"))
		{
			int carid = Integer.parseInt(barcode.substring(1));
			if (Database.d.loadEntrant(carid, false) == null)
				throw new BarcodeException("Unable to find a car with ID=" + carid);
			event(MT.CAR_ADD, carid);
			return;
		}
		
		Driver d = findDriverByBarcode(barcode);
		
		List<Car> available = Database.d.getRegisteredCars(d.getId());
		Iterator<Car> iter = available.iterator();
		while (iter.hasNext()) {
			Car c = iter.next();
			if (Database.d.isInCurrentOrder(c.getId())) {
				event(MT.CAR_ADD, c.getId()); // if there is something in this run order, just go with it and return
				return;
			}
			if (Database.d.isInOrder(c.getId()))
				iter.remove(); // otherwise, remove those active in another run order (same course/event)
		}
		
		if (available.size() == 1) { // pick only one available
			event(MT.CAR_ADD, available.get(0).getId());
			return;
		}

		for (Car c : available) {  // multiple available, skip TOPM, pick whatever else
			if (c.getClassCode().equals("TOPM")) continue;
			event(MT.CAR_ADD, c.getId());
			return;
		}

		Messenger.sendEvent(MT.SHOW_ADD_PANE, d);
		throw new BarcodeException("Unable to locate a registed car for " + d.getFullName() + " that isn't already used in this event on this course.  See left panel.");
	}

	
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
				try {
					processBarcode((String)o);
				} catch (IOException be) {
					log.log(Level.SEVERE, be.getMessage());
				}
				
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
