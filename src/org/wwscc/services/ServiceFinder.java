/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.wwscc.services;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.HashSet;
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
	protected String serviceName;
	protected HashSet<FoundService> found;

	public interface ServiceFinderListener
	{
		public void newService(FoundService service);
	}
	
	public ServiceFinder(String sname) throws IOException 
	{
		listener = null;
		done = false;
		found = new HashSet<FoundService>();
		serviceName = sname;
		group = InetAddress.getByName(ServiceAnnouncer.MDNSAddr);
		sock = new MulticastSocket(ServiceAnnouncer.MDNSPort);
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
		ServiceMessage announcement;
		String msg = ServiceMessage.createRequest(serviceName).encode();
		byte[] buf = new byte[1500];
				
		DatagramPacket request = new DatagramPacket(msg.getBytes(), msg.length(), group, ServiceAnnouncer.MDNSPort);
		DatagramPacket recv = new DatagramPacket(buf, buf.length);
		
		while (!done)
		{
			try
			{
				sock.send(request);
				log.log(Level.INFO, "finder sent {0}", request);
				sock.setSoTimeout(1000);
				long marker = System.currentTimeMillis() + 1000;
				while (System.currentTimeMillis() < marker)  // make sure we break at some point if flood of messages
				{
					try { sock.receive(recv); }
					catch (SocketTimeoutException toe) { break; } // full timeout, break out to resend
					log.log(Level.INFO, "finder receives {0}", new String(recv.getData()));
					
					for (String incoming : new String(recv.getData(), 0, recv.getLength()).split("\n"))
					{
						announcement = ServiceMessage.decodeMessage(incoming);
						if (announcement.isAnnouncement() && announcement.getService().equals(serviceName))
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
