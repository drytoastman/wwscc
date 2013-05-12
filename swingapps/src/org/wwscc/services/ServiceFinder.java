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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.wwscc.util.ThreadedClass;

/**
 */
public class ServiceFinder implements ThreadedClass
{
	private static final Logger log = Logger.getLogger(ServiceFinder.class.getCanonicalName());
	
	private boolean done;
	private InetAddress group;
	private List<String> serviceNames;
	private Map<FoundService, Long> found;
	private List<ServiceFinderListener> listeners;

	public interface ServiceFinderListener
	{
		public void newService(FoundService service);
		public void serviceTimedOut(FoundService service);
	}
	
	public ServiceFinder(String name) throws IOException
	{
		this(Arrays.asList(new String[] { name }));
	}
	
	public ServiceFinder(List<String> names) throws IOException 
	{
		done = true;
		found = new HashMap<FoundService, Long>();
		serviceNames = names;
		group = InetAddress.getByName(ServiceAnnouncer.MDNSAddr);
		listeners = new Vector<ServiceFinderListener>();
	}
	
	protected void fireNewService(final FoundService service)
	{
		SwingUtilities.invokeLater(new Runnable() { public void run() {
			for (ServiceFinderListener listener : listeners) {
				listener.newService(service);	// new element, notify listeners
		}}});
	}
	
	public void addListener(ServiceFinderListener lis)
	{
		listeners.add(lis);
		for (FoundService service : found.keySet())
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
		MulticastSocket sock = null;
		int rounds = 10;
		
		/**
		 * Run through current services to see if something should be timedout
		 */
		void timeout()
		{
			long threshold = System.currentTimeMillis() - 30000; // 30 seconds
			Iterator<Map.Entry<FoundService, Long>> iter = found.entrySet().iterator();
			while (iter.hasNext())
			{
				Map.Entry<FoundService, Long> entry = iter.next();
				if (entry.getValue() < threshold)
				{
					log.log(Level.INFO, "finder timesout " + entry.getKey());
					for (ServiceFinderListener listener : listeners)
						listener.serviceTimedOut(entry.getKey());
					iter.remove();
				}
			}
		}
		
		/**
		 * Process incoming data from a multicast transmission
		 * @param recv the incoming packet
		 * @throws IOException
		 */
		void processData(DatagramPacket recv) throws IOException
		{
			String data = new String(recv.getData(), 0, recv.getLength());
			log.log(Level.INFO, "finder receives " + data);

			if (!data.contains(","))  // nothing good in that packet, don't bother trying
				return;

			for (String incoming : data.split("\n"))
			{
				ServiceMessage announcement = ServiceMessage.decodeMessage(incoming);
				if (announcement.isAnnouncement() && serviceNames.contains(announcement.getService()))
				{
					if (recv.getAddress().equals(InetAddress.getLocalHost()))
						recv.setAddress(InetAddress.getLoopbackAddress()); // make sure it shows up as 127.0.0.1
					FoundService decoded = new FoundService(recv.getAddress(), announcement);
					if (!found.containsKey(decoded))
						fireNewService(decoded);
					// update timestamp regardless
					found.put(decoded, System.currentTimeMillis());
				}
			}			
		}
		
		/**
		 * reset ourselves periodically to make sure multicast is initiating correctly
		 * @throws IOException 
		 */
		void socketCheck() throws IOException
		{
			if (++rounds > 5)
			{
				rounds = 0;
				if (sock != null && sock.isBound())
					sock.close();
				sock = new MulticastSocket(ServiceAnnouncer.MDNSPortPlus);
				sock.joinGroup(group);
			}			
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
					socketCheck();
					
					sock.send(request);
					sock.setSoTimeout(1000);
					
					long marker = System.currentTimeMillis() + 1000;
					while (System.currentTimeMillis() < marker)  // make sure we break at some point if flood of messages
					{
						try { sock.receive(recv); }
						catch (SocketTimeoutException toe) { break; } // full timeout, break out to resend
						processData(recv);
					}
					
					timeout();
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
