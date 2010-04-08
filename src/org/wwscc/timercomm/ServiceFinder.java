/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.wwscc.timercomm;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 */
public class ServiceFinder
{
	private static Logger log = Logger.getLogger(ServiceFinder.class.getCanonicalName());
	
	protected boolean done = false;
	protected MulticastSocket sock = null;
	protected InetAddress group = null;
	protected String serviceName;
	protected HashSet<InetSocketAddress> found;

	public ServiceFinder(String sname) throws UnknownHostException, IOException
	{
		found = new HashSet<InetSocketAddress>();
		serviceName = sname;
		group = InetAddress.getByName(ServiceAnnouncer.MDNSAddr);
		sock = new MulticastSocket(ServiceAnnouncer.MDNSPort);
		sock.joinGroup(group);		
	}

	class Sender implements Runnable
	{
		@Override
		public void run()
		{
			String msg = String.format("FIND " + serviceName);
			for (int ii = 0; ii < 3; ii++)
			{
				try
				{
					DatagramPacket request = new DatagramPacket(msg.getBytes(), msg.length(), group, ServiceAnnouncer.MDNSPort);
					sock.send(request);
					Thread.sleep(300);
				}
				catch (Exception ioe) {}
			}

			done = true;
			sock.close();
		}
	}


	public Collection<InetSocketAddress> find()
	{
		new Thread(new Sender()).start();
		
		while (!done)
		{
			try
			{
				byte[] buf = new byte[256];
				DatagramPacket recv = new DatagramPacket(buf, buf.length);
				sock.receive(recv);

				String data[] = new String(recv.getData()).split("\\s+");
				if (data[0].equals("SERVICE") && data[1].equals(serviceName))
					found.add(new InetSocketAddress(recv.getAddress(), new Integer(data[2].trim())));
			}
			catch (Exception ioe)
			{
				log.info("servicefinder: " + ioe);
			}
		}

		return found;
	}

	public static InetSocketAddress dialogFind(String service) throws IOException
	{
		ServiceFinder f = new ServiceFinder(service);
		return (InetSocketAddress)JOptionPane.showInputDialog(null, "Services of type " + service,
			"Find Service", JOptionPane.PLAIN_MESSAGE, null, f.find().toArray(), null);
	}

	public static void main(String args[]) throws UnknownHostException, IOException
	{
		System.out.println(dialogFind("BWTimer"));
	}
}
