/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.wwscc.timercomm;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.logging.Logger;
import org.wwscc.services.ServiceAnnouncer;
import org.wwscc.services.ServiceMessage;
import org.wwscc.storage.LeftRightDialin;
import org.wwscc.storage.Run;

/**
 * A server that create TimerClients for each connection as well as running a
 * TimerService to advertise our location.
 */
public class TimerService implements Runnable
{
	private static Logger log = Logger.getLogger(TimerService.class.getName());

	ServerSocket serversock;
	ServiceAnnouncer announcer;
	Vector<TimerClient> clients;
	Vector<TimerClient> marked;
	boolean done = false;

	public TimerService(String name) throws IOException
	{
		serversock = new ServerSocket(0);
		log.info("Service " + name + " started on port " + serversock.getLocalPort());
		announcer = new ServiceAnnouncer();
		announcer.addDescription(ServiceMessage.createType(name, serversock.getInetAddress().getHostName(), serversock.getLocalPort()));
		clients = new Vector<TimerClient>();
		marked = new Vector<TimerClient>();
		new Thread(announcer).start();
	}

	public void close()
	{
		done = true;
		for (TimerClient tc : clients)
		{
			try {
				tc.close();
			} catch (IOException ioe) {
				log.warning("Couldn't close Timer Client: " + ioe);
			}
		}

		try { announcer.close(); } catch (IOException ioe) {}
		try { serversock.close(); } catch (IOException ioe) {}
	}

	public void sendDial(LeftRightDialin d)
	{
		for (TimerClient c : clients) {
			if (!c.sendDial(d))
				marked.add(c);
		}
		clients.removeAll(marked);
		marked.clear();
	}

	public void sendRun(Run r)
	{
		for (TimerClient c : clients) {
			if (!c.sendRun(r))
				marked.add(c);
		}
		clients.removeAll(marked);
		marked.clear();
	}

	public void deleteRun(Run r)
	{
		for (TimerClient c : clients) {
			if (!c.deleteRun(r))
				marked.add(c);
		}
		clients.removeAll(marked);
		marked.clear();
	}

	@Override
	public void run()
	{
		while (!done)
		{
			try
			{
				Socket s = serversock.accept();
				TimerClient c = new TimerClient(s);
				clients.add(c);
				new Thread(c, "ClientThread").start();
			}
			catch (IOException ioe)
			{
				log.info("Server stopped: " + ioe);
			}
		}

		try { announcer.close(); } catch (IOException ioe) {}
		try { serversock.close(); } catch (IOException ioe) {}
	}
}
