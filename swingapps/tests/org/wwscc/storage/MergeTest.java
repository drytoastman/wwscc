package org.wwscc.storage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;

public class MergeTest 
{
	File testdir;
	@Before
	public void setUp() throws Exception 
	{
		testdir = new File(System.getProperty("user.dir"), "tests/org/wwscc/storage");
	}

	@After
	public void tearDown() throws Exception 
	{
	}

	//@Test
	public void testMergeTo() throws Exception 
	{
		// TODO, stop using static database, need to keep updating it with schema changes and then the serialID bit me...
		System.out.println(testdir);
		Files.copy(Paths.get(testdir.getPath(), "mergesrc.db"), Paths.get("testsrc.db"), StandardCopyOption.REPLACE_EXISTING);
		Files.copy(Paths.get(testdir.getPath(), "mergedst.db"), Paths.get("testdst.db"), StandardCopyOption.REPLACE_EXISTING);
		Database.openDatabaseFile(new File("testsrc.db"));
		MergeProcess.mergeToInternal(new SqliteDatabase(new File("testdst.db")));
		// check data here
		Database.openDatabaseFile(new File("testdst.db"));
		
		Map<Integer, Driver> drivers = Database.d.getAllDrivers();
		Assert.assertEquals(drivers.get(6).lastname, "FiveX");
		Assert.assertEquals(drivers.get(7).lastname, "Six");
		
		Map<Integer, Car> cars = Database.d.getAllCars();
		Assert.assertEquals(cars.get(6).number, 55);
		Assert.assertEquals(cars.get(6).driverid, 6);
		Assert.assertEquals(cars.get(7).number, 66);
		Assert.assertEquals(cars.get(7).driverid, 7);
		Assert.assertEquals(cars.get(8).number, 222);
		Assert.assertEquals(cars.get(8).driverid, 2);
		
		Database.d.setCurrentEvent(Database.d.getEvents().get(0));
		Assert.assertTrue(Database.d.isRegistered(1));
		Assert.assertTrue(Database.d.isRegistered(2));
		Assert.assertTrue(Database.d.isRegistered(3));
		Assert.assertFalse(Database.d.isRegistered(4));
		Assert.assertFalse(Database.d.isRegistered(5));
		Assert.assertTrue(Database.d.isRegistered(6));
		Assert.assertTrue(Database.d.isRegistered(7));
		Assert.assertTrue(Database.d.isRegistered(8));
	}

}
