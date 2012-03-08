/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2010 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 */
public class MergeProcess
{
	private static Logger log = Logger.getLogger(MergeProcess.class.getCanonicalName());

	public static void mergeTo(String host, String name)
	{
		Map<Integer, Integer> driveridmap = new HashMap<Integer,Integer>();
		Map<Integer, Integer> caridmap = new HashMap<Integer,Integer>();
		WebDataSource dest;
		
		try
		{
			dest = new WebDataSource(host, name);
		}
		catch (IOException ex)
		{
			log.severe("Unable to connect: " + ex);
			return;
		}

		try
		{
			dest.start();
			List<Change> changes = Database.d.getChanges();
			for (Change change : changes)
			{
				String type = change.getType();
				log.info("Merge "+type+": " + change.arg);
				if (type.equals("SETEVENT"))
				{
					Event e = (Event)change.arg;
					dest.setCurrentEvent(e);
				}
				else if (type.equals("INSERTDRIVER"))
				{
					Driver d = (Driver)change.arg;
					int usedid = d.id;
					dest.newDriver(d); // no mapping as all brand new
					driveridmap.put(usedid, d.id);
				}
				else if (type.equals("UPDATEDRIVER"))
				{
					Driver d = (Driver)change.arg;
					if (driveridmap.containsKey(d.id)) // map driverid
						d.id = driveridmap.get(d.id);
					dest.updateDriver(d);
				}
				else if (type.equals("INSERTCAR"))
				{
					Car c = (Car)change.arg;
					int usedid = c.id;
					if (driveridmap.containsKey(c.driverid)) // map driverid
						c.driverid = driveridmap.get(c.driverid);
					dest.newCar(c);
					caridmap.put(usedid, c.id);
				}
				else if (type.equals("UPDATECAR"))
				{
					Car c = (Car)change.arg;
					if (caridmap.containsKey(c.id)) // map carid and driverid
						c.id = caridmap.get(c.id);
					if (driveridmap.containsKey(c.driverid))
						c.driverid = driveridmap.get(c.driverid);
					dest.updateCar(c);
				}
				else if (type.equals("UNREGISTERCAR"))
				{
					Integer carid = (Integer)change.arg;
					if (caridmap.containsKey(carid))
						carid = caridmap.get(carid);
					dest.unregisterCar(carid);
				}
				else if (type.equals("REGISTERCAR"))
				{
					Integer carid = (Integer)change.arg;
					if (caridmap.containsKey(carid))
						carid = caridmap.get(carid);
					dest.registerCar(carid);
				}
			}

			dest.commit();
			Database.d.clearChanges();
		}
		catch (Exception e)
		{
			dest.rollback();
			log.log(Level.SEVERE, "Unable to merge: " + e, e);
			return;
		}

		/* download the merged version from the server so we are completely synced again */
		try  
		{
			File db = Database.file;
			Database.d.close();
			if (!db.delete())
				throw new IOException("Error deleting old version already on disk ("+db+")");

			dest.server.downloadDatabase(db, false);
			Database.openDatabaseFile(db);
			Database.d.trackRegChanges(true);
			Database.d.setCurrentEvent(Database.d.getCurrentEvent());
			JOptionPane.showMessageDialog(null, "Merge Complete");
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Unable to download new merged version from server:\n " + e, e);
		}
	}
}
