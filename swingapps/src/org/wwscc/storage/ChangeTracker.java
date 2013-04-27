package org.wwscc.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.wwscc.util.Logging;

public class ChangeTracker
{
	private static Logger log = Logger.getLogger(ChangeTracker.class.getCanonicalName());
	
	private List<Change> changes;
	private String dbname;
	
	/**
	 * Create a new change tracker
	 * @param db the string name of the database we are tracking, used to determine file name
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws ClassNotFoundException 
	 * @throws Exception 
	 */
	public ChangeTracker(String db) throws FileNotFoundException, IOException, ClassNotFoundException
	{
		dbname = db;
		changes = readInFile(generateFileName(0));
	}

	/**
	 * @return a count of the current changes
	 */
	public int size()
	{
		return changes.size();
	}
	
	
	// #### public static methods ####
	
	/**
	 * Read a file containing serialized version of a List of changes.
	 * @param f the file to read from
	 * @return a list of changes, empty if the file doesn't exist yet
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked")
	public static List<Change> readInFile(File f) throws FileNotFoundException, IOException, ClassNotFoundException
	{
		List<Change> ret = null; 
		if (f.exists())
		{
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(f));;
			ret = (List<Change>) in.readObject();
			in.close();
		}
		
		if (ret == null)
			ret = new ArrayList<Change>();
		return ret;
	}
	
	
	// ### Package private methods ###
	
	/**
	 * Record the change for later use such as merging.
	 * @param type the key for the change type
	 * @param o the serializable object used
	 */
	void trackChange(String type, Serializable o[])
	{
		changes.add(new Change(type, o));
		syncToFile(); // not efficient but simple, easy to understand, and files are small enough that its a non-issue
	}
	

	/**
	 * Rotates the current list of changeset files. Package private.
	 * @throws FileNotFoundException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	void archiveChanges() throws FileNotFoundException, ClassNotFoundException, IOException
	{
		syncToFile(); // do I need this?
		changes.clear();
		rotate();
	}
	
	
	/**
	 * Get the list of changes that we've recorded so far.  Package private.
	 * @return a List of Change objects, could be empty if nothing recorded
	 */
	List<Change> getChanges()
	{
		return changes;
	}
	

	// #### Private methods ####
	 
	private File generateFileName(int counter)
	{
		if (counter > 0)
			return new File(Logging.getLogDir(), dbname + "changes.log." + counter);
		else
			return new File(Logging.getLogDir(), dbname + "changes.log");
	}

	
	private void syncToFile()
	{
		if (changes.isEmpty())
			return;
		
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(generateFileName(0)));
			out.writeObject(changes);
			out.close();
		} catch (IOException ioe) {
			log.log(Level.WARNING, "Failed to sync changes to disk: " + ioe.getMessage(), ioe);
		}
	}
	
	private void rotate()
	{
		// rotate
		File files[] = new File[10];
		for (int ii = 0; ii < files.length; ii++)
			files[ii] = generateFileName(ii);

		for (int ii = files.length - 2; ii >= 0; ii--)
		{
			File f1 = files[ii];
			File f2 = files[ii + 1];
			if (f1.exists())
			{
				if (f2.exists())
					f2.delete();
				f1.renameTo(f2);
			}
		}
	}

}
