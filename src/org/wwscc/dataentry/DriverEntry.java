/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */


package org.wwscc.dataentry;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;

import org.wwscc.components.DriverCarPanel;
import org.wwscc.components.UnderlineBorder;
import org.wwscc.storage.Car;
import org.wwscc.storage.Driver;
import org.wwscc.storage.Entrant;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;


public class DriverEntry extends DriverCarPanel
{
	private static Logger log = Logger.getLogger(DriverEntry.class.getCanonicalName());

	JButton addit, changeit;
	boolean carAlreadyInOrder = true;
	boolean entrantIsSelected = false;

	public DriverEntry()
	{
		super();
		setLayout(new GridBagLayout());
		carAddOption = true;

		Messenger.register(MT.OBJECT_CLICKED, this);
		Messenger.register(MT.OBJECT_DCLICKED, this);
		Messenger.register(MT.ENTRANTS_CHANGED, this);

		MyListRenderer listRenderer = new MyListRenderer();
		drivers.setCellRenderer(listRenderer);
		cars.setCellRenderer(listRenderer);
		
		/* Buttons */
		addit = new JButton("Add Entrant");	
		addit.addActionListener(this);
		addit.setEnabled(false);

		changeit = new JButton("Swap Entrant");	
		changeit.addActionListener(this);
		changeit.setEnabled(false);

		driverInfo.setFont(new Font("Dialog", Font.PLAIN, 10));
		carInfo.setFont(new Font("Dialog", Font.PLAIN, 10));
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 1;
		c.weighty = 1;
		c.insets = new Insets(2, 4, 2, 4);

		int y = 0;

		/* Delete button, row 0 */
		c.gridx = 0; c.gridy = y++; c.gridwidth = 2; c.weighty = 0;   add(createTitle("1. Search"), c); 

		c.gridx = 0; c.gridy = y;   c.gridwidth = 1; c.weighty = 0;   add(new JLabel("First Name"), c);
		c.gridx = 1; c.gridy = y++; c.gridwidth = 1; c.weighty = 0;   add(firstSearch, c);
		c.gridx = 0; c.gridy = y;   c.gridwidth = 1; c.weighty = 0;   add(new JLabel("Last Name"), c);
		c.gridx = 1; c.gridy = y++; c.gridwidth = 1; c.weighty = 0;   add(lastSearch, c);
		c.gridx = 1; c.gridy = y++; c.gridwidth = 1; c.weighty = 0;   add(smallButton("Clear"), c);

		c.gridx = 0; c.gridy = y++; c.gridwidth = 2; c.weighty = 0;   add(createTitle("2. Driver"), c);
		c.gridx = 0; c.gridy = y++; c.gridwidth = 2; c.weighty = 0.9; add(dscroll, c);
		c.gridx = 0; c.gridy = y++;   c.gridwidth = 1; c.weighty = 0;   add(smallButton("New Driver"), c);
		c.gridx = 0; c.gridy = y++; c.gridwidth = 2; c.weighty = 0;   add(driverInfo, c);

		c.gridx = 0; c.gridy = y++; c.gridwidth = 2; c.weighty = 0;   add(createTitle("3. Car"), c);
		c.gridx = 0; c.gridy = y++; c.gridwidth = 2; c.weighty = 0.8; add(cscroll, c);
		c.gridx = 0; c.gridy = y;   c.gridwidth = 1; c.weighty = 0;   add(smallButton("New Car"), c);
		c.gridx = 1; c.gridy = y++; c.gridwidth = 1; c.weighty = 0;   add(smallButton("New From"), c);
		c.gridx = 0; c.gridy = y++; c.gridwidth = 2; c.weighty = 0;   add(carInfo, c);

		c.gridx = 0; c.gridy = y++; c.gridwidth = 2; c.weighty = 0;   add(createTitle("4. Do it"), c);
		c.gridx = 0; c.gridy = y;   c.gridwidth = 1; c.weighty = 0.0; add(addit, c);
		c.gridx = 1; c.gridy = y++; c.gridwidth = 1; c.weighty = 0.0; add(changeit, c);
		/* Fill vertical space so everything stays at the top */
		c.gridx = 0; c.gridy = y++; c.gridwidth = 2; c.weighty = 1;   add(new JLabel(""), c);
	}


	protected JComponent createTitle(String text)
	{
		JLabel lbl = new JLabel(text);
		lbl.setFont(new Font("serif", Font.BOLD, 16));
		lbl.setBorder(new UnderlineBorder(10, 0, 0, 0));

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
	 */
	@Override
	public void actionPerformed(ActionEvent e)
	{
		String cmd = e.getActionCommand();

		if (cmd.equals("Add Entrant"))
		{
			if (selectedCar != null)
				Messenger.sendEvent(MT.CAR_ADD, selectedCar.getId());
		}

		else if (cmd.equals("Swap Entrant"))
		{
			if (selectedCar != null)
				Messenger.sendEvent(MT.CAR_CHANGE, selectedCar.getId());
		}

		else
			super.actionPerformed(e);
	}


	/**
	 * One of the list value selections has changed.
	 * This can be either a user selection or the list model was updated
	 */
	@Override
	public void valueChanged(ListSelectionEvent e) 
	{
		super.valueChanged(e);
		carAlreadyInOrder = ((selectedCar == null) || (selectedCar.isInRunOrder));
		addit.setEnabled(!carAlreadyInOrder);
		changeit.setEnabled(!carAlreadyInOrder && entrantIsSelected);
	}

	
	@Override
	public void event(MT type, Object o)
	{
		switch (type)
		{
			case OBJECT_CLICKED:
				entrantIsSelected = (o instanceof Entrant);
				changeit.setEnabled(!carAlreadyInOrder && entrantIsSelected);
				break;

			case OBJECT_DCLICKED:
				if (o instanceof Entrant)
				{
					Entrant e = (Entrant)o;
					focusOnDriver(e.getFirstName(), e.getLastName());
					focusOnCar(e.getCarId());
				}
				break;

			case ENTRANTS_CHANGED: // resync loaded cars to check status
				reloadCars(selectedCar);
				break;
				
			default:
				super.event(type, o);
		}
	}
	

	class MyListRenderer extends DefaultListCellRenderer 
	{
		private Color mygray = new Color(220,220,220);

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
				
				setText(myclass + " #" + c.getNumber() + ": " + c.getYear() + " " + c.getModel() + " " + c.getColor());
				if (c.isInRunOrder)
				{
					setForeground(mygray);
					if (iss)
						setBackground(Color.GRAY);
				}
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
