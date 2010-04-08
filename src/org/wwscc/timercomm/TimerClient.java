/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

/**
 *
 * @author bwilson
 */
public class TimerClient implements Runnable
{
	private static Logger log = Logger.getLogger(TimerClient.class.getName());

	Socket sock;
	BufferedReader in;
	OutputStream out;
	boolean done = false;

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
	}

	public TimerClient(Socket s) throws IOException
	{
		sock = s;
		in = new BufferedReader(new InputStreamReader(s.getInputStream()));
		out = s.getOutputStream();
	}

	public void close() throws IOException
	{
		done = true;
		sock.close();
	}
	
	public boolean send(String s)
	{
		try {
			out.write(s.getBytes());
			return true;
		} catch (IOException ioe) {
			log.log(Level.INFO, "TimerClient send failed: " + ioe, ioe);
			done = true;
		}
		return false;
	}

	public boolean sendDial(LeftRightDialin d)
	{
		return send(("DIAL " + d.encode() + "\n"));
	}

	public boolean sendRun(Run r)
	{
		return send(("RUN " + r.encode() + "\n"));
	}

	public boolean deleteRun(Run r)
	{
		return send(("DELETE " + r.encode() + "\n"));
	}

	@Override
	public void run()
	{
		try
		{
			Messenger.sendEvent(MT.TIMER_SERVICE_CONNECTION, true);

			while (!done)
			{
				String line = in.readLine();
				log.info("TimerClient reads: " + line);
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
		Messenger.sendEvent(MT.TIMER_SERVICE_CONNECTION, false);
	}
}