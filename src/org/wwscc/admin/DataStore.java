/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2009 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.admin;

import java.util.ArrayList;
import java.util.Map;
import javax.swing.table.AbstractTableModel;
import org.wwscc.storage.Car;
import org.wwscc.storage.Database;
import org.wwscc.storage.Driver;
import org.wwscc.storage.Run;


public class DataStore
{
	Map<Integer, Driver> drivers;
	Map<Integer, Car> cars;
	Map<Integer, Run> runs;

	Driver driverlist[];
	Car carlist[];

	CarModel carModel;
	DriverModel driverModel;
	public AbstractTableModel getCarModel() { return carModel; }
	public AbstractTableModel getDriverModel() { return driverModel; }
	
    public DataStore()
	{
		driverlist = new Driver[0];
		carlist = new Car[0];
		carModel = new CarModel();
		driverModel = new DriverModel();
	}

	public void loadData()
	{
		drivers = Database.d.getAllDrivers();
		cars = Database.d.getAllCars();
		runs = Database.d.getAllRuns();
		dataChanged();
	}


	public void dataChanged()
	{
		driverlist = drivers.values().toArray(new Driver[0]);
		carlist = cars.values().toArray(new Car[0]);

		for (Driver d : drivers.values())
			d.carcount = 0;
		for (Car c : cars.values())
			c.runcount = 0;
		
		for (Car c : cars.values())
		{
			Driver d = drivers.get(c.getDriverId());
			if (d != null) d.carcount++;
		}
		
		for (Run r : runs.values())
		{
			Car c = cars.get(r.getCarId());
			if (c != null) c.runcount++;
		}

		carModel.fireTableDataChanged();
		driverModel.fireTableDataChanged();
	}

	public void deleteDrivers(int rows[])
	{
		ArrayList<Driver> list = new ArrayList<Driver>();
		for (int r : rows)
		{
			drivers.remove(driverlist[r].getId());
			list.add(driverlist[r]);
		}
		Database.d.deleteDrivers(list);
		dataChanged();
	}

	public void deleteCars(int rows[])
	{
		ArrayList<Car> list = new ArrayList<Car>();
		for (int r : rows)
		{
			cars.remove(carlist[r].getId());
			list.add(carlist[r]);
		}
		Database.d.deleteCars(list);
		dataChanged();
	}

	class CarModel extends AbstractTableModel
	{
		Class[] classes = new Class[] { String.class, Integer.class, String.class, String.class, Integer.class };
		String[] names = new String[] { "Class", "Number", "Name", "Desc", "Runs" };

		public CarModel() {}
		public int getRowCount() { return carlist.length; }
		public int getColumnCount() { return names.length; }
		public String getColumnName(int c) { return names[c]; }
		public Class getColumnClass(int c) { return classes[c]; }

		@Override
		public Object getValueAt(int row, int col)
		{
			try
			{
				Car c = carlist[row];
				switch (col)
				{
					case 0: return c.getClassCode();
					case 1: return c.getNumber();
					case 2: Driver d = drivers.get(carlist[row].getDriverId());
							return (d != null) ? d.getFullName() : "-- No Driver --";
					case 3: return String.format("%s %s %s %s", c.getYear(), c.getMake(), c.getModel(), c.getColor());
					case 4: return c.runcount;
				}
			}
			catch (Exception e) {}
			return "XX";
		}
	}

	class DriverModel extends AbstractTableModel
	{
		Class[] classes = new Class[] { String.class, String.class, String.class, String.class, Integer.class };
		String[] names = new String[] { "First", "Last", "Email", "Address", "Cars" };

		public DriverModel() {}
		public int getRowCount() { return driverlist.length; }
		public int getColumnCount() { return names.length; }
		public String getColumnName(int c) { return names[c]; }
		public Class getColumnClass(int c) { return classes[c]; }

		@Override
		public Object getValueAt(int row, int col)
		{
			try
			{
				Driver d = driverlist[row];
				switch (col)
				{
					case 0: return d.getFirstName();
					case 1: return d.getLastName();
					case 2: return d.getEmail();
					case 3: return String.format("%s, %s, %s %s", d.getAddress(), d.getCity(), d.getState(), d.getZip());
					case 4: return d.carcount;
				}
			}
			catch (Exception e) {}
			return "XX";
		}
	}
}
