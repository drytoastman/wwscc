package org.wwscc.storage;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BarcodeLookup
{
	private static final Logger log = Logger.getLogger(BarcodeLookup.class.getCanonicalName());
	
	public static class LookupException extends IOException 
	{
		public LookupException(String message) 
		{
			super(message);
		}
	}
	
	/**
	 * Given a barcode try and find the matching Driver or Car for the id and return an associated object.
	 * If the barcode starts with 'D', it is assumed that the remaining digits are a driver id
	 * If the barcode starts with 'C', it is assumed that the remaining digits are a car id
	 * Otherwise, the barcode is treated as a membership value.
	 * @param barcode the barcode string
	 * @return an Entrant object for a matching car id, a Driver for a matching driver id or membership, or null for unmatched membership
	 * @throws LookupException 
	 */
	public static Object findObjectByBarcode(String barcode) throws LookupException
	{
		/** FINISH ME, what about quick entry for cars or drivers, UUID is too long
		if (barcode.startsWith("D"))
		{
			Driver driverbyid = Database.d.getDriver(UUID.fromString(barcode.substring(1)));
			if (driverbyid == null)
				throw new LookupException("Unable to located a driver with id=" + barcode.substring(1));
			return driverbyid;
		}
		
		if (barcode.startsWith("C"))
		{
			int carid = Integer.parseInt(barcode.substring(1));
			Entrant entrant = Database.d.loadEntrant(carid, false);
			if (entrant == null)
				throw new LookupException("Unable to find a car with id=" + carid);
			return entrant;
		}
		*/
		if (barcode.length() < 4)
			throw new LookupException(barcode + " is too short to be a valid membership value, ignoring");
		
		List<Driver> found = Database.d.findDriverByMembership(barcode);
		if (found.size() == 0)
		{
			log.log(Level.INFO, "Unable to locate a driver using barcode {0}", barcode);
			return null;
		}
		
		if (found.size() > 1)
			log.log(Level.WARNING, "{0} drivers exist with the membership value {1}, using the first", new Object[] {found.size(), barcode});
		
		return found.get(0);
	}
}
