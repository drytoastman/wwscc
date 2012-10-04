/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

/**
 */
public class ServiceAnnouncer implements Runnable
{
	private static Logger log = Logger.getLogger(ServiceAnnouncer.class.getCanonicalName());
	public static final String MDNSAddr = "224.0.0.251";  // mDNS address
	public static final int MDNSPort = 5354;  // one port higher than mDNS
	
	protected boolean done = false;
	protected MulticastSocket sock = null;
	protected InetAddress group = null;
	final protected List<ServiceMessage> descriptions;

	public ServiceAnnouncer() throws UnknownHostException, IOException
	{
		descriptions = Collections.synchronizedList(new ArrayList<ServiceMessage>());
		group = InetAddress.getByName(MDNSAddr);
		sock = new MulticastSocket(MDNSPort);
		sock.joinGroup(group);
	}

	public void addDescription(ServiceMessage desc)
	{
		descriptions.add(desc);
	}
	
	public void removeDescription(ServiceMessage desc)
	{
		descriptions.remove(desc);
	}

	public void close() throws IOException
	{
		done = true;
		sock.leaveGroup(group);
		sock.close();
	}

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
					StringBuilder builder = new StringBuilder();
					for (ServiceMessage out : toReply)
					{
						builder.append(out.encode());
						builder.append("\n");
					}
					String replyMessage = builder.toString();
					DatagramPacket reply = new DatagramPacket(replyMessage.getBytes(), replyMessage.length(), group, MDNSPort);
					sock.send(reply);
				}
			}
			catch (Exception ioe)
			{
				log.info("serviceannouncer: " + ioe);
				try { Thread.sleep(1000); } catch (InterruptedException ie) {}
			}
		}
	}

	public static void main(String args[]) throws UnknownHostException, IOException
	{
		ServiceAnnouncer f = new ServiceAnnouncer();
		f.addDescription(ServiceMessage.createType("ProTimer", InetAddress.getLocalHost().getHostName(), 62608));
		f.addDescription(ServiceMessage.createType("RemoteDatabase", "ww2012", 3332));
		new Thread(f, "ServiceAnnouncer").start();
	}
}
