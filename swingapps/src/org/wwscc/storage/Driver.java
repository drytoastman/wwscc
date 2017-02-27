/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.UUID;
import org.wwscc.util.IdGenerator;

public class Driver extends AttrBase //implements Comparable<Driver>
{
	protected UUID driverid;
	protected String firstname;
	protected String lastname;
	protected String email;
	protected String password;
	protected String membership;

	public Driver()
	{
		super();
		driverid = IdGenerator.generateId();
		firstname = "";
		lastname = "";
		email = "";
		password = "";
		membership = "";
	}

	public Driver(String f, String l)
	{
		this();
		firstname = f;
		lastname = l;
	}
	
	public Driver(ResultSet rs) throws SQLException
	{
		super(rs);
		driverid   = (UUID)rs.getObject("driverid");
		firstname  = rs.getString("firstname");
		lastname   = rs.getString("lastname");
		email      = rs.getString("email");
		password   = rs.getString("password");
		membership = rs.getString("membership");
	}
	
	public LinkedList<Object> getValues()
	{
		LinkedList<Object> ret = new LinkedList<Object>();
		ret.add(driverid);
		ret.add(firstname);
		ret.add(lastname);
		ret.add(email);
		ret.add(password);
		ret.add(membership);
		attrCleanup();
		ret.add(attr);
		return ret;
	}

	public String getFullName()   { return firstname + " " + lastname; }
	public UUID   getDriverId()   { return driverid; }
	public String getFirstName()  { return firstname; }
	public String getLastName()   { return lastname; }
	public String getEmail()      { return email; }
	public String getMembership() { return membership; }
	public String getAddress()    { return getAttrS("address"); }
	public String getCity()       { return getAttrS("city"); }
	public String getState()      { return getAttrS("state"); }
	public String getZip()        { return getAttrS("zip"); }
	public String getPhone()      { return getAttrS("phone"); }
	public String getBrag()       { return getAttrS("brag"); }
	public String getSponsor()    { return getAttrS("sponsor"); }
	public String getAlias()      { return getAttrS("alias"); }
	
	public void setFirstName(String s)  { firstname = s; }
	public void setLastName(String s)   { lastname = s; }
	public void setEmail(String s)      { email = s; }
	public void setMembership(String s) { membership = s; }
	public void setAddress(String s)    { setAttrS("address", s); }
	public void setCity(String s)       { setAttrS("city", s); }
	public void setState(String s)      { setAttrS("state", s); }
	public void setZip(String s)        { setAttrS("zip", s); }
	public void setPhone(String s)      { setAttrS("phone", s); }
	public void setBrag(String s)       { setAttrS("brag", s); }
	public void setSponsor(String s)    { setAttrS("sponsor", s); }
	public void setAlias(String s)      { setAttrS("alias", s); }
	
	@Override
	public boolean equals(Object o)
	{
		return ((o instanceof Driver) && ((Driver)o).driverid == driverid);
	}

	@Override
	public int hashCode()
	{
		return driverid.hashCode();
	}

	/*
	@Override
	public int compareTo(Driver d)
	{
		return (firstname + lastname).toLowerCase().compareTo((d.firstname + d.lastname).toLowerCase());
	} */
	
	public String toString()
	{
		return firstname + " " + lastname;
	}
}

