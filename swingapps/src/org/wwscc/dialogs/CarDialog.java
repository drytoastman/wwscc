/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dialogs;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JComboBox;
import net.miginfocom.swing.MigLayout;
import org.wwscc.storage.Car;
import org.wwscc.storage.ClassData;
import org.wwscc.storage.Database;


/**
 * Core functions for all dialogs.
 */
public class CarDialog extends BaseDialog<Car>
{
	private static Logger log = Logger.getLogger("org.wwscc.dialogs.CarDialog");
	
	protected JButton add;
	protected boolean addToRunOrder;
	
	/**
	 * Create the dialog.
	 * @param car		the initial car data to use
	 * @param cd		the classdata to use for classes/indexes
	 * @param addoption whether to add the create and add button
	 */
    public CarDialog(Car car, ClassData cd, boolean addoption)
	{
		super(new MigLayout("", "[70, align right][100, fill]"), true);

		if (car == null)
			car = new Car();
		
		addToRunOrder = false;
		if (addoption)
		{
			add = new JButton("Create and Add");
			add.addActionListener(this);
			buttonPanel.add(add, 0);
			defaultButton = add;
		}
		else
		{
			defaultButton = ok;
		}

		ok.setText("Create");

		mainPanel.add(label("Year", false), "");
		mainPanel.add(entry("year", car.getYear()), "wrap");

		List<String> makes = Database.d.getCarAttributes("make");
		mainPanel.add(label("Make", false), "");
		mainPanel.add(autoentry("make", car.getMake(), makes), "wrap");

		List<String> models = Database.d.getCarAttributes("model");
		mainPanel.add(label("Model", false), "");
		mainPanel.add(autoentry("model", car.getModel(), models), "wrap");

		List<String> colors = Database.d.getCarAttributes("color");
		mainPanel.add(label("Color", false), "");
		mainPanel.add(autoentry("color", car.getColor(), colors), "wrap");

		mainPanel.add(label("Number", true), "");
		mainPanel.add(ientry("number", (car.getNumber()>0)?car.getNumber():null), "wrap");

		List<ClassData.Class> classlist = cd.getClasses();
		List<ClassData.Index> indexlist = cd.getIndexes();
		Collections.sort(classlist, new ClassData.Class.StringOrder());
		Collections.sort(indexlist, new ClassData.Index.StringOrder());

		mainPanel.add(label("Class", true), "");
		mainPanel.add(select("classcode", cd.getClass(car.getClassCode()), classlist, this), "wrap");

		mainPanel.add(label("Index", true), "");
		mainPanel.add(select("indexcode", cd.getIndex(car.getIndexCode()), indexlist, null), "wrap");

		mainPanel.add(label("Global Tire Index", true), "");
		mainPanel.add(checkbox("tireindexed", car.isTireIndexed()), "wrap");
		
		actionPerformed(new ActionEvent(selects.get("classcode"), 1, ""));

		result = car;
    }
	 

	@Override
	public void actionPerformed(ActionEvent ae)
	{
		Object o = ae.getSource();

		if (o instanceof JComboBox)
		{
			JComboBox<?> cb = (JComboBox<?>)ae.getSource();
			ClassData.Class c = (ClassData.Class)cb.getSelectedItem();
			if (c == null) return;

			JComboBox<Object> index = selects.get("indexcode");
			index.setEnabled(c.carsNeedIndex());
		}
		else if (ae.getSource() == add)
		{
			addToRunOrder = true;
			ae.setSource(ok);
			super.actionPerformed(ae);
		}
		else
		{
			super.actionPerformed(ae);
		}
	}

	/**
	 * Determine if the dialog wanted to add to run order
	 * @return true if should directly add to runorder
	 */
	public boolean getAddToRunOrder()
	{
		return addToRunOrder;
	}
	
	/**
	 * Called after OK to verify data before closing.
	 * @return
	 */
	@Override
	public boolean verifyData()
	{
		try
		{
			ClassData.Class c = (ClassData.Class)getSelect("classcode");
			if (c.getCode().equals(""))
			{
				errorMessage = "Must select a class";
				return false;
			}

			if (c.carsNeedIndex())
			{
				ClassData.Index i = (ClassData.Index)getSelect("indexcode");
				if (i.getCode().equals(""))
				{
					errorMessage = "Selected class requires an index";
					return false;
				}
			}
	
			if (getEntryText("number").equals(""))
			{
				errorMessage = "Please enter a car number";
				return false;
			}
		}
		catch (Exception e)
		{
			errorMessage = "Error: " + e.getMessage();
			return false;
		}

		return true;
	}


	/**
	 * Data is good, return it.
	 * @return
	 */
	@Override
	public Car getResult()
	{
		if (!valid)
			return null;
		
		try
		{
			result.setYear(getEntryText("year"));
			result.setMake(getEntryText("make"));
			result.setModel(getEntryText("model"));
			result.setColor(getEntryText("color"));
	
			ClassData.Class c = (ClassData.Class)getSelect("classcode");
			result.setClassCode(c.getCode());

			if (c.carsNeedIndex())
			{
				ClassData.Index i = (ClassData.Index)getSelect("indexcode");
				result.setIndexCode(i.getCode());
			}
			else
			{
				result.setIndexCode("");
			}
	
			result.setTireIndexed(isChecked("tireindexed"));
			result.setNumber(Integer.valueOf(getEntryText("number")));
			return result;
		}
		catch (Exception e)
		{
			log.severe("Bad data in CarDialog: " + e);
			return null;
		}
	}
}


