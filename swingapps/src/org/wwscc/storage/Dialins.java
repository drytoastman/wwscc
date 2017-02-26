/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 */
public class Dialins 
{
	private class CarInfo
	{
		UUID   carid;
		String classcode;
		Double index;
		Double net;
		Double bonus;
		Double dial;
	}
	
	private class Leader
	{
		double net;   // net time of class leader
		double basis; // leader dialin * leader index for other dialin calculations
		public Leader() { net = basis = 999.999; }
	}

	private Map <String, Leader> classmap;   // used to determine class dialin basis
	private Map <UUID, CarInfo> carmap;     // map from carid to details for the Car
	
	public Dialins()
	{
		classmap = new HashMap<String, Leader>();
		carmap   = new HashMap<UUID, CarInfo>();
	}
	
	public void setEntrant(UUID carid, String classcode, double raw, double net, double index)
	{
		CarInfo c = new CarInfo();
		carmap.put(carid, c);
		c.carid     = carid;
		c.classcode = classcode;
		c.index     = index;
		c.net       = net;
		c.bonus     = raw/2.0;
		c.dial      = 999.999;
		
		if (!classmap.containsKey(c.classcode))
			classmap.put(c.classcode, new Leader());
		
		Leader l = classmap.get(c.classcode);
		if (c.net < l.net)  // new leader
		{
			l.net   = c.net;
			l.basis = c.bonus*index;
		}
	}
	
	public void finalize()
	{
		for (CarInfo info : carmap.values())
		{
			Leader l = classmap.get(info.classcode);
			info.dial = l.basis / info.index;
		}
	}
	
	public double getNet(UUID carid)  { return carmap.get(carid).net; }
	public double getDial(UUID carid, boolean bonus)
	{
		double ret;
		if (bonus)
			ret = carmap.get(carid).bonus;
		else
			ret = carmap.get(carid).dial;
		
		return (Math.round(ret * 1000.0))/1000.0;
	}

	public List<UUID> getNetOrder()
	{
		return mapSort(
			new Comparator<CarInfo>() {
				public int compare(CarInfo o1, CarInfo o2) {
					return (o1.net.compareTo(o2.net));
				}
			});
	}


	private List<UUID> mapSort(Comparator<CarInfo> compare)
	{
		List<CarInfo> torder = new LinkedList<CarInfo>(carmap.values());
		Collections.sort(torder, compare);

		List<UUID> ids = new ArrayList<UUID>();
		for (CarInfo i : torder)
			ids.add(i.carid);
		
		return ids;
	}
}
