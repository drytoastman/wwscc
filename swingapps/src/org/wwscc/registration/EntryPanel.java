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
import java.sql.SQLException;
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
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ListModel;
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
import org.wwscc.storage.MetaCar;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;
import org.wwscc.util.Prefs;
import org.wwscc.util.SearchTrigger;


public class EntryPanel extends DriverCarPanel implements MessageListener
{
	private static final Logger log = Logger.getLogger(EntryPanel.class.getCanonicalName());
	
	public static final String REGISTERANDPAY = "Registered and Paid";
	public static final String REGISTERONLY   = "Registered Only";
	public static final String UNREGISTER     = "Unregister";

	JButton registeredandpaid, registerit, unregisterit;
	JButton newdriver, editdriver, editnotes;
	JButton newcar, newcarfrom, editcar, deletecar, print;
	JLabel membershipwarning, noteswarning, paidwarning;
	JComboBox<PrintService> printers;
	Code39 activeLabel;
	SearchDrivers2 searchDrivers2;
	NameStorage extraNames;
	
	public EntryPanel(NameStorage names)
	{
		super(Registration.state);
		setLayout(new MigLayout("fill", "[400, grow 25][:150:200, grow 25][:150:200, grow 25]"));
		Messenger.register(MT.EVENT_CHANGED, this);
		Messenger.register(MT.ATTENDANCE_SETUP_CHANGE, this);
		Messenger.register(MT.BARCODE_SCANNED, this);
		Messenger.register(MT.CAR_CREATED, this);

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
		driverInfo.setLineWrap(false);
		cars.setCellRenderer(new CarRenderer());
		carInfo.setLineWrap(false);

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
		
		newdriver  = smallButton(NEWDRIVER, true);
		editdriver = smallButton(EDITDRIVER, false);
		editnotes  = smallButton(EDITNOTES, false);
		newcar     = smallButton(NEWCAR, false);		
		newcarfrom = smallButton(NEWFROM, false);
		
		editcar = new JButton(new EditCarAction());
		editcar.setEnabled(false);
		
		deletecar = new JButton(new DeleteCarAction());
		deletecar.setEnabled(false);
		
		membershipwarning = new JLabel("");
		membershipwarning.setForeground(Color.WHITE);
		membershipwarning.setBackground(Color.RED);
		
		noteswarning = new JLabel("");
		noteswarning.setForeground(Color.WHITE);
		noteswarning.setBackground(Color.RED);
		
		paidwarning = new JLabel("");
		paidwarning.setForeground(Color.WHITE);
		paidwarning.setBackground(Color.RED);
		paidwarning.setFont(paidwarning.getFont().deriveFont(Font.BOLD, 13));
		paidwarning.setHorizontalAlignment(JLabel.CENTER);
		
		activeLabel = new Code39();
		print = new JButton(new PrintLabelAction());
		print.setFont(new Font(null, Font.PLAIN, 11));
		print.setEnabled(false);

		/* Delete button, row 0 */
		add(createTitle("1. Search"), "spanx 3, growx, wrap");

		add(new JLabel("First Name"), "split 2");
		add(firstSearch, "grow, wrap");
		add(new JLabel("Last Name"), "split 2");
		add(lastSearch, "grow, wrap");
		add(smallButton(CLEAR, true), "right, wrap");

		add(createTitle("2. Driver"), "spanx 3, growx, wrap");
		add(dscroll, "spany 7, grow");
		add(newdriver, "growx, spanx 3, split");
		add(editdriver, "growx");
		add(editnotes, "growx, wrap");
		
		add(driverInfo, "spanx 2, growx, wrap");
		add(membershipwarning, "spanx 2, growx, h 15, wrap");
		add(noteswarning, "spanx 2, growx, h 15, wrap");
		add(activeLabel, "center, spanx 2, wrap");
		add(printers, "center, spanx 2, wrap");
		add(print, "center, growx, spanx 2, wrap");

		add(createTitle("3. Car"), "spanx 3, growx, gaptop 4, wrap");
		add(cscroll, "spany 3, hmin 130, grow");
		add(newcar, "growx");
		add(newcarfrom, "growx, wrap");
		add(editcar, "growx"); 
		add(deletecar, "growx, wrap");
		
		add(carInfo, "spanx 2, growx, top, wrap");
		
		add(paidwarning, "grow, h 15, wrap");
		add(createTitle("4. Do it"), "spanx 3, growx, wrap");
		add(registeredandpaid, "split 3, spanx 3, gapbottom 5");
		add(registerit, "");
		add(unregisterit, "wrap");
		
		new Thread(new FindPrinters()).start();
	}
	
	
	class PrintLabelAction extends AbstractAction
	{
		public PrintLabelAction() { super("Print Label"); }
		@Override
		public void actionPerformed(ActionEvent e)
		{
			try {
				if (selectedDriver == null)
					return;
				
				PrintService ps = (PrintService)printers.getSelectedItem();					
				PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();
				
				attr.add(new Copies(1));
				attr.add((Media)ps.getDefaultAttributeValue(Media.class)); // set to default paper from printer
				attr.add(OrientationRequested.LANDSCAPE);
				
				SimpleDoc doc = new SimpleDoc(activeLabel, DocFlavor.SERVICE_FORMATTED.PRINTABLE, null);
				ps.createPrintJob().print(doc, attr);
			}  catch (PrintException ex) {
				log.log(Level.SEVERE, "Barcode print failed: " + ex.getMessage(), ex);
			}
		}
	}

	
	class EditCarAction extends AbstractAction
	{
		public EditCarAction() { super(EDITCAR); }
		@Override
		public void actionPerformed(ActionEvent e)
		{
			if (selectedCar == null) return;
			final CarDialog cd = new CarDialog(selectedCar, Database.d.getClassData(), false);
			cd.setOkButtonText("Edit");
			cd.doDialog(EDITCAR, new DialogFinisher<Car>() {
				@Override
				public void dialogFinished(Car c) {
					if ((c == null) || (selectedDriver == null))
						return;
					try {
						c.setDriverId(selectedDriver.getDriverId());
						Database.d.updateCar(c);
						reloadCars(c);
					} catch (SQLException ioe) {
						log.log(Level.SEVERE, "Failed to edit car: " + ioe.getMessage(), ioe);
					}
				}
			});
		}
	}
	
	
	class DeleteCarAction extends AbstractAction
	{
		public DeleteCarAction() { super(DELETECAR); }
		@Override
		public void actionPerformed(ActionEvent e)
		{
			if (selectedCar == null) return;
			if (JOptionPane.showConfirmDialog(EntryPanel.this, "Are you sure you want to delete the selected car?", DELETECAR, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
			{
				try {
					Database.d.deleteCar(selectedCar);
					reloadCars(null);
				} catch (SQLException ioe) {
					log.log(Level.SEVERE, "Failed to delete car: " + ioe, ioe);
				}
			}
		}
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
		b.setEnabled(enabled);
		b.addActionListener(this);
		return b;
	}

	public void reloadCars(Car select)
	{
		super.reloadCars(select);
		setPaidWarning();
	}
	
	protected void setPaidWarning()
	{
		paidwarning.setOpaque(false);
		paidwarning.setText("");

		ListModel<Car> m = cars.getModel();
		if (m.getSize() == 0)
			return;
		for (int ii = 0; ii < m.getSize(); ii++)
		{
			MetaCar c = (MetaCar)m.getElementAt(ii);
			if (!c.isInRunOrder() && c.isPaid()) return;
		}
		
		paidwarning.setText("No unused paid cars are present");
		paidwarning.setOpaque(true);
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
				Database.d.registerCar(Registration.state.getCurrentEventId(), selectedCar.getCarId(), false, true);
				reloadCars(selectedCar);
			}
			else if (cmd.equals(REGISTERANDPAY) && (selectedCar != null))
			{
				Database.d.registerCar(Registration.state.getCurrentEventId(), selectedCar.getCarId(), true, true);
				reloadCars(selectedCar);
			}
			else if (cmd.equals(UNREGISTER) && (selectedCar != null))
			{
				Database.d.unregisterCar(Registration.state.getCurrentEventId(), selectedCar.getCarId());
				reloadCars(selectedCar);
			}
			else if (cmd.equals(EDITNOTES) && (selectedDriver != null))
			{
				String ret = (String)JOptionPane.showInputDialog(this, EDITNOTES, noteswarning.getText());
				if (ret != null)
				{
					selectedDriver.setAttrS("notes", ret);
					Database.d.updateDriver(selectedDriver);
					valueChanged(new ListSelectionEvent(drivers, -1, -1, false));
				}
			}
			else
				super.actionPerformed(e);
		}
		catch (SQLException ioe)
		{
			log.log(Level.SEVERE, "Registation action failed: " + ioe, ioe);
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
		if (e.getValueIsAdjusting())
				return;
		
		if (e.getSource() == drivers)
		{
			membershipwarning.setText("");
			membershipwarning.setOpaque(false);
			noteswarning.setText("");
			noteswarning.setOpaque(false);
			
			if (selectedDriver != null)
			{
				editdriver.setEnabled(true);
				activeLabel.setValue(selectedDriver.getMembership(), String.format("%s - %s", selectedDriver.getMembership(), selectedDriver.getFullName()));
				activeLabel.repaint();

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
				
				String notes = selectedDriver.getAttrS("notes");
				if (!notes.trim().equals(""))
				{
					noteswarning.setText(notes);
					noteswarning.setOpaque(true);
				}
				
				Messenger.sendEvent(MT.DRIVER_SELECTED, new Name(selectedDriver.getFirstName(), selectedDriver.getLastName()));
			}
			else
			{
				editdriver.setEnabled(false);
				activeLabel.setValue("", "");
				activeLabel.repaint();
				Messenger.sendEvent(MT.DRIVER_SELECTED, drivers.getSelectedValue());
			}
		}
	
		if (e.getSource() == cars)
		{
			newcar.setEnabled(selectedDriver != null);

			if (selectedCar != null)
			{
				newcarfrom.setEnabled(selectedCar != null);
				editcar.setEnabled(!selectedCar.isInRunOrder() && !selectedCar.hasActivity());
				deletecar.setEnabled(!selectedCar.isRegistered() && !selectedCar.isInRunOrder() && !selectedCar.hasActivity());
				registeredandpaid.setEnabled((!selectedCar.isRegistered() || !selectedCar.isPaid()) && !selectedCar.isInRunOrder());
				registerit.setEnabled((!selectedCar.isRegistered() || selectedCar.isPaid()) && !selectedCar.isInRunOrder());
				unregisterit.setEnabled(selectedCar.isRegistered() && !selectedCar.isInRunOrder());
			}
			else
			{
				newcarfrom.setEnabled(false);
				editcar.setEnabled(false);
				deletecar.setEnabled(false);
				registeredandpaid.setEnabled(false);
				registerit.setEnabled(false);
				unregisterit.setEnabled(false);
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
				break;

			case CAR_CREATED:
				Car c = (Car)o;
				if (JOptionPane.showConfirmDialog(this, 
										"Do you wish to mark this newly created car as registered and paid?",  
										"Register Car",
										JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
				{
					try {
						Database.d.registerCar(Registration.state.getCurrentEventId(), c.getCarId(), true, true);
						reloadCars(c);
					} catch (SQLException e) {
						log.log(Level.WARNING, "Hmm.  I wasn't able to register the car: " + e.getMessage(), e);
					}
				}
				break;
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
	
	
	final static class NameDriverComparator implements Comparator<Object> 
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
