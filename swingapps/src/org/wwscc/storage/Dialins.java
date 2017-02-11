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
	//private static Logger log = Logger.getLogger(Dialins.class.getCanonicalName());

	//protected Map <String, Double> leaderNet;
	protected Map <UUID, Double> classDial;
	protected Map <UUID, Double> bonusDial;
	protected Map <UUID, Double> netTime;
	protected Map <UUID, Double> classDiff;

	//protected List<Integer> netOrder;
	//protected List<Integer> diffOrder;
	
	public Dialins()
	{
		//leaderNet = new HashMap<String, Double>();
		classDial = new HashMap<UUID, Double>();
		bonusDial = new HashMap<UUID, Double>();
		netTime = new HashMap<UUID, Double>();
		classDiff = new HashMap<UUID, Double>();

		//netOrder = new ArrayList<Integer>();
		//diffOrder = new ArrayList<Integer>();
	}

	public double getNet(UUID carid) { return netTime.get(carid); }
	public double getDiff(UUID carid) { return classDiff.get(carid); }
	public double getDial(UUID carid, boolean bonus)
	{
		double ret;
		if (bonus)
			ret = bonusDial.get(carid);
		else
			ret = classDial.get(carid);
		
		return (Math.round(ret * 1000.0))/1000.0;
	}

	public List<UUID> getNetOrder()
	{
		return mapSort(netTime);
	}

	public List<UUID> getDiffOrder()
	{
		return mapSort(classDiff);
	}

	public List<UUID> mapSort(Map<UUID, Double> map)
	{
		List<Map.Entry<UUID,Double>> torder = new LinkedList<Map.Entry<UUID,Double>>(map.entrySet());
		Collections.sort(torder,  new Comparator<Map.Entry<UUID,Double>>() {
			public int compare(Map.Entry<UUID,Double> o1, Map.Entry<UUID,Double> o2) {
				return (o1.getValue().compareTo(o2.getValue()));
			}
		});

		List<UUID> ids = new ArrayList<UUID>();
		for (Map.Entry<UUID,Double> e : torder)
			ids.add(e.getKey());

		return ids;
	}

}
