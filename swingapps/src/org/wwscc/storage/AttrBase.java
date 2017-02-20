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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@SuppressWarnings("unchecked")
public class AttrBase
{
	protected JSONObject attr;

	public AttrBase()
	{
		attr = new JSONObject();
	}

	public void loadAttr(ResultSet rs) throws SQLException
	{
		try {
			attr = (JSONObject)new JSONParser().parse(rs.getString("attr"));
		} catch (ParseException e) {
			attr = new JSONObject();
		}
	}
	
	public String getAttrS(String name) 
	{ 
		String ret = (String)attr.get(name); 
		if (ret == null)
			return "";
		return ret;
	}
	
	public double getAttrD(String name)
	{
		Double ret = (Double)attr.get(name);
		if (ret == null)
			return -1.0;
		else
			return ret;
	}

	public boolean getAttrB(String name)
	{
		Boolean ret = (Boolean)attr.get(name);
		if (ret == null)
			return false;
		else
			return ret;
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

