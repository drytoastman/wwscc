/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.wwscc.timercomm;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 */
public class ServiceAnnouncer implements Runnable
{
	private static Logger log = Logger.getLogger(ServiceAnnouncer.class.getCanonicalName());
	public static final String MDNSAddr = "228.98.99.100";
	public static final int MDNSPort = 6789;
	
	protected boolean done = false;
	protected MulticastSocket sock = null;
	protected InetAddress group = null;
	final protected Map<String, Integer> descriptions;

	public ServiceAnnouncer() throws UnknownHostException, IOException
	{
		descriptions = Collections.synchronizedMap(new HashMap<String, Integer>());
		group = InetAddress.getByName(MDNSAddr);
		sock = new MulticastSocket(MDNSPort);
		sock.joinGroup(group);
	}

	public void setDescription(String name, int port)
	{
		descriptions.put(name, port);
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
			try
			{
				byte[] buf = new byte[256];
				DatagramPacket recv = new DatagramPacket(buf, buf.length);
				sock.receive(recv);

				String request[] = new String(recv.getData(), 0, recv.getLength()).split("\\s+");
				if (request[0].equals("FIND"))
				{
					synchronized (descriptions) {
						for (String name : descriptions.keySet()) {
							if (name.equals(request[1])) {
								String msg = String.format("SERVICE %s %s", name, descriptions.get(name));
								DatagramPacket reply = new DatagramPacket(msg.getBytes(), msg.length(), group, MDNSPort);
								sock.send(reply);
							}
						}
					}
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
		f.setDescription("ProTimer", 62608);
		f.setDescription("RemoteDatabase", 3332);
		new Thread(f, "ServiceAnnouncer").start();
	}
}
