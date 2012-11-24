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

/**
 */
public class Dialins 
{
	//private static Logger log = Logger.getLogger(Dialins.class.getCanonicalName());

	//protected Map <String, Double> leaderNet;
	protected Map <Integer, Double> classDial;
	protected Map <Integer, Double> bonusDial;
	protected Map <Integer, Double> netTime;
	protected Map <Integer, Double> classDiff;

	protected List<Integer> netOrder;
	protected List<Integer> diffOrder;
	
	public Dialins()
	{
		//leaderNet = new HashMap<String, Double>();
		classDial = new HashMap<Integer, Double>();
		bonusDial = new HashMap<Integer, Double>();
		netTime = new HashMap<Integer, Double>();
		classDiff = new HashMap<Integer, Double>();

		netOrder = new ArrayList<Integer>();
		diffOrder = new ArrayList<Integer>();
	}

	public double getNet(int carid) { return netTime.get(carid); }
	public double getDiff(int carid) { return classDiff.get(carid); }
	public double getDial(int carid, boolean bonus)
	{
		double ret;
		if (bonus)
			ret = bonusDial.get(carid);
		else
			ret = classDial.get(carid);
		
		return (Math.round(ret * 1000.0))/1000.0;
	}

	public List<Integer> getNetOrder()
	{
		return mapSort(netTime);
	}

	public List<Integer> getDiffOrder()
	{
		return mapSort(classDiff);
	}

	public List<Integer> mapSort(Map<Integer, Double> map)
	{
		List<Map.Entry<Integer,Double>> torder = new LinkedList<Map.Entry<Integer,Double>>(map.entrySet());
		Collections.sort(torder,  new Comparator<Map.Entry<Integer,Double>>() {
			public int compare(Map.Entry<Integer,Double> o1, Map.Entry<Integer,Double> o2) {
				return (o1.getValue().compareTo(o2.getValue()));
			}
		});

		List<Integer> ids = new ArrayList<Integer>();
		for (Map.Entry<Integer,Double> e : torder)
			ids.add(e.getKey());

		return ids;
	}

}
