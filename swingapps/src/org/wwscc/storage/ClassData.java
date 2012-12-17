/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class ClassData
{
	//private static Logger log = Logger.getLogger("org.wwscc.storage.ClassData");

	HashMap <String, ClassData.Class> classes;
	HashMap <String, ClassData.Index> indexes;
	double globaltireindex;
	
	public ClassData()
	{
		classes = new HashMap<String, ClassData.Class>();
		indexes = new HashMap<String, ClassData.Index>();
		globaltireindex = 1.0;
	}

	protected void add(ClassData.Class c)
	{
		classes.put(c.getCode(), c);
	}

	protected void add(ClassData.Index i)
	{
		indexes.put(i.getCode(), i);
	}
	
	protected void setGlobalTireIndex(double d)
	{
		globaltireindex = d;
	}

	public ClassData.Class getClass(String code)
	{
		return classes.get(code);
	}

	public ClassData.Index getIndex(String code)
	{
		return indexes.get(code);
	}
	
	public double getGlobalTireIndex()
	{
		return globaltireindex;
	}

	public ArrayList<ClassData.Class> getClasses()
	{
		return new ArrayList<ClassData.Class>(classes.values());
	}

	public ArrayList<ClassData.Index> getIndexes()
	{
		return new ArrayList<ClassData.Index>(indexes.values());
	}

	public ArrayList<String> getClassCodes()
	{
		ArrayList<String> result = new ArrayList<String>();
		for (ClassData.Class c : classes.values())
		{
			result.add(c.getCode());
		}
		return result;
	}

	public ArrayList<String> getIndexCodes()
	{
		ArrayList<String> result = new ArrayList<String>();
		for (ClassData.Index c : indexes.values())
		{
			result.add(c.getCode());
		}
		return result;
	}


	/***********************************************************************/
	/* Class */
	public static class Class
	{
		protected String code;
		protected String descrip;
		protected boolean carindexed;
		protected String classindex;
		protected double classmultiplier;
		protected boolean eventtrophy;
		protected boolean champtrophy;
		protected int numorder;
		protected int countedruns;

		public Class()
		{
		}

		public String toString() {
			return code;
		}

		public String getCode() {
			return code;
		}

		public String getDescrip() {
			return descrip;
		}

		public int getCountedRuns()
		{
			if (countedruns <= 0)
				return Integer.MAX_VALUE;
			else
				return countedruns;
		}
		
		public boolean carsNeedIndex() {
			return carindexed;
		}

		static public class StringOrder implements Comparator<ClassData.Class>
		{
		    public int compare(ClassData.Class c1, ClassData.Class c2)
			{
				return c1.getCode().compareTo(c2.getCode());
			}
		}
	}


	/***********************************************************************/
	/* Index */
	public static class Index
	{
		protected String code;
		protected String descrip;
		protected double value;

		public Index()
		{
		}

		public String toString() {
			return code;
		}

		public String getCode() {
			return code;
		}

		public String getDescrip() {
			return descrip;
		}

		public double getValue() {
			return value;
		}

		static public class StringOrder implements Comparator<ClassData.Index>
		{
		    public int compare(ClassData.Index c1, ClassData.Index c2)
			{
				return c1.getCode().compareTo(c2.getCode());
			}
		}

		static public class ValueOrder implements Comparator<ClassData.Index>
		{
		    public int compare(ClassData.Index c1, ClassData.Index c2)
			{
				return (int)(c1.getValue()*1000 - c2.getValue()*1000);
			}
		}
	}
}
