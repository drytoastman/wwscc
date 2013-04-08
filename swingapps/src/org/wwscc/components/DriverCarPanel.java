/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2010 Brett Wilson.
 * All rights reserved.
 */


package org.wwscc.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.miginfocom.swing.MigLayout;
import org.wwscc.dialogs.BaseDialog.DialogFinisher;
import org.wwscc.dialogs.CarDialog;
import org.wwscc.dialogs.DriverDialog;
import org.wwscc.storage.Car;
import org.wwscc.storage.Database;
import org.wwscc.storage.Driver;
import org.wwscc.storage.DriverField;
import org.wwscc.storage.MetaCar;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;
import org.wwscc.util.SearchTrigger;


public abstract class DriverCarPanel extends JPanel implements ActionListener, ListSelectionListener, FocusListener, MessageListener
{
	private static final Logger log = Logger.getLogger(DriverCarPanel.class.getCanonicalName());

	protected JTextField firstSearch;
	protected JTextField lastSearch;

	protected JScrollPane dscroll;
	protected JList<Driver> drivers;
	protected JTextArea driverInfo;

	protected JScrollPane cscroll;
	protected JList<Car> cars;
	protected JTextArea carInfo;

	protected boolean carAddOption = false;
	protected Driver selectedDriver;
	protected MetaCar selectedCar;
	
	protected List<DriverField> driverFields;

	public DriverCarPanel()
	{
		super();
		setLayout(new MigLayout("", "fill"));

		Messenger.register(MT.DATABASE_CHANGED, this);
		
		selectedDriver = null;
		selectedCar = null;

		/* Search Section */
		SearchDrivers searchDrivers = new SearchDrivers();
		firstSearch = new JTextField("", 8);
		firstSearch.getDocument().addDocumentListener(searchDrivers);
		firstSearch.addFocusListener(this);
		lastSearch = new JTextField("", 8);
		lastSearch.getDocument().addDocumentListener(searchDrivers);
		lastSearch.addFocusListener(this);

		/* Driver Section */
		drivers = new JList<Driver>();
		drivers.addListSelectionListener(this);
		drivers.setVisibleRowCount(1);
		//drivers.setPrototypeCellValue("12345678901234567890");
		drivers.setSelectionMode(0);

		dscroll = new JScrollPane(drivers);
		dscroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		dscroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		dscroll.getVerticalScrollBar().setPreferredSize(new Dimension(15,200));

		// create default height
		driverInfo = displayArea("\n\n\n\n\n");

		/* Car Section */
		cars = new JList<Car>();
		cars.addListSelectionListener(this);
		cars.setVisibleRowCount(2);
		//cars.setPrototypeCellValue("12345678901234567890");
		cars.setSelectionMode(0);
	
		cscroll = new JScrollPane(cars);
		cscroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		cscroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		cscroll.getVerticalScrollBar().setPreferredSize(new Dimension(15,200));

		carInfo = displayArea("\n");
	}


	@Override
	public void event(MT type, Object o)
	{
		switch (type)
		{
			case DATABASE_CHANGED:
				try {
					driverFields = Database.d.getDriverFields();
				} catch (IOException ex) {
					log.log(Level.WARNING, "Can''t load extra driver fields, editor will be hamstrung: {0}", ex);
				}
				break;
		}
	}
				
	private JTextArea displayArea(String text)
	{
		JTextArea ta = new JTextArea(text);
		ta.setEditable(false);
		ta.setLineWrap(true);
		ta.setWrapStyleWord(true);
		ta.setBackground((Color)UIManager.get("Label.background"));
		ta.setForeground(new Color(20, 20, 150));
		ta.setFont(new Font("Dialog", Font.PLAIN, 12));

		return ta;
	}


	/**
	 * Set the name search fields and select the name.
	 * @param firstname the value to put in the firstname field
	 * @param lastname  the value to put in the lastname field
	 */
	public void focusOnDriver(String firstname, String lastname)
	{
		firstSearch.setText(firstname);
		lastSearch.setText(lastname);
		drivers.setSelectedIndex(0);
		drivers.ensureIndexIsVisible(0);
	}


	/**
	 * Set the car list to select a particular carid if its in the list.
	 * @param carid  the id of the car to select
	 */
	public void focusOnCar(int carid)
	{
		ListModel<Car> lm = cars.getModel();
		for (int ii = 0; ii < lm.getSize(); ii++)
		{
			Car c = (Car)lm.getElementAt(ii);
			if (c.getId() == carid)
			{
				cars.setSelectedIndex(ii);
				cars.ensureIndexIsVisible(ii);
				break;
			}
		}
	}


	/**
	 * Reload the carlist based on the selected driver, and optionally select one.
	 * @param select
	 */
	public void reloadCars(Car select)
	{
		log.log(Level.FINE, "Reload cars ({0})", select);
		Driver d = (Driver)drivers.getSelectedValue();

		if (d == null) // nothing to do
			return;

		List<Car> driverCars = Database.d.getCarsForDriver(d.getId());
		Vector<MetaCar> touse = new Vector<MetaCar>();
		for (Iterator<Car> citer = driverCars.iterator(); citer.hasNext(); )
		{
			Car c = citer.next();
			touse.add(new MetaCar(c, Database.d.isRegistered(c), Database.d.isInOrder(c.getId()), Database.d.hasActivity(c.getId())));
		}

		cars.setListData(touse);
		if (select != null)
			focusOnCar(select.getId());
		else
			cars.setSelectedIndex(0);
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

		if (cmd.equals("New Driver"))
		{
			DriverDialog dd = new DriverDialog(new Driver(firstSearch.getText(), lastSearch.getText()), driverFields);
			dd.doDialog("New Driver", new DialogFinisher<Driver>() {
				@Override
				public void dialogFinished(Driver d) {
					if (d == null) return;
					try {
						Database.d.newDriver(d);
						focusOnDriver(d.getFirstName(), d.getLastName());
					} catch (IOException ioe) {
						log.log(Level.SEVERE, "Failed to create driver: " + ioe, ioe);
					}
				}
			});
		}

		else if (cmd.equals("Edit Driver"))
		{
			DriverDialog dd = new DriverDialog(selectedDriver, driverFields);
			dd.doDialog("Edit Driver", new DialogFinisher<Driver>() {
				@Override
				public void dialogFinished(Driver d) {
					if (d == null) return;
					try {
						Database.d.updateDriver(d);
						driverInfo.setText(driverDisplay(d));
					} catch (IOException ioe) {
						log.log(Level.SEVERE, "Failed to update driver: " + ioe, ioe);
					}
				}
			});
		}

		else if (cmd.equals("New Car") || cmd.equals("New From"))
		{
			final CarDialog cd;
			if (cmd.equals("New From"))
				cd = new CarDialog(selectedCar, Database.d.getClassData(), carAddOption);
			else
				cd = new CarDialog(null, Database.d.getClassData(), carAddOption);

			cd.doDialog("New Car", new DialogFinisher<Car>() {
				@Override
				public void dialogFinished(Car c) {
					if (c == null)
						return;
					try
					{
						if (selectedDriver != null)
						{
							c.setDriverId(selectedDriver.getId());
							Database.d.newCar(c);
							reloadCars(c);
							if (cd.getAddToRunOrder())
								Messenger.sendEvent(MT.CAR_ADD, c.getId());
						}
					}
					catch (IOException ioe)
					{
						log.log(Level.SEVERE, "Failed to create a car: " + ioe, ioe);
					}
				}
			});
		}

		else if (cmd.equals("Clear"))
		{
			firstSearch.setText("");
			lastSearch.setText("");
			firstSearch.requestFocus();
		}

		else
		{
			log.log(Level.INFO, "Unknown command in DriverEntry: {0}", cmd);
		}
	}


	/**
	 * One of the list value selections has changed.
	 * This can be either a user selection or the list model was updated
	 */
	@Override
	public void valueChanged(ListSelectionEvent e) 
	{
		if (e.getValueIsAdjusting() == false)
		{
			Object source = e.getSource();
			if (source == drivers)
			{
				Object o = drivers.getSelectedValue();
				if (o instanceof Driver)
				{
					selectedDriver = (Driver)o;
					driverInfo.setText(driverDisplay(selectedDriver));
					reloadCars(null);
				}
				else
				{
					selectedDriver = null;
					driverInfo.setText("\n\n\n\n");
					cars.setListData(new Car[0]);
					cars.clearSelection();
				}
			}

			else if (source == cars)
			{
				Object o = cars.getSelectedValue();
				if (o instanceof MetaCar)
				{
					selectedCar = (MetaCar)o;
					carInfo.setText(carDisplay(selectedCar));
				}
				else
				{
					selectedCar = null;
					carInfo.setText("\n");
				}
			}
		}
	}

	protected String driverDisplay(Driver d)
	{
		StringBuilder ret = new StringBuilder();
		ret.append(d.getFullName()).append("  (driverid=").append(d.getId()).append(")\n");
		ret.append(d.getAddress()).append("\n");
		ret.append(String.format("%s, %s %s\n", d.getCity(), d.getState(), d.getZip()));
		ret.append(d.getEmail()).append("\n");
		ret.append(d.getPhone()).append("\n");
		ret.append("Member #").append(d.getMembership());
		return ret.toString();
	}


	protected String carDisplay(Car c)
	{
		StringBuilder ret = new StringBuilder();
		ret.append(c.getClassCode()).append(" ").append(c.getIndexStr()).append(" #").append(c.getNumber());
		ret.append("  (carid=").append(c.getId()).append(")\n");
		ret.append(c.getYear() + " " + c.getMake() + " " + c.getModel() + " " + c.getColor());
		return ret.toString();
	}


	@Override
	public void focusGained(FocusEvent e)
	{
		JTextField tf = (JTextField)e.getComponent();
		tf.selectAll();
	}

	@Override
	public void focusLost(FocusEvent e)
	{
		JTextField tf = (JTextField)e.getComponent();
		tf.select(0,0);
	}
	
	class SearchDrivers extends SearchTrigger
	{
		@Override
		public void search(String txt)
		{
			String first = null, last = null;
			if (lastSearch.getDocument().getLength() > 0) 
				last = lastSearch.getText();
			if (firstSearch.getDocument().getLength() > 0)
				first = firstSearch.getText();
			drivers.setListData(new Vector<Driver>(Database.d.getDriversLike(first, last)));
			drivers.setSelectedIndex(0);
		}
	}
}
