/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2013 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.io.Serializable;

public class Change implements Serializable
{
	static final long serialVersionUID = -377835109543815997L;
	
	 protected String sqlmap;
	 protected Serializable args[];
	 
	 public Change(String t, Serializable a[]) 
	 { 
		 sqlmap = t; 
		 args = a; 
	 }
	 
	 public String getType() 
	 { 
		 return sqlmap; 
	 }
	 
	 public Serializable[] getArgs() 
	 { 
		 return args; 
	 }
}
