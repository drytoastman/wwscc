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
	protected String serviceName;
	protected int servicePort;


	public ServiceAnnouncer(String sname, int sport) throws UnknownHostException, IOException
	{
		serviceName = sname;
		servicePort = sport;
		group = InetAddress.getByName(MDNSAddr);
		sock = new MulticastSocket(MDNSPort);
		sock.joinGroup(group);
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

				if (new String(recv.getData()).startsWith("FIND " + serviceName))
				{
					String msg = String.format("SERVICE %s %s", serviceName, servicePort);
					DatagramPacket reply = new DatagramPacket(msg.getBytes(), msg.length(), group, MDNSPort);
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
		ServiceAnnouncer f = new ServiceAnnouncer("ProTimer", 666);
		new Thread(f, "ServiceAnnouncer").start();
	}
}
