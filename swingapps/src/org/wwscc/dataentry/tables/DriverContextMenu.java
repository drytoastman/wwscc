/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */
package org.wwscc.dataentry.tables;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import org.wwscc.components.UnderlineBorder;
import org.wwscc.dataentry.DataEntry;
import org.wwscc.dialogs.TextRunsDialog;
import org.wwscc.storage.Car;
import org.wwscc.storage.Database;
import org.wwscc.storage.Entrant;
import org.wwscc.storage.Run;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;

/**
 * Create a simple context menu for the driver columns to allow pasting
 * of textual run data from other sources (like national pro solo)
 */
class DriverContextMenu extends MouseAdapter
{
	JTable target;
	JPopupMenu menu;
	
	public DriverContextMenu(JTable tbl)
	{
		target = tbl;
		menu = null;
	}
	
	@Override
	public void mousePressed(MouseEvent e)
	{
		if (e.getButton() == MouseEvent.BUTTON3)
		{  // first right click will show popup, no need to select first
			Point p = e.getPoint();
			target.changeSelection(target.rowAtPoint(p), target.columnAtPoint(p), false, false);
		}

		if (e.isPopupTrigger())
			doPopup(e);
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		if (e.isPopupTrigger())
			doPopup(e);
	}

	private void doPopup(MouseEvent e)
	{
		int row = target.rowAtPoint(e.getPoint());
		int col = target.columnAtPoint(e.getPoint());
		if ((row == -1) || (col == -1)) return;
		if (target.getSelectedRow() != row) return;
		if (target.getSelectedColumn() != col) return;

		Entrant selectedE = (Entrant)target.getValueAt(row, col);			
		List<Car> registered = Database.d.getRegisteredCars(selectedE.getDriverId(), null);
		List<Car> allcars = Database.d.getCarsForDriver(selectedE.getDriverId());

		menu = new JPopupMenu("");
		addTitle("Swap to Registered Car");
		for (Car c : registered) 
			addCarAction(c);

		addTitle("Swap to Unregistered Car");
		boolean removelast = true;
		for (Car c : allcars) {
			if (!registered.contains(c) && !Database.d.isInOrder(DataEntry.state.getCurrentEventId(), c.getCarId(), DataEntry.state.getCurrentCourse())) {
				addCarAction(c);
				removelast = false;
			}
		}
		if (removelast)
			menu.remove(menu.getComponentCount()-1);
		
		addTitle("Other");
		menu.add(new JMenuItem(new TextRunAction(selectedE))).setFont(itemFont);
		menu.add(new JMenuItem(new PaidAction(selectedE))).setFont(itemFont);
		
		menu.show(target, e.getX(), e.getY());
	}

	private Color superLightGray = new Color(200, 200, 200);
	private Font itemFont = new Font("sansserif", Font.PLAIN, 12);

	private void addTitle(String s)
	{
		JMenuItem lbl = new JMenuItem(s);
		lbl.setFont(new Font("sansserif", Font.BOLD, 13));
		lbl.setForeground(Color.DARK_GRAY);
		lbl.setBorder(new UnderlineBorder(2, 0, 0, 0));
		lbl.setBorderPainted(true);
		menu.add(lbl);
	}

	private void addCarAction(final Car c)
	{
		JMenuItem lbl = new JMenuItem(String.format("%s %s #%s %s %s", c.getClassCode(), c.getIndexStr(), c.getNumber(), c.getMake(), c.getModel()));
		lbl.setFont(itemFont);
		lbl.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				Messenger.sendEvent(MT.CAR_CHANGE, c.getCarId());
		}});
		
		if (Database.d.isInOrder(DataEntry.state.getCurrentEventId(), c.getCarId(), DataEntry.state.getCurrentCourse()))
		{
			lbl.setEnabled(false);
			lbl.setForeground(superLightGray);
		}
		
		menu.add(lbl);
	}
}


class PaidAction extends AbstractAction
{
	private static final Logger log = Logger.getLogger(PaidAction.class.getCanonicalName());
	
	Entrant entrant;
	public PaidAction(Entrant e)
	{
		super("Mark Driver Paid");
		entrant = e;
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		try {
			Database.d.registerCar(null, entrant.getCarId(), true, true);
			Messenger.sendEvent(MT.RUNGROUP_CHANGED, null);
		} catch (IOException ioe) {
			log.severe("Failed to mark driver paid: " + ioe.getMessage());
		}
	}
}


class TextRunAction extends AbstractAction
{
	Entrant entrant;
	public TextRunAction(Entrant e)
	{
		super("Add Text Runs");
		entrant = e;
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		TextRunsDialog trd = new TextRunsDialog();
		trd.doDialog("Textual Run Input", null);
		List<Run> runs = trd.getResult();
		if (runs == null) return;

		for (Run r : trd.getResult())
		{
			r.updateTo(DataEntry.state.getCurrentEventId(), entrant.getCarId(), r.course(), r.run());
			Database.d.setRun(r);
		}
		Messenger.sendEvent(MT.RUNGROUP_CHANGED, 1);
	}
}
