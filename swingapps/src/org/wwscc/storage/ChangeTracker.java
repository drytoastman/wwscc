package org.wwscc.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChangeTracker
{
	private static Logger log = Logger.getLogger(ChangeTracker.class.getCanonicalName());
	
	protected boolean tracking = false;
	protected SQLDataInterface dataSource;
	protected FileWriter audit;
	
	/**
	 * Create a new change tracker
	 * @param db a link to a SQLDataInterface for performing executeSelect and executeUpdate
	 * @throws IOException 
	 */
	public ChangeTracker(SQLDataInterface db)
	{
		dataSource = db;
	}
	
	/**
	 * @return true if we are currently tracking changes
	 */
	public boolean isTracking()
	{
		return tracking;
	}
	
	/**
	 * User requests that we start/stop tracking
	 * @param track true if tracking should be occuring
	 */
	public void trackRegChanges(boolean track)
	{
		tracking = track;
		
		try {
			if (tracking && audit == null)
				audit = new FileWriter("changesaudit.log", true);
			if (!tracking && audit != null)
				audit.close();
		} catch (IOException ioe) {
			log.log(Level.WARNING, "Error opening/closing audit log: {0}", ioe);
		}
	}

	/**
	 * @param o an object to serialize
	 * @return a byte array of serialized data
	 * @throws IOException
	 */
	byte[] serialize(Serializable o) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream writer = new ObjectOutputStream(out);
		writer.writeObject(o);
		return out.toByteArray();
	}

	/**
	 * 
	 * @param b a byte array of serialized data
	 * @return the object that was serialized
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	Object unserialize(byte[] b) throws IOException, ClassNotFoundException
	{
		ByteArrayInputStream out = new ByteArrayInputStream(b);
		ObjectInputStream reader = new ObjectInputStream(out);
		Object o = reader.readObject();
		return o;
	}
	
	/**
	 * Record the change for later use such as merging.
	 * @param type the key for the change type
	 * @param o the serializable object used
	 */
	public void trackChange(String type, Serializable o)
	{
		if (!tracking)
			return;
		try{
			audit.write(String.format("%s, %s\n", type, o));
			audit.flush();
			dataSource.executeUpdate("TRACK", SQLDataInterface.newList(type, serialize(o)));
		} catch (IOException ioe) {
			log.log(Level.WARNING, "Failed to track the last change to the database: {0}", ioe.getMessage());
		}
	}
	
	
	/**
	 * Get the list of changes that we've recorded so far
	 * @return a List of Change objects, could be empty if nothing recorded
	 */
	public List<Change> getChanges()
	{
		try
		{
			SQLDataInterface.ResultData data = dataSource.executeSelect("GETCHANGES", null);
			List<Change> ret = new ArrayList<Change>();
			for (SQLDataInterface.ResultRow r : data)
			{
				Change c = new Change();
				c.id = r.getInt("id");
				c.type = r.getString("type");
				c.arg = unserialize(r.getBlob("args"));
				ret.add(c);
			}
			return ret;
		}
		catch (Exception ioe)
		{
			log.log(Level.WARNING, "Failed to get the list of changes from the database: {0}", ioe.getMessage());
			return null;
		}
	}
	

	/**
	 * Clear all the changes we have recorded.
	 */
	public void clearChanges()
	{
		try {
			audit.write("===========================\nCHANGES CLEARED\n===========================\n");
			audit.flush();
			dataSource.executeUpdate("TRACKCLEAR", null);
		} catch (Exception ioe) {
			log.log(Level.WARNING, "Failed to clear changes from the database: {0}", ioe.getMessage());
		}
	}
	
	/**
	 * @return a count of the current changes
	 */
	public int countChanges()
	{
		try {
			SQLDataInterface.ResultData data = dataSource.executeSelect("TRACKCOUNT", null);
			if (data.size() > 0)
				return data.get(0).getInt("count(*)");
		} catch (Exception ioe) {
			log.log(Level.INFO, "Unabled to count database changes: {0}", ioe.getMessage());
		}
		return -1;
	}
}
