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
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.wwscc.util.ThreadedClass;

/**
 */
public class ServiceAnnouncer implements ThreadedClass
{
	private static final Logger log = Logger.getLogger(ServiceAnnouncer.class.getCanonicalName());
	public static final String MDNSAddr = "224.0.0.251";  // mDNS address
	public static final int MDNSPortPlus = 5354;  // one port higher than mDNS
	
	private boolean done;
	private MulticastSocket sock;
	private InetAddress group;
	final private List<ServiceMessage> descriptions;

	public ServiceAnnouncer() throws UnknownHostException, IOException
	{
		descriptions = Collections.synchronizedList(new ArrayList<ServiceMessage>());
		group = InetAddress.getByName(MDNSAddr);
		sock = new MulticastSocket(MDNSPortPlus);
		sock.joinGroup(group);	
		done = true;
	}

	public void addDescription(ServiceMessage desc)
	{
		descriptions.add(desc);
	}
	
	public void removeDescription(ServiceMessage desc)
	{
		descriptions.remove(desc);
	}

	@Override
	public void start()
	{
		if (!done) return;
		done = false;
		new Thread(new AnnouncerThread()).start();
	}
	
	@Override
	public void stop()
	{
		done = true;
	}

	class AnnouncerThread implements Runnable
	{
		@Override
		public void run()
		{
			while (!done)
			{
				byte[] buf = new byte[1500];
				List<ServiceMessage> toReply = new ArrayList<ServiceMessage>();

				try
				{
					DatagramPacket recv = new DatagramPacket(buf, buf.length);
					ServiceMessage requestMessage;
					sock.receive(recv);
					log.log(Level.INFO, "announcer receives: {0}", new String(recv.getData()));

					toReply.clear();
					for (String incoming : new String(recv.getData(), 0, recv.getLength()).split("\n"))
					{
						requestMessage = ServiceMessage.decodeMessage(incoming);
						if (requestMessage.isRequest())
						{
							synchronized (descriptions) {
								for (ServiceMessage desc : descriptions) {
									if (desc.getService().equals(requestMessage.getService())) {
										toReply.add(desc);
									}
								}
							}
						}
					}

					if (toReply.size() > 0)
					{
						String replyMessage = ServiceMessage.encodeList(toReply);
						DatagramPacket reply = new DatagramPacket(replyMessage.getBytes(), replyMessage.length(), group, MDNSPortPlus);
						sock.send(reply);
					}
				}
				catch (Exception ioe)
				{
					log.log(Level.INFO, "serviceannouncer: {0}", ioe);
					try { Thread.sleep(1000); } catch (InterruptedException ie) {}
				}
			}
			
			sock.close();
		}
	}

	public static void main(String args[]) throws UnknownHostException, IOException
	{
		ServiceAnnouncer f = new ServiceAnnouncer();
		f.addDescription(ServiceMessage.createType("ProTimer", InetAddress.getLocalHost().getHostName(), 62608));
		f.addDescription(ServiceMessage.createType("RemoteDatabase", "ww2012", 3332));
		f.start();
	}
}
