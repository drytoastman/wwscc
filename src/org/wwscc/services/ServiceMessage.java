/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wwscc.services;

import java.io.IOException;
import java.util.logging.Logger;

/**
 *
 * @author bwilson
 */
public class ServiceMessage 
{
	private static Logger log = Logger.getLogger(ServiceMessage.class.getCanonicalName());
	
	private String service;
	private String id;
	private int port;
	
	public ServiceMessage()
	{
		this.service = "";
		this.id = "";
		this.port = 0;
	}
	
	public static ServiceMessage createType(String service, String id, int port)
	{
		ServiceMessage ret = new ServiceMessage();
		ret.service = service;
		ret.id = id;
		ret.port = port;
		if (ret.port == 0)
			log.warning("Creating a service type with port == 0");
		return ret;
	}
	
	public static ServiceMessage createRequest(String service)
	{
		ServiceMessage ret = new ServiceMessage();
		ret.service = service;
		ret.id = "";
		ret.port = 0;
		return ret;
	}

	public static ServiceMessage decodeMessage(String data) throws IOException
	{
		String parts[] = data.split(",");
		ServiceMessage ret = new ServiceMessage();

		try {	
			ret.service = parts[0];
			ret.id = parts[1];
			ret.port = Integer.parseInt(parts[2]);
		} catch (Exception e) {
			throw new IOException(e);
		}
		
		return ret;
	}
	
	public String encode()
	{
		return String.format("%s,%s,%s", service, id, port);
	}
	
	public String toString()
	{
		return String.format("%s => %s,%s,%s", (port==0)?"REQUEST":"REPLY", service, id, port);
	}
	
	public boolean isRequest()
	{
		return (port == 0);
	}
	
	public boolean isAnnouncement()
	{
		return (port != 0);
	}

	public String getService()
	{
		return service;
	}
	
	public String getId()
	{
		return id;
	}
	
	public int getPort()
	{
		return port;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o instanceof ServiceMessage)
		{
			ServiceMessage m = (ServiceMessage)o;
			return (m.service.equals(service)) && (m.id.equals(id)) && (m.port == port);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 71 * hash + (this.service != null ? this.service.hashCode() : 0);
		hash = 71 * hash + (this.id != null ? this.id.hashCode() : 0);
		hash = 71 * hash + this.port;
		return hash;
	}
}
