package org.wwscc.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Parse, holds and represents the array of points provided based on position.
 */
public class PositionPoints 
{
	private static Logger log = Logger.getLogger(PositionPoints.class.getCanonicalName());
	List<Integer> points;
	
	public PositionPoints(String data)
	{
		points = new ArrayList<Integer>();
		for (String s : data.split(","))
		{
			try {
				points.add(Integer.valueOf(s));
			} catch (NumberFormatException nfe) {
				log.warning("Failed to read pospointlist from settings properly: " + nfe);
			}
		}
	}
	
	/**
	 * Get the points value based on position.  Last value in points array is 
	 * used to fill out anything past the length of the array
	 * @param position the position to lookup
	 * @return an integer value for the points based on position
	 */
	public int get(int position)
	{
		if (position <= points.size())
			return points.get(position-1);
		return points.get(points.size()-1);
	}
	
}
