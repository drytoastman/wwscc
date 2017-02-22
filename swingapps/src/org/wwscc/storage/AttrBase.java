/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2017 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@SuppressWarnings("unchecked")
public class AttrBase
{
	private static Logger log = Logger.getLogger(AttrBase.class.getCanonicalName());
	
	protected JSONObject attr;

	public AttrBase()
	{
		attr = new JSONObject();
	}
	
	public AttrBase(JSONObject o)
	{
		attr = (JSONObject)o.clone();
	}

	public AttrBase(ResultSet rs) throws SQLException
	{
		try {
			attr = (JSONObject)new JSONParser().parse(rs.getString("attr"));
		} catch (ParseException e) {
			attr = new JSONObject();
		}
	}
	
	/**
	 * Try and purge unnecessary keys that have no information from the database
	 */
	public void attrCleanup()
	{
		for (Object key : attr.keySet())
		{
			Object val = attr.get(key);
			if (    (val == null)
				|| ((val instanceof String)  && (val.equals("")))
				|| ((val instanceof Integer) && ((Integer)val == 0))
				|| ((val instanceof Double)  && ((Double)val <= 0.0))
				|| ((val instanceof Boolean) && (!(Boolean)val))
			   ) {
				attr.remove(key);
			}
		}
	}
	
	public String getAttrS(String name) 
	{ 
		String ret = null;
		try {
			ret = (String)attr.get(name);
			if (ret != null)
				return ret;
		} catch (Exception e) {
			log.info(String.format("Failed to load string named %s from %s: %s", name, ret, e)); 
		}
		return "";
	}
	
	public double getAttrD(String name)
	{
		Double ret = null;
		try {
			ret = (Double)attr.get(name);
			if (ret != null)
				return ret;
		} catch (Exception e) {
			log.info(String.format("Failed to load double named %s from %s: %s", name, ret, e)); 
		}
		return -1.0;
	}

	public boolean getAttrB(String name)
	{
		Boolean ret = null;
		try {
			ret = (Boolean)attr.get(name);
			if (ret != null)
				return ret;
		} catch (Exception e) {
			log.info(String.format("Failed to load boolean named %s from %s: %s", name, ret, e)); 
		}
		
		return false;
	}
	
	public Set<String> getAttrKeys()
	{
		return (Set<String>)attr.keySet();
	}

	public void setAttrS(String name, String val) 
	{ 
		if ((val == null) || (val.trim().length() == 0))
			attr.remove(name);
		else
			attr.put(name, val);
	}
	
	public void setAttrD(String name, Double val)
	{
		if ((val == null) || (val <= 0.0))
			attr.remove(name);
		else
			attr.put(name, val);
	}

	public void setAttrB(String name, Boolean val)
	{
		if (val == null)
			attr.remove(name);
		else
			attr.put(name, val);
	}
}

