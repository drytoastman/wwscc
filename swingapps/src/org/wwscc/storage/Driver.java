/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.wwscc.util.IdGenerator;

public class Driver implements Serializable, Comparable<Driver>
{
	private static final long serialVersionUID = -5960219850370470938L;
	
	protected UUID driverid;
	protected String firstname;
	protected String lastname;
	protected String alias;
	protected String email;
	protected String address;
	protected String city;
	protected String state;
	protected String zip;
	protected String phone;
	protected String brag;
	protected String sponsor;
	protected String membership;
	private Map<String, String> extras;

	/* meta */
	public int carcount = 0;

	public Driver()
	{
		extras = new HashMap<String,String>();
	}

	public Driver(String f, String l)
	{
		driverid = IdGenerator.generateId();
		firstname = f;
		lastname = l;
		alias = "";
		email = "";
		address = "";
		city = "";
		state = "";
		zip = "";
		phone = "";
		brag = "";
		sponsor = "";
		membership = "";
		extras = new HashMap<String,String>();
	}

	public String getFullName() { return firstname + " " + lastname; }
	public UUID getDriverId() { return driverid; }
	public String getFirstName() { return firstname; }
	public String getLastName() { return lastname; }
	public String getEmail() { return email; }
	public String getAddress() { return address; }
	public String getCity() { return city; }
	public String getState() { return state; }
	public String getZip() { return zip; }
	public String getPhone() { return phone; }
	public String getBrag() { return brag; }
	public String getSponsor() { return sponsor; }
	public String getAlias() { return alias; }
	public String getMembership() { return membership; }
	public String getExtra(String name) 
	{ 
		String ret = extras.get(name); 
		if (ret == null)
			return "";
		return ret;
	}
	public Set<String> getExtraKeys()
	{
		return extras.keySet();
	}

	public void setFirstName(String s) { firstname = s; }
	public void setLastName(String s) { lastname = s; }
	public void setEmail(String s) { email = s; }
	public void setAddress(String s) { address = s; }
	public void setCity(String s) { city = s; }
	public void setState(String s) { state = s; }
	public void setZip(String s) { zip = s; }
	public void setPhone(String s) { phone = s; }
	public void setBrag(String s) { brag = s; }
	public void setSponsor(String s) { sponsor = s; }
	public void setAlias(String s) { alias = s; }
	public void setMembership(String s) { membership = s; }
	public void setExtra(String name, String val) 
	{ 
		if ((val == null) || (val.trim().length() == 0))
			extras.remove(name);
		else
			extras.put(name, val);
	}
	
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

	@Override
	public int compareTo(Driver d)
	{
		return (firstname + lastname).toLowerCase().compareTo((d.firstname + d.lastname).toLowerCase());
	}
	
	public String toString()
	{
		return firstname + " " + lastname;
	}
	
}

