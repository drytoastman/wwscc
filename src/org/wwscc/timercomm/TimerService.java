/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.timercomm;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.wwscc.services.ServiceAnnouncer;
import org.wwscc.services.ServiceMessage;
import org.wwscc.storage.LeftRightDialin;
import org.wwscc.storage.Run;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;
import org.wwscc.util.ThreadedClass;

/**
 * A server that create TimerClients for each connection as well as running a
 * TimerService to advertise our location.
 */
public class TimerService implements RunServerListener, ThreadedClass
{
	private static final Logger log = Logger.getLogger(TimerService.class.getName());

	ServerSocket serversock;
	ServiceAnnouncer announcer;
	Vector<RunServerListener> clients;
	Vector<RunServerListener> marked;
	boolean done;

	
	public TimerService(String name) throws IOException
	{
		serversock = new ServerSocket(0);
		log.log(Level.INFO, "Service {0} started on port {1}", new Object[]{name, serversock.getLocalPort()});
		
		announcer = new ServiceAnnouncer();
		announcer.addDescription(ServiceMessage.createType(name, InetAddress.getLocalHost().getHostName(), serversock.getLocalPort()));
		announcer.start();
		clients = new Vector<RunServerListener>();
		marked = new Vector<RunServerListener>();
		done = true;
	}

	@Override
	public void start()
	{
		if (!done) return;
		done = false;
		new Thread(new ServiceThread()).start();
	}
	
	@Override
	public void stop()
	{
		done = true;
	}

	@Override
	public boolean sendDial(LeftRightDialin d)
	{
		boolean ret = true;
		for (RunServerListener c : clients) {
			if (!c.sendDial(d))
			{
				marked.add(c);
				ret = false;
			}
		}
		clients.removeAll(marked);
		marked.clear();
		return ret;
	}

	@Override
	public boolean sendRun(Run r)
	{
		boolean ret = true;
		for (RunServerListener c : clients) {
			if (!c.sendRun(r)) 
			{
				marked.add(c);
				ret = false;
			}
		}
		clients.removeAll(marked);
		marked.clear();
		return ret;	
	}
	
	@Override
	public boolean deleteRun(Run r)
	{
		boolean ret = true;
		for (RunServerListener c : clients) {
			if (!c.deleteRun(r))
			{
				marked.add(c);
				ret = false;
			}
		}
		clients.removeAll(marked);
		marked.clear();
		return ret;
	}

	class ServiceThread implements Runnable
	{
		@Override
		public void run()
		{
			String ip;
			try { ip = InetAddress.getLocalHost().getHostAddress(); }
			catch (UnknownHostException ex) { ip = "unknown"; }
			
			Messenger.sendEvent(MT.TIMER_SERVICE_LISTENING, new Object[] { this, ip, serversock.getLocalPort() } );
			
			while (!done)
			{
				try
				{
					Socket s = serversock.accept();
					TimerClient c = new TimerClient(s);
					c.start();
					clients.add(c);
				}
				catch (IOException ioe)
				{
					log.log(Level.INFO, "Server error: {0}", ioe);
				}
			}
			
			for (RunServerListener tc : clients)
			{
				((TimerClient)tc).stop();
			}

			announcer.stop();
			try { serversock.close(); } catch (IOException ioe) {}
			
			Messenger.sendEvent(MT.TIMER_SERVICE_NOTLISTENING, this);
		}
	}
}
