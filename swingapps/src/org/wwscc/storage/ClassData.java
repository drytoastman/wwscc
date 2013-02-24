/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClassData
{
	private static Logger log = Logger.getLogger(ClassData.class.getCanonicalName());

	HashMap <String, ClassData.Class> classes;
	HashMap <String, ClassData.Index> indexes;
	
	public ClassData()
	{
		classes = new HashMap<String, ClassData.Class>();
		indexes = new HashMap<String, ClassData.Index>();
	}

	protected void add(ClassData.Class c)
	{
		classes.put(c.getCode(), c);
	}

	protected void add(ClassData.Index i)
	{
		indexes.put(i.getCode(), i);
	}

	public ClassData.Class getClass(String code)
	{
		return classes.get(code);
	}

	public ClassData.Index getIndex(String code)
	{
		return indexes.get(code);
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

	
	public double getEffectiveIndex(String classcode, String indexcode, boolean tireindexed)
	{
		double indexVal = 1.0;
		try
		{
			ClassData.Class classData = getClass(classcode);
			ClassData.Index indexData;

			if (classData == null)
				throw new Exception("Invalid class: " + classcode);

			/* Apply car index */
			if (classData.carindexed)
			{
				if ((indexData = getIndex(indexcode)) != null)
					indexVal *= indexData.getValue();
			}
			
			/* Apply class index (linked to index tables) */
			if (!classData.classindex.equals(""))
			{
				if ((indexData = getIndex(classData.classindex)) != null)
					indexVal *= indexData.getValue();
			}

			/* Apply special class multiplier (only < 1.000 for Tire class at this point) */
			if (classData.classmultiplier < 1.0 && (!classData.usecarflag || tireindexed))
				indexVal *= classData.classmultiplier;
		}
		catch (Exception ioe)
		{
			log.log(Level.WARNING, "getEffectiveIndex failed: " + ioe, ioe);
		}

		return indexVal;
	}

	public String getIndexStr(String classcode, String indexcode, boolean tireindexed)
	{
        String indexstr = indexcode;
        try
        {
			ClassData.Class cls = getClass(classcode);
			if (cls == null)
				throw new Exception("Invalid class: " + classcode);

            if (!cls.classindex.equals(""))
                indexstr = cls.classindex;

            if (cls.classmultiplier < 1.000 && (!cls.usecarflag || tireindexed))
                indexstr = indexstr + "*";
        }
        catch (Exception e)
        {
        }
        
        if (indexstr.equals(""))
        	return indexstr;
        return String.format("(%s)", indexstr);
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
		protected boolean usecarflag;
		protected String caridxrestrict;
		
		private List<String> _restricted;
		private boolean _inverse;

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
		
		public boolean useCarFlag() {
			return usecarflag;
		}
		
		private void parseRestricted() 
		{
			if (caridxrestrict.trim().equals(""))
			{
				_restricted = new ArrayList<String>();
				_inverse = true;
			}
			else if (caridxrestrict.charAt(0) == '!')
			{
				_inverse = true;
				_restricted = Arrays.asList(caridxrestrict.substring(1).split(","));
			}
			else
			{
				_inverse = false;
				_restricted = Arrays.asList(caridxrestrict.split(","));
			}
		}
			
		public List<String> restrictedIndexes () {
			if (_restricted == null) parseRestricted();
			return _restricted;
		}
		
		public boolean restrictedInverted() {
			if (_restricted == null) parseRestricted();
			return _inverse;
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
