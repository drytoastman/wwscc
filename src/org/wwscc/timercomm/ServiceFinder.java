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
	protected HashSet<FoundService> found;

	public static class FoundService
	{
		public InetAddress host;
		public String name;
		public int port;
		public String[] args;

		public FoundService(InetAddress h, String[] vals)
		{
			host = h;
			name = vals[1];
			port = new Integer(vals[2].trim());
			if (vals.length > 3)
			{
				args = new String[vals.length - 3];
				for (int ii = 3; ii < vals.length; ii++)
					args[ii-3] = vals[ii];
			}
			else
			{
				args = new String[0];
			}
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			final FoundService other = (FoundService) obj;
			if ((this.host == null) ? (other.host != null) : !this.host.equals(other.host))  return false;
			if (this.port != other.port) return false;
			return true;
		}

		@Override
		public int hashCode() {
			int hash = 7;
			hash = 29 * hash + (this.host != null ? this.host.hashCode() : 0);
			hash = 29 * hash + this.port;
			return hash;
		}

		@Override
		public String toString()
		{
			return host.getHostAddress() + ":" + port;
		}

	}

	public ServiceFinder(String sname) throws UnknownHostException, IOException
	{
		found = new HashSet<FoundService>();
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


	public Collection<FoundService> find()
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
					found.add(new FoundService(recv.getAddress(), data));
			}
			catch (Exception ioe)
			{
				log.info("servicefinder: " + ioe);
			}
		}

		return found;
	}

	public static FoundService dialogFind(String service) throws IOException
	{
		ServiceFinder f = new ServiceFinder(service);
		return (FoundService)JOptionPane.showInputDialog(null, "Services of type " + service,
			"Find Service", JOptionPane.PLAIN_MESSAGE, null, f.find().toArray(), null);
	}

	public static void main(String args[]) throws UnknownHostException, IOException
	{
		System.out.println(dialogFind("ProTimer"));
		System.out.println(dialogFind("RemoteDatabase"));
	}
}
