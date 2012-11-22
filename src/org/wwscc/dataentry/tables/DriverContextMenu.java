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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import org.wwscc.components.UnderlineBorder;
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
		List<Car> registered = Database.d.getRegisteredCars(selectedE.getDriverId());
		List<Car> allcars = Database.d.getCarsForDriver(selectedE.getDriverId());

		menu = new JPopupMenu("");
		addTitle("Swap to Registered Car");
		for (Car c : registered) 
			addCarAction(c);

		addTitle("Swap to Unregistered Car");
		boolean removelast = true;
		for (Car c : allcars) {
			if (!registered.contains(c) && !Database.d.isInOrder(c.getId())) {
				addCarAction(c);
				removelast = false;
			}
		}
		if (removelast)
			menu.remove(menu.getComponentCount()-1);
		
		addTitle("Other");
		menu.add(new JMenuItem(new TextRunAction(selectedE))).setFont(itemFont);
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
		JMenuItem lbl = new JMenuItem(String.format("%s(%s) #%s %s %s", c.getClassCode(), c.getIndexCode(), c.getNumber(), c.getMake(), c.getModel()));
		lbl.setFont(itemFont);
		lbl.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				Messenger.sendEvent(MT.CAR_CHANGE, c.getId());
		}});
		
		if (Database.d.isInOrder(c.getId()))
		{
			lbl.setEnabled(false);
			lbl.setForeground(superLightGray);
		}
		
		menu.add(lbl);
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
		Map<Integer, Run> course1, course2;
		course1 = new HashMap<Integer,Run>();
		course2 = new HashMap<Integer,Run>();

		Database.d.setCurrentCourse(1);
		Entrant ent = Database.d.loadEntrant(entrant.getCarId(), false);
		for (Run r : trd.getResult())
		{
			r.updateTo(Database.d.getCurrentEvent().getId(), r.course(), r.run(), entrant.getCarId(), ent.getIndex());
			if (r.course() == 2) course2.put(r.run(), r); else course1.put(r.run(), r);
		}

		ent.setRuns(course1);
		if (course2.size() > 0)
		{
			Database.d.setCurrentCourse(2);
			ent = Database.d.loadEntrant(entrant.getCarId(), false);
			ent.setRuns(course2);
		}

		Database.d.setCurrentCourse(1);
		Messenger.sendEvent(MT.RUNGROUP_CHANGED, 1);
	}
}
