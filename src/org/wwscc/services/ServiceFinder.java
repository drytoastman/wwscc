/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.services;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
public class ServiceFinder implements Runnable
{
	private static Logger log = Logger.getLogger(ServiceFinder.class.getCanonicalName());
	
	ServiceFinderListener listener;
	protected boolean done = false;
	protected MulticastSocket sock = null;
	protected InetAddress group = null;
	protected List<String> serviceNames;
	protected HashSet<FoundService> found;

	public interface ServiceFinderListener
	{
		public void newService(FoundService service);
	}
	
	public ServiceFinder(String name) throws IOException
	{
		this(Arrays.asList(new String[] { name }));
	}
	
	public ServiceFinder(List<String> names) throws IOException 
	{
		listener = null;
		done = false;
		found = new HashSet<FoundService>();
		serviceNames = names;
		group = InetAddress.getByName(ServiceAnnouncer.MDNSAddr);
		sock = new MulticastSocket(ServiceAnnouncer.MDNSPortPlus);
		sock.joinGroup(group);		
	}
	
	public void setListener(ServiceFinderListener lis)
	{
		listener = lis;
		for (FoundService service : found)
			listener.newService(service);
	}
	
	public void stop()
	{
		done = true;
	}

	@Override
	public void run()
	{	
		// for requests
		String msg = ServiceMessage.encodeRequstList(serviceNames);
		DatagramPacket request = new DatagramPacket(msg.getBytes(), msg.length(), group, ServiceAnnouncer.MDNSPortPlus);
		
		// for replies
		byte[] buf = new byte[1500];
		DatagramPacket recv = new DatagramPacket(buf, buf.length);
		
		while (!done)
		{
			try
			{
				sock.send(request);
				log.log(Level.INFO, "finder sent {0}", msg);
				
				sock.setSoTimeout(1000);
				long marker = System.currentTimeMillis() + 1000;
				while (System.currentTimeMillis() < marker)  // make sure we break at some point if flood of messages
				{
					try { sock.receive(recv); }
					catch (SocketTimeoutException toe) { break; } // full timeout, break out to resend
					String data = new String(recv.getData(), 0, recv.getLength());
					log.log(Level.INFO, "finder receives {0}", data);
					
					if (!data.contains(","))  // nothing good in that packet, don't bother trying
						continue;
					
					for (String incoming : data.split("\n"))
					{
						ServiceMessage announcement = ServiceMessage.decodeMessage(incoming);
						if (announcement.isAnnouncement() && serviceNames.contains(announcement.getService()))
						{
							FoundService decoded = new FoundService(recv.getAddress(), announcement);
							if (found.add(decoded) && (listener != null))
								listener.newService(decoded);	// new element, notify listener
						}
					}
				}
			}
			catch (IOException ioe) 
			{
				log.log(Level.INFO, "servicefinder: {0}", ioe);
				try { Thread.sleep(1000); } catch (InterruptedException ex) {} // don't go into super loop on errors
			}
		}
			
		sock.close();
	}
}
