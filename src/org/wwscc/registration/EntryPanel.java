/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */


package org.wwscc.registration;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;

import net.miginfocom.swing.MigLayout;
import org.wwscc.components.DriverCarPanel;
import org.wwscc.components.UnderlineBorder;
import org.wwscc.storage.Car;
import org.wwscc.storage.Database;
import org.wwscc.storage.Driver;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;


public class EntryPanel extends DriverCarPanel
{
	private static Logger log = Logger.getLogger("org.wwscc.dataentry.DriverEntry");

	JButton addit, removeit;

	public EntryPanel()
	{
		super();
		setLayout(new MigLayout("", "fill"));
		Messenger.register(MT.EVENT_CHANGED, this);

		RegListRenderer listRenderer = new RegListRenderer();
		drivers.setCellRenderer(listRenderer);
		cars.setCellRenderer(listRenderer);

		/* Buttons */
		addit = new JButton("Register Entrant");
		addit.addActionListener(this);
		addit.setEnabled(false);

		removeit = new JButton("Unregister Entrant");
		removeit.addActionListener(this);
		removeit.setEnabled(false);

		/* Delete button, row 0 */
		add(createTitle("1. Search"), "spanx 4, wrap");

		add(new JLabel("First Name"), "");
		add(firstSearch, "wrap");
		add(new JLabel("Last Name"), "");
		add(lastSearch, "wrap");
		add(smallButton("Clear"), "skip, wrap");

		add(createTitle("2. Driver"), "spanx 4, wrap");
		add(dscroll, "spanx 2, spany 2, hmin 125");
		add(smallButton("New Driver"), "");
		add(smallButton("Edit Driver"), "wrap");
		add(driverInfo, "spanx2, wrap");

		add(createTitle("3. Car"), "spanx 4, wrap");
		add(cscroll, "spanx 2, spany 2, hmin 125");
		add(smallButton("New Car"), "");
		add(smallButton("New From"), "wrap");
		add(carInfo, "spanx2, wrap");

		add(createTitle("4. Do it"), "spanx 4, wrap");
		add(addit, "");
		add(removeit, "wrap");
	}

	protected JComponent createTitle(String text)
	{
		JLabel lbl = new JLabel(text);
		lbl.setFont(new Font("serif", Font.BOLD, 16));
		lbl.setBorder(new UnderlineBorder(0, 0, 0, 0));

		return lbl;
	}

	protected JButton smallButton(String text)
	{
		JButton b = new JButton(text);
		b.setFont(new Font(null, Font.PLAIN, 11));
		b.addActionListener(this);
		return b;
	}


	/**
	 * Process events from the various buttons
	 *
	 * @param e 
	 */
	@Override
	public void actionPerformed(ActionEvent e)
	{
		String cmd = e.getActionCommand();
		try
		{
			if (cmd.equals("Register Entrant") && (selectedCar != null))
			{
					Database.d.registerCar(selectedCar.getId());
					reloadCars(selectedCar);
			}
			else if (cmd.equals("Unregister Entrant") && (selectedCar != null))
			{
					Database.d.unregisterCar(selectedCar.getId());
					reloadCars(selectedCar);
			}
			else
				super.actionPerformed(e);
		}
		catch (IOException ioe)
		{
			log.log(Level.SEVERE, "Registation failed: " + ioe, ioe);
		}
	}


	/**
	 * One of the list value selections has changed.
	 * This can be either a user selection or the list model was updated
	 */
	@Override
	public void valueChanged(ListSelectionEvent e) 
	{
		super.valueChanged(e);
		if (selectedCar != null)
		{
			addit.setEnabled(!selectedCar.isRegistered);
			removeit.setEnabled(selectedCar.isRegistered && !selectedCar.isInRunOrder);
		}
		else
		{
			addit.setEnabled(false);
			removeit.setEnabled(true);
		}
	}


	@Override
	public void event(MT type, Object o)
	{
		switch (type)
		{
			case EVENT_CHANGED:
				reloadCars(selectedCar);
				break;
		}
	}

	static class RegListRenderer extends DefaultListCellRenderer
	{
		private Icon runs = new ImageIcon(getClass().getResource("/org/wwscc/images/run.gif"));
		private Icon reg = new ImageIcon(getClass().getResource("/org/wwscc/images/reg.gif"));
		private Icon blank = new ImageIcon(getClass().getResource("/org/wwscc/images/blank.gif"));

		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean iss, boolean chf)
		{
			super.getListCellRendererComponent(list, value, index, iss, chf);

			setForeground(Color.BLACK);

			if (value instanceof Car)
			{
				Car c = (Car)value;
				String myclass = c.getClassCode();
				if (!c.getIndexCode().equals(""))
					myclass += " ("+c.getIndexCode()+")";
				
				String data = myclass + " #" + c.getNumber() + ": " + c.getYear() + " " + c.getModel() + " " + c.getColor();
				setText(data);
				if (c.isInRunOrder)
					setIcon(runs);
				else if (c.isRegistered)
					setIcon(reg);
				else
					setIcon(blank);
			}
			else if (value instanceof Driver)
			{
				Driver d = (Driver)value;
				setText(d.getId() + ": " + d.getFullName());
			}

			return this;
		}
	}
}
