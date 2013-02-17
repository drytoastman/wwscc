/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.registration;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
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
import org.wwscc.storage.Car;
import org.wwscc.storage.Driver;
import org.wwscc.storage.Database;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;
import org.wwscc.util.Prefs;


public class EntryPanel extends DriverCarPanel
{
	private static final Logger log = Logger.getLogger(EntryPanel.class.getCanonicalName());

	JButton addit, removeit, editcar, deletecar;
	JComboBox<PrintService> printers;
	Code39 activeLabel;

	public EntryPanel()
	{
		super();
		setLayout(new MigLayout("fill", "[400, grow 25][150, grow 25][150, grow 25]"));
		Messenger.register(MT.EVENT_CHANGED, this);


		drivers.setCellRenderer(new DefaultListCellRenderer() {
			Font font = getFont().deriveFont(14f);
			public Component getListCellRendererComponent(JList<?> l, Object o, int i, boolean s, boolean c)
			{
				super.getListCellRendererComponent(l, o, i, s, c);
				setFont(font);
				setText(((Driver)o).getFullName());
				return this;
			}
		});
		
		cars.setCellRenderer(new RegListRenderer());

		/* Buttons */
		addit = new JButton("Register Entrant");
		addit.addActionListener(this);
		addit.setEnabled(false);
		addit.setFont(addit.getFont().deriveFont(12f));

		removeit = new JButton("Unregister Entrant");
		removeit.addActionListener(this);
		removeit.setEnabled(false);
		removeit.setFont(removeit.getFont().deriveFont(12f));
		
		editcar = new JButton("Edit Car");
		editcar.addActionListener(this);
		editcar.setEnabled(false);
		
		deletecar = new JButton("Delete Car");
		deletecar.addActionListener(this);
		deletecar.setEnabled(false);
		
		activeLabel = new Code39();

		/* Delete button, row 0 */
		add(createTitle("1. Search"), "spanx 3, growx, wrap");

		add(new JLabel("First Name"), "split 2");
		add(firstSearch, "grow, wrap");
		add(new JLabel("Last Name"), "split 2");
		add(lastSearch, "grow, wrap");
		add(smallButton("Clear"), "right, wrap");

		add(createTitle("2. Driver"), "spanx 3, growx, gaptop 4, wrap");
		add(dscroll, "spany 5, grow");
		add(smallButton("New Driver"), "growx");
		add(smallButton("Edit Driver"), "growx, wrap");
		driverInfo.setLineWrap(false);
		add(driverInfo, "spanx 2, growx, wrap");
		add(activeLabel, "center, spanx 2, wrap");
		add(printerList(), "center, spanx 2, wrap");
		add(smallButton("Print Label"), "center, growx, spanx 2, wrap");

		
		add(createTitle("3. Car"), "spanx 3, growx, gaptop 4, wrap");
		add(cscroll, "spany 3, hmin 130, grow");
		add(smallButton("New Car"), "growx");
		add(smallButton("New From"), "growx, wrap");
		add(editcar, "growx"); 
		add(deletecar, "growx, wrap");
		carInfo.setLineWrap(false);
		add(carInfo, "spanx 2, growx, top, wrap");
		
		add(createTitle("4. Do it"), "spanx 3, growx, gaptop 4, wrap");
		add(addit, "split 2, spanx 3, gapbottom 3");
		add(removeit, "wrap");
		
	}

	private JComponent createTitle(String text)
	{
		JLabel lbl = new JLabel(text);
		lbl.setFont(new Font("serif", Font.BOLD, 18));
		lbl.setBorder(new UnderlineBorder(0, 0, 0, 0));

		return lbl;
	}

	private JButton smallButton(String text)
	{
		JButton b = new JButton(text);
		b.setFont(new Font(null, Font.PLAIN, 11));
		b.addActionListener(this);
		return b;
	}
	
	private JComboBox<PrintService> printerList()
	{
		HashPrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
		aset.add(new Copies(2)); // silly request but cuts out fax, xps, etc.
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(
				DocFlavor.SERVICE_FORMATTED.PRINTABLE, 
				aset);		
		printers = new JComboBox<PrintService>(printServices);
		printers.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> jlist, Object e, int i, boolean bln, boolean bln1) {
				super.getListCellRendererComponent(jlist, e, i, bln, bln1);
				setText(((PrintService)e).getName());
				return this;
			}
		});		
		printers.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent ie) {
				Prefs.setDefaultPrinter(((PrintService)printers.getSelectedItem()).getName());
			}
		});
		
		for (PrintService ps : printServices)
		{
			if (ps.getName().equals(Prefs.getDefaultPrinter()))
				printers.setSelectedItem(ps);
		}
		
		return printers;
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
		if (selectedDriver != null)
		{
			activeLabel.setValue(""+selectedDriver.getId(), String.format("%d - %s", selectedDriver.getId(), selectedDriver.getFullName()));
			activeLabel.repaint();
		}
		else
		{
			activeLabel.setValue("", "");
			activeLabel.repaint();
		}
		
		if (selectedCar != null)
		{
			addit.setEnabled(!selectedCar.isRegistered());
			removeit.setEnabled(selectedCar.isRegistered() && !selectedCar.isInRunOrder());
			editcar.setEnabled(!selectedCar.isInRunOrder() && !selectedCar.hasActivity());
			deletecar.setEnabled(!selectedCar.isRegistered() && !selectedCar.isInRunOrder() && !selectedCar.hasActivity());
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
				
			default:
				super.event(type, o);
		}
	}
}
