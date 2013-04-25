/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.registration;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.print.DocFlavor;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import net.miginfocom.swing.MigLayout;
import org.wwscc.barcodes.Code39;
import org.wwscc.components.DriverCarPanel;
import org.wwscc.components.UnderlineBorder;
import org.wwscc.dialogs.CarDialog;
import org.wwscc.dialogs.BaseDialog.DialogFinisher;
import org.wwscc.registration.attendance.Name;
import org.wwscc.registration.attendance.NameStorage;
import org.wwscc.storage.BarcodeLookup;
import org.wwscc.storage.BarcodeLookup.LookupException;
import org.wwscc.storage.Car;
import org.wwscc.storage.Driver;
import org.wwscc.storage.Database;
import org.wwscc.storage.Entrant;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;
import org.wwscc.util.Prefs;
import org.wwscc.util.SearchTrigger;


public class EntryPanel extends DriverCarPanel
{
	private static final Logger log = Logger.getLogger(EntryPanel.class.getCanonicalName());
	
	public static final String REGISTERANDPAY = "Registered and Paid";
	public static final String REGISTERONLY = "Registered Only";
	public static final String UNREGISTER = "Unregister";

	JButton registeredandpaid, registerit, unregisterit;
	JButton editcar, deletecar, print;
	JLabel membershipwarning;
	JComboBox<PrintService> printers;
	Code39 activeLabel;
	SearchDrivers2 searchDrivers2;
	NameStorage extraNames;
	
	public EntryPanel(NameStorage names)
	{
		super();
		setLayout(new MigLayout("fill", "[400, grow 25][150, grow 25][150, grow 25]"));
		Messenger.register(MT.EVENT_CHANGED, this);
		Messenger.register(MT.ATTENDANCE_SETUP_CHANGE, this);
		Messenger.register(MT.BARCODE_SCANNED, this);

		extraNames = names;
		
		printers = new JComboBox<PrintService>();
		printers.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> jlist, Object e, int i, boolean bln, boolean bln1) {
				super.getListCellRendererComponent(jlist, e, i, bln, bln1);
				if ((e != null) && (e instanceof PrintService))
					setText(((PrintService)e).getName());
				return this;
			}
		});		
		printers.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent ie) {
				Prefs.setDefaultPrinter(((PrintService)printers.getSelectedItem()).getName());
				print.setEnabled(true);
			}
		});
		
		searchDrivers2 = new SearchDrivers2();
		firstSearch.getDocument().removeDocumentListener(searchDrivers);
		firstSearch.getDocument().addDocumentListener(searchDrivers2);
		lastSearch.getDocument().removeDocumentListener(searchDrivers);
		lastSearch.getDocument().addDocumentListener(searchDrivers2);

		drivers.setCellRenderer(new DriverRenderer());		
		cars.setCellRenderer(new CarRenderer());

		/* Buttons */
		registeredandpaid = new JButton(REGISTERANDPAY);
		registeredandpaid.addActionListener(this);
		registeredandpaid.setEnabled(false);
		registeredandpaid.setFont(registeredandpaid.getFont().deriveFont(12f));

		registerit = new JButton(REGISTERONLY);
		registerit.addActionListener(this);
		registerit.setEnabled(false);
		registerit.setFont(registerit.getFont().deriveFont(12f));

		unregisterit = new JButton(UNREGISTER);
		unregisterit.addActionListener(this);
		unregisterit.setEnabled(false);
		unregisterit.setFont(unregisterit.getFont().deriveFont(12f));
		
		editcar = new JButton("Edit Car");
		editcar.addActionListener(this);
		editcar.setEnabled(false);
		
		deletecar = new JButton("Delete Car");
		deletecar.addActionListener(this);
		deletecar.setEnabled(false);
		
		membershipwarning = new JLabel("");
		membershipwarning.setForeground(Color.WHITE);
		membershipwarning.setBackground(Color.RED);
		
		activeLabel = new Code39();

		/* Delete button, row 0 */
		add(createTitle("1. Search"), "spanx 3, growx, wrap");

		add(new JLabel("First Name"), "split 2");
		add(firstSearch, "grow, wrap");
		add(new JLabel("Last Name"), "split 2");
		add(lastSearch, "grow, wrap");
		add(smallButton("Clear", true), "right, wrap");

		add(createTitle("2. Driver"), "spanx 3, growx, gaptop 4, wrap");
		add(dscroll, "spany 6, grow");
		add(smallButton("New Driver", true), "growx");
		add(smallButton("Edit Driver", true), "growx, wrap");
		driverInfo.setLineWrap(false);
		add(driverInfo, "spanx 2, growx, wrap");
		add(membershipwarning, "spanx 2, growx, h 15, wrap");
		add(activeLabel, "center, spanx 2, wrap");
		add(printers, "center, spanx 2, wrap");
		print = smallButton("Print Label", false);
		add(print, "center, growx, spanx 2, wrap");

		
		add(createTitle("3. Car"), "spanx 3, growx, gaptop 4, wrap");
		add(cscroll, "spany 3, hmin 130, grow");
		add(smallButton("New Car", true), "growx");
		add(smallButton("New From", true), "growx, wrap");
		add(editcar, "growx"); 
		add(deletecar, "growx, wrap");
		carInfo.setLineWrap(false);
		add(carInfo, "spanx 2, growx, top, wrap");
		
		add(createTitle("4. Do it"), "spanx 3, growx, gaptop 4, wrap");
		add(registeredandpaid, "split 3, spanx 3, gapbottom 5");
		add(registerit, "");
		add(unregisterit, "wrap");
		
		new Thread(new FindPrinters()).start();
	}
	
	class FindPrinters implements Runnable
	{
		public void run()
		{
			HashPrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
			aset.add(new Copies(2)); // silly request but cuts out fax, xps, etc.
	        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(DocFlavor.SERVICE_FORMATTED.PRINTABLE, aset);			
			for (PrintService ps : printServices) {
				log.log(Level.INFO, "Found printer: {0}", ps);
				printers.addItem(ps);
				if (ps.getName().equals(Prefs.getDefaultPrinter()))
					printers.setSelectedItem(ps);
			}
		}
	}

	private JComponent createTitle(String text)
	{
		JLabel lbl = new JLabel(text);
		lbl.setFont(new Font("serif", Font.BOLD, 18));
		lbl.setBorder(new UnderlineBorder(0, 0, 0, 0));

		return lbl;
	}

	private JButton smallButton(String text, boolean enabled)
	{
		JButton b = new JButton(text);
		b.setFont(new Font(null, Font.PLAIN, 11));
		b.addActionListener(this);
		b.setEnabled(enabled);
		return b;
	}

	public void reloadCars(Car select)
	{
		super.reloadCars(select);
		Messenger.sendEvent(MT.TRACKING_CHANGE_MADE, null);
	}
	
	/**
	 * Process events from the various buttons
	 * @param e 
	 */
	@Override
	public void actionPerformed(ActionEvent e)
	{
		String cmd = e.getActionCommand();
		try
		{
			if (cmd.equals(REGISTERONLY) && (selectedCar != null))
			{
				Database.d.registerCar(selectedCar.getId(), false, true);
				reloadCars(selectedCar);
			}
			else if (cmd.equals(REGISTERANDPAY) && (selectedCar != null))
			{
				Database.d.registerCar(selectedCar.getId(), true, true);
				reloadCars(selectedCar);
			}
			else if (cmd.equals(UNREGISTER) && (selectedCar != null))
			{
				Database.d.unregisterCar(selectedCar.getId());
				reloadCars(selectedCar);
			}
			else if (cmd.equals("Delete Car") && (selectedCar != null))
			{
				if (JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the selected car?", "Delete Car", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
				{
					Database.d.deleteCar(selectedCar);
					reloadCars(null);
				}
			}
			else if (cmd.equals("Edit Car") && (selectedCar != null))
			{
				final CarDialog cd = new CarDialog(selectedCar, Database.d.getClassData(), false);
				cd.setOkButtonText("Edit");
				cd.doDialog("Edit Car", new DialogFinisher<Car>() {
					@Override
					public void dialogFinished(Car c) {
						if ((c == null) || (selectedDriver == null))
							return;
						try {
							c.setDriverId(selectedDriver.getId());
							Database.d.updateCar(c);
							reloadCars(c);
						} catch (IOException ioe) {
							log.log(Level.SEVERE, "Failed to edit car: " + ioe, ioe);
						}
					}
				});
			}
			else if (cmd.equals("Print Label") && (selectedDriver != null))
			{
				try {
					PrintService ps = (PrintService)printers.getSelectedItem();
					ps.createPrintJob().print(new SimpleDoc(activeLabel, DocFlavor.SERVICE_FORMATTED.PRINTABLE, null), null);
				} catch (PrintException ex) {
					log.log(Level.SEVERE, "Barcode print failed: " + ex.getMessage(), ex);
				}
			}
			else
				super.actionPerformed(e);
		}
		catch (IOException ioe)
		{
			log.log(Level.SEVERE, "Registation failed: " + ioe, ioe);
		}
		
		Messenger.sendEvent(MT.TRACKING_CHANGE_MADE, null);
	}


	/**
	 * One of the list value selections has changed.
	 * This can be either a user selection or the list model was updated
	 */
	@Override
	public void valueChanged(ListSelectionEvent e) 
	{
		super.valueChanged(e);
		if (e.getValueIsAdjusting())
				return;
		
		if (e.getSource() == drivers)
		{
			if (selectedDriver != null)
			{
				activeLabel.setValue(selectedDriver.getMembership(), String.format("%s - %s", selectedDriver.getMembership(), selectedDriver.getFullName()));
				activeLabel.repaint();
				membershipwarning.setText("");
				membershipwarning.setOpaque(false);
				
				if (!selectedDriver.getMembership().trim().equals(""))
				{
					List<Driver> dups = Database.d.findDriverByMembership(selectedDriver.getMembership());
					dups.remove(selectedDriver);
					if (dups.size() > 0)
					{
						StringBuffer buf = new StringBuffer(dups.get(0).getFullName());
						for (int ii = 1; ii < dups.size(); ii++)
							buf.append(", ").append(dups.get(ii).getFullName());
						membershipwarning.setText("Duplicate Membership with " + buf);
						membershipwarning.setOpaque(true);
					}
				}
				Messenger.sendEvent(MT.DRIVER_SELECTED, new Name(selectedDriver.getFirstName(), selectedDriver.getLastName()));
			}
			else
			{
				activeLabel.setValue("", "");
				activeLabel.repaint();
				membershipwarning.setText("");
				membershipwarning.setOpaque(false);
				Messenger.sendEvent(MT.DRIVER_SELECTED, drivers.getSelectedValue());
			}
		}
	
		if (e.getSource() == cars)
		{
			if (selectedCar != null)
			{
				registeredandpaid.setEnabled((!selectedCar.isRegistered() || !selectedCar.isPaid()) && !selectedCar.isInRunOrder());
				registerit.setEnabled((!selectedCar.isRegistered() || selectedCar.isPaid()) && !selectedCar.isInRunOrder());
				unregisterit.setEnabled(selectedCar.isRegistered() && !selectedCar.isInRunOrder());
				
				editcar.setEnabled(!selectedCar.isInRunOrder() && !selectedCar.hasActivity());
				deletecar.setEnabled(!selectedCar.isRegistered() && !selectedCar.isInRunOrder() && !selectedCar.hasActivity());
			}
			else
			{
				registeredandpaid.setEnabled(false);
				registerit.setEnabled(false);
				unregisterit.setEnabled(true);
			}
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
				
			case ATTENDANCE_SETUP_CHANGE:
				if (selectedDriver != null)
					Messenger.sendEvent(MT.DRIVER_SELECTED, new Name(selectedDriver.getFirstName(), selectedDriver.getLastName()));
				break;
			
			case BARCODE_SCANNED:
				try {
					Object found = BarcodeLookup.findObjectByBarcode((String)o);
					if (found instanceof Driver) {
						Driver d = (Driver)found;
						focusOnDriver(d.getFirstName(), d.getLastName());
					} else if (found instanceof Entrant) {
						Entrant e = (Entrant)found;
						focusOnDriver(e.getFirstName(), e.getLastName());
						focusOnCar(e.getCarId());
					} else {
						throw new LookupException("No object found");
					}
				} catch (LookupException e) {
					log.warning("Barcode lookup exception: " + e.getMessage());
				}

			default:
				super.event(type, o);
		}
	}
	

	class SearchDrivers2 extends SearchTrigger
	{
		@Override
		public void search(String txt)
		{
			String first = null, last = null;
			if (lastSearch.getDocument().getLength() > 0) 
				last = lastSearch.getText();
			if (firstSearch.getDocument().getLength() > 0)
				first = firstSearch.getText();
			
			List<Object> display = new ArrayList<Object>();
			HashSet<Name> used = new HashSet<Name>();
			for (Driver d : Database.d.getDriversLike(first, last))
			{
				display.add(d);
				used.add(new Name(d.getFirstName(), d.getLastName()));
			}
			
			for (Name n : extraNames.getNamesLike(first, last))
			{
				if (!used.contains(n))
					display.add(n);
			}
			

			Collections.sort(display, new NameDriverComparator());			
			drivers.setListData(display.toArray());
			drivers.setSelectedIndex(0);
		}
	}
	
	
	class NameDriverComparator implements Comparator<Object> 
	{
		public int compare(Object o1, Object o2)
		{
			String c1 = "", c2 = "";
			
			if (o1 instanceof Driver)
				c1 = ((Driver)o1).getFullName();
			else if (o1 instanceof Name)
				c1 = ((Name)o1).toString();

			if (o2 instanceof Driver)
				c2 = ((Driver)o2).getFullName();
			else if (o2 instanceof Name)
				c2 = ((Name)o2).toString();
						
			return c1.compareTo(c2);

		}
	}
}
