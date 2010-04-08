/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.io.Serializable;

public class Driver implements Serializable
{
	protected int id;
	protected String firstname;
	protected String lastname;
	protected String email;
	protected String address;
	protected String city;
	protected String state;
	protected String zip;
	protected String homephone;
	protected String workphone;
	protected String clubs;
	protected String brag;
	protected String sponsor;
	protected String membership;

	/* meta */
	public int carcount = 0;

	public Driver()
	{
	}

	public Driver(String f, String l)
	{
		firstname = f;
		lastname = l;
	}

	public String getFullName() { return firstname + " " + lastname; }
	public int getId() { return id; }
	public String getFirstName() { return firstname; }
	public String getLastName() { return lastname; }
	public String getEmail() { return email; }
	public String getAddress() { return address; }
	public String getCity() { return city; }
	public String getState() { return state; }
	public String getZip() { return zip; }
	public String getHomePhone() { return homephone; }
	public String getWorkPhone() { return workphone; }
	public String getBrag() { return brag; }
	public String getSponsor() { return sponsor; }
	public String getMembership() { return membership; }

	public void setFirstName(String s) { firstname = s; }
	public void setLastName(String s) { lastname = s; }
	public void setEmail(String s) { email = s; }
	public void setAddress(String s) { address = s; }
	public void setCity(String s) { city = s; }
	public void setState(String s) { state = s; }
	public void setZip(String s) { zip = s; }
	public void setHomePhone(String s) { homephone = s; }
	public void setWorkPhone(String s) { workphone = s; }
	public void setBrag(String s) { brag = s; }
	public void setSponsor(String s) { sponsor = s; }
	public void setMembership(String s) { membership = s; }
}

