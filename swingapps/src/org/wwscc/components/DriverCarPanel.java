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
import java.sql.SQLException;
import java.util.UUID;
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
import org.wwscc.storage.MetaCar;
import org.wwscc.util.ApplicationState;
import org.wwscc.util.IdGenerator;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;
import org.wwscc.util.SearchTrigger;


public abstract class DriverCarPanel extends JPanel implements ActionListener, ListSelectionListener, FocusListener
{
	private static final Logger log = Logger.getLogger(DriverCarPanel.class.getCanonicalName());

	public static final String CLEAR      = "Clear";
	public static final String NEWDRIVER  = "New Driver";
	public static final String EDITDRIVER = "Edit Driver";
	public static final String EDITNOTES  = "Edit Notes";

	public static final String NEWCAR     = "New Car";
	public static final String NEWFROM    = "New From";
	public static final String EDITCAR    = "Edit Car";
	public static final String DELETECAR  = "Delete Car";
	
	protected JTextField firstSearch;
	protected JTextField lastSearch;

	protected JScrollPane dscroll;
	protected JList<Object> drivers;
	protected JTextArea driverInfo;

	protected JScrollPane cscroll;
	protected JList<Car> cars;
	protected JTextArea carInfo;

	protected boolean carAddOption = false;
	protected Driver selectedDriver;
	protected MetaCar selectedCar;
	
	protected SearchDrivers searchDrivers = new SearchDrivers();
	protected ApplicationState state;
	
	public DriverCarPanel(ApplicationState s)
	{
		super();
		state = s;
		setLayout(new MigLayout("", "fill"));
		
		selectedDriver = null;
		selectedCar = null;

		/* Search Section */
		firstSearch = new JTextField("", 8);
		firstSearch.getDocument().addDocumentListener(searchDrivers);
		firstSearch.addFocusListener(this);
		lastSearch = new JTextField("", 8);
		lastSearch.getDocument().addDocumentListener(searchDrivers);
		lastSearch.addFocusListener(this);

		/* Driver Section */
		drivers = new JList<Object>();
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
	public void focusOnCar(UUID carid)
	{
		ListModel<Car> lm = cars.getModel();
		for (int ii = 0; ii < lm.getSize(); ii++)
		{
			Car c = (Car)lm.getElementAt(ii);
			if (c.getCarId() == carid)
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

		Vector<MetaCar> touse = new Vector<MetaCar>();
		for (Car c : Database.d.getCarsForDriver(d.getDriverId())) {
			touse.add(Database.d.loadMetaCar(c, state.getCurrentEventId(), state.getCurrentCourse()));
		}

		cars.setListData(touse);
		if (select != null)
			focusOnCar(select.getCarId());
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

		if (cmd.equals(NEWDRIVER))
		{
			DriverDialog dd = new DriverDialog(new Driver(firstSearch.getText(), lastSearch.getText()));
			dd.doDialog(NEWDRIVER, new DialogFinisher<Driver>() {
				@Override
				public void dialogFinished(Driver d) {
					if (d == null) return;
					try {
						Database.d.newDriver(d);
						focusOnDriver(d.getFirstName(), d.getLastName());
					} catch (SQLException ioe) {
						log.log(Level.SEVERE, "Failed to create driver: " + ioe, ioe);
					}
				}
			});
		}

		else if (cmd.equals(EDITDRIVER))
		{
			DriverDialog dd = new DriverDialog(selectedDriver);
			dd.doDialog(EDITDRIVER, new DialogFinisher<Driver>() {
				@Override
				public void dialogFinished(Driver d) {
					if (d == null) return;
					try {
						Database.d.updateDriver(d);
						driverInfo.setText(driverDisplay(d));
						// round about way to fire selected index
						int ii = drivers.getSelectedIndex();
						drivers.clearSelection();
						drivers.setSelectedIndex(ii);
					} catch (SQLException ioe) {
						log.log(Level.SEVERE, "Failed to update driver: " + ioe, ioe);
					}
				}
			});
		}

		else if (cmd.equals(NEWCAR) || cmd.equals(NEWFROM))
		{
			final CarDialog cd;
			if (cmd.equals(NEWFROM) && (selectedCar != null))
			{
				Car initial = new Car(selectedCar);
				initial.setCarId(IdGenerator.generateId());  // Need a new id
				cd = new CarDialog(initial, Database.d.getClassData(), carAddOption);
			}
			else
			{
				cd = new CarDialog(null, Database.d.getClassData(), carAddOption);
			}

			cd.doDialog(NEWCAR, new DialogFinisher<Car>() {
				@Override
				public void dialogFinished(Car c) {
					if (c == null)
						return;
					try
					{
						if (selectedDriver != null)
						{
							c.setDriverId(selectedDriver.getDriverId());
							Database.d.newCar(c);
							reloadCars(c);
							if (cd.getAddToRunOrder())
								Messenger.sendEvent(MT.CAR_ADD, c.getCarId());
						}
					}
					catch (SQLException ioe)
					{
						log.log(Level.SEVERE, "Failed to create a car: " + ioe, ioe);
					}
				}
			});
		}

		else if (cmd.equals(CLEAR))
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

	public static String driverDisplay(Driver d)
	{
		StringBuilder ret = new StringBuilder();
		ret.append(d.getDriverId()).append("\n");
		ret.append(d.getFullName()).append("\n");
		ret.append(d.getAddress()).append("\n");
		ret.append(String.format("%s, %s %s\n", d.getCity(), d.getState(), d.getZip()));
		ret.append(d.getEmail()).append("\n");
		ret.append(d.getPhone()).append("\n");
		ret.append("Member #").append(d.getMembership());
		return ret.toString();
	}


	public static String carDisplay(Car c)
	{
		StringBuilder ret = new StringBuilder();
		ret.append(c.getCarId()).append("\n");
		ret.append(c.getClassCode()).append(" ").append(Database.d.getEffectiveIndexStr(c)).append(" #").append(c.getNumber()).append("\n");
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
