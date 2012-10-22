/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.timercomm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.wwscc.storage.LeftRightDialin;
import org.wwscc.storage.Run;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;
import org.wwscc.util.ThreadedClass;

/**
 *
 * @author bwilson
 */
public final class TimerClient implements RunServiceInterface, ThreadedClass
{
	private static final Logger log = Logger.getLogger(TimerClient.class.getName());

	Socket sock;
	BufferedReader in;
	OutputStream out;
	boolean done;

	public TimerClient(String host, int port) throws IOException
	{
		this(new InetSocketAddress(host, port));
	}

	public TimerClient(InetSocketAddress addr) throws IOException
	{
		sock = new Socket();
		sock.connect(addr);
		in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		out = sock.getOutputStream();
		done = true;
	}

	public TimerClient(Socket s) throws IOException
	{
		sock = s;
		in = new BufferedReader(new InputStreamReader(s.getInputStream()));
		out = s.getOutputStream();
		done = true;
	}

	@Override
	public void start()
	{
		if (!done) return;
		done = false;
		new Thread(new ReceiverThread()).start();
	}
	
	@Override
	public void stop()
	{
		done = true;
	}
	
	public boolean send(String s)
	{
		try {
			log.log(Level.FINE, "Sending ''{0}'' to the timer", s);
			out.write(s.getBytes());
			return true;
		} catch (IOException ioe) {
			log.log(Level.INFO, "TimerClient send failed: " + ioe, ioe);
			done = true;
		}
		return false;
	}

	@Override
	public boolean sendDial(LeftRightDialin d)
	{
		return send(("DIAL " + d.encode() + "\n"));
	}

	@Override
	public boolean sendRun(Run r)
	{
		return send(("RUN " + r.encode() + "\n"));
	}

	@Override
	public boolean deleteRun(Run r)
	{
		return send(("DELETE " + r.encode() + "\n"));
	}

	class ReceiverThread implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				Messenger.sendEvent(MT.TIMER_SERVICE_CONNECTION, new Object[] { TimerClient.this, true });

				while (!done)
				{
					String line = in.readLine();
					log.log(Level.INFO, "TimerClient reads: {0}", line);
					if (line.startsWith("DIAL "))
					{
						LeftRightDialin d = new LeftRightDialin();
						d.decode(line.substring(5));
						Messenger.sendEvent(MT.TIMER_SERVICE_DIALIN, d);
					}
					else if (line.startsWith("RUN "))
					{
						Run r = new Run();
						r.decode(line.substring(4));
						Messenger.sendEvent(MT.TIMER_SERVICE_RUN, r);
					}
					else if (line.startsWith("DELETE "))
					{
						Run r = new Run();
						r.decode(line.substring(7));
						Messenger.sendEvent(MT.TIMER_SERVICE_DELETE, r);
					}
				}
			}
			catch (IOException ex)
			{
				log.log(Level.INFO, "read failure: " + ex, ex);
			}

			try { sock.close(); } catch (IOException ioe)  {}
			Messenger.sendEvent(MT.TIMER_SERVICE_CONNECTION, new Object[] { TimerClient.this, false });
		}
	}
}