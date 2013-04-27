package org.wwscc.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wwscc.util.Logging;

/**
 * 	expects 'blank.db' to be present in the current directory.  blank.db is a current schema database with at least one event
 *	see pythonwebservice/scripts/blankdb.py to recreate this file
 */
public class MergeTest 
{
	File testdir;
	@Before
	public void setUp() throws Exception 
	{
		tearDown(); // make sure its clean
		testdir = new File(System.getProperty("user.dir"), "tests/org/wwscc/storage");
	}

	@After
	public void tearDown() throws Exception 
	{
		new File("testhost.db").delete();
		new File("testclient.db").delete();
		new File(Logging.getLogDir(), "testclientchanges.log").delete();
		new File(Logging.getLogDir(), "testclientchanges.log.1").delete();
	}
	
	/**
	 * Take a blank database, add some known data to it
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 */
	private void createTestDBs() throws IOException, ClassNotFoundException
	{
		Files.copy(Paths.get(testdir.getPath(), "blank.db"), Paths.get("testhost.db"), StandardCopyOption.REPLACE_EXISTING);
		Database.openDatabaseFile(new File("testhost.db"));
		Database.d.setCurrentEvent(Database.d.getEvents().get(0));
		
		Driver d1 = new Driver("Existing", "Driver"); // driverid = 1
		Database.d.newDriver(d1);
		Assert.assertEquals(1, d1.getId());

		Car c1a = new Car();  // carid = 1
		c1a.setDriverId(d1.getId());
		c1a.setModel("Existing Car");
		Database.d.newCar(c1a);
		Assert.assertEquals(1, c1a.getId());
		Database.d.registerCar(c1a.getId(), false, true); // unpaid registration
		
		Car c1b = new Car(); // carid = 2
		c1b.setDriverId(d1.getId());
		c1b.setModel("Car to be updated");				
		Database.d.newCar(c1b);
		Assert.assertEquals(2, c1b.getId());
		Database.d.registerCar(c1a.getId(), false, true); // unpaid registration

		Database.d.close();
		
		createTestDBClient(); // this stuff is what happens 'offline' in registration
		
		Database.openDatabaseFile(new File("testhost.db"));
		Database.d.setCurrentEvent(Database.d.getEvents().get(0));
		
		// now do some things that happen after the other program got a copy
		Driver d2 = new Driver("New DataEntry", "Driver");  // driverid = 2
		Database.d.newDriver(d2);
		Assert.assertEquals(2, d2.getId());
		
		Car c2 = new Car(); // carid = 3
		c2.setModel("New DataEntry Car");
		c2.setDriverId(d2.getId());
		Database.d.newCar(c2);
		Assert.assertEquals(3, c2.getId());
		
		Database.d.close();
	}
	
	/**
	 * Take a copy of the test host database and do some registration like things to create testclient
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 */
	private void createTestDBClient() throws IOException, ClassNotFoundException
	{
		Files.copy(Paths.get("testhost.db"), Paths.get("testclient.db"), StandardCopyOption.REPLACE_EXISTING);
		Database.openDatabaseFile(new File("testclient.db"));
		Database.d.setChangeTracker(new ChangeTracker("testclient"));
		Database.d.setCurrentEvent(Database.d.getEvents().get(0));
		
		Driver d2 = new Driver("New Registration", "Driver");
		Database.d.newDriver(d2);
		Assert.assertEquals(2, d2.getId());
		
		Car c2 = new Car();
		c2.setModel("New Registration Car");
		c2.setDriverId(d2.getId());
		Database.d.newCar(c2);
		Assert.assertEquals(3, c2.getId());
		Database.d.registerCar(c2.getId(), true, true);
		
		
		List<Car> list = Database.d.getCarsForDriver(1);
		Car c1a = list.get(0);
		Car c1b = list.get(1);
		c1b.setColor("Updated Color");
		
		Database.d.registerCar(c1a.getId(), true, true); // make as paid
		Database.d.unregisterCar(c1b.getId()); // unregister
		Database.d.updateCar(c1b); // update the color

		Database.d.close();
	}

	@Test
	public void testMerge() throws Exception 
	{		
		createTestDBs();
		
		Database.openDatabaseFile(new File("testhost.db"));
		ChangeTracker tracker = new ChangeTracker("testclient");
		Database.d.mergeChanges(tracker.getChanges());
		tracker.archiveChanges();
		
		Map<Integer, Driver> drivers = Database.d.getAllDrivers();
		Assert.assertEquals("Existing", drivers.get(1).firstname);
		Assert.assertEquals("New DataEntry", drivers.get(2).firstname);
		Assert.assertEquals("New Registration", drivers.get(3).firstname); // remapped to 3
		
		Database.d.setCurrentEvent(Database.d.getEvents().get(0));
		Map<Integer, Car> cars = Database.d.getAllCars();
		
		Assert.assertEquals(cars.get(1).model, "Existing Car");
		Assert.assertEquals(cars.get(1).driverid, 1);
		Assert.assertTrue(Database.d.isRegistered(1));
		
		Assert.assertEquals(cars.get(2).model, "Car to be updated");
		Assert.assertEquals(cars.get(2).driverid, 1);
		Assert.assertEquals(cars.get(2).color, "Updated Color");
		Assert.assertFalse(Database.d.isRegistered(2));
		
		Assert.assertEquals(cars.get(3).model, "New DataEntry Car");
		Assert.assertEquals(cars.get(3).driverid, 2);
		Assert.assertFalse(Database.d.isRegistered(3));
		
		Assert.assertEquals(cars.get(4).model, "New Registration Car");  // remapped id
		Assert.assertEquals(cars.get(4).driverid, 3); // remapped driverid
		Assert.assertTrue(Database.d.isRegistered(4));
		
		Database.d.close();
	}

}
