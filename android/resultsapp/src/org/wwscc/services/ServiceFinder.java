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
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.wwscc.util.ThreadedClass;

/**
 */
public class ServiceFinder implements ThreadedClass
{
	private static final Logger log = Logger.getLogger(ServiceFinder.class.getCanonicalName());
	
	private boolean done;
	private InetAddress group;
	private List<String> serviceNames;
	private HashSet<FoundService> found;
	private List<ServiceFinderListener> listeners;

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
		done = true;
		found = new HashSet<FoundService>();
		serviceNames = names;
		group = InetAddress.getByName(ServiceAnnouncer.MDNSAddr);
		listeners = new Vector<ServiceFinderListener>();
	}
	
	public void addListener(ServiceFinderListener lis)
	{
		listeners.add(lis);
		for (FoundService service : found)
			lis.newService(service);
	}
	
	@Override
	public void start()
	{
		if (!done) return;
		done = false;
		found.clear();
		new Thread(new FinderThread()).start();
	}
	
	@Override
	public void stop()
	{
		done = true;
	}

	class FinderThread implements Runnable
	{
		@Override
		public void run()
		{
			MulticastSocket sock = null;
			
			// (re)create the socket
			try
			{
				sock = new MulticastSocket(ServiceAnnouncer.MDNSPortPlus);
				sock.joinGroup(group);
			}
			catch (IOException ioe)
			{
				log.log(Level.WARNING, "servicefinder start failure: " + ioe);
				try { Thread.sleep(1000); } catch (InterruptedException ex) {} // don't go into super loop on errors
			}
			
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
					sock.setSoTimeout(1000);
					long marker = System.currentTimeMillis() + 1000;
					while (System.currentTimeMillis() < marker)  // make sure we break at some point if flood of messages
					{
						try { sock.receive(recv); }
						catch (SocketTimeoutException toe) { break; } // full timeout, break out to resend
						String data = new String(recv.getData(), 0, recv.getLength());
						log.log(Level.INFO, "finder receives " + data);

						if (!data.contains(","))  // nothing good in that packet, don't bother trying
							continue;

						for (String incoming : data.split("\n"))
						{
							ServiceMessage announcement = ServiceMessage.decodeMessage(incoming);
							if (announcement.isAnnouncement() && serviceNames.contains(announcement.getService()))
							{
								if (recv.getAddress().equals(InetAddress.getLocalHost()))
									continue; // ignore myself, they should use direct connection
								FoundService decoded = new FoundService(recv.getAddress(), announcement);
								if (found.add(decoded))
								{
									for (ServiceFinderListener listener : listeners)
										listener.newService(decoded);	// new element, notify listeners
								}
							}
						}
					}
				}
				catch (IOException ioe) 
				{
					log.log(Level.WARNING, "servicefinder: " + ioe);
					try { Thread.sleep(8000); } catch (InterruptedException ex) {} // don't go into super loop on errors
				}
			}

			sock.close();
		}
	}
}
