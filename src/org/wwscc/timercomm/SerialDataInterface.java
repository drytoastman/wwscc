/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2009 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.timercomm;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TooManyListenersException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.wwscc.dialogs.PortDialog;
import org.wwscc.storage.Run;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;
import org.wwscc.util.TimerTimestamp;


public class SerialDataInterface implements SerialPortEventListener
{
	private static Logger log = Logger.getLogger(SerialDataInterface.class.getCanonicalName());

	private CommPortIdentifier commId = null;
    private SerialPort port = null;
	private OutputStream os = null;
	private InputStream is = null;
	private ByteBuffer buffer = null;
	private boolean open = false;
	private int ioerrorbump = 0;


    public SerialDataInterface(CommPortIdentifier portId)
	{
		commId = portId;
	}

	public void open() throws PortInUseException, UnsupportedCommOperationException, TooManyListenersException, IOException
	{
		if (open)
		{
			log.info(commId.getName() + " already open, skipping");
			return;
		}
		
		log.info("Opening port " + commId.getName());
		buffer = new ByteBuffer();

		port = (SerialPort)commId.open("TimerInterface-"+commId.getName(), 30000);
		port.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);

		port.addEventListener(this);
		port.notifyOnDataAvailable(true);
		port.enableReceiveTimeout(30);

		os = port.getOutputStream();
		is = port.getInputStream();
		open = true;
    }

	public void write(String s) throws IOException
	{
		os.write(s.getBytes());
	}
	
	public void close() throws IOException
	{
		log.info("Closing port " + commId.getName());
		os.close();
		is.close();
		port.close();
		open = false;
	}


	public void processData(byte[] buf)
	{
		switch ((int)buf[0] & 0xFF)  // compare as unsigned
		{
			case 0x80:
				String strtime = new StringBuffer(new String(buf, 1, buf.length-1)).reverse().toString();
				double time = Double.valueOf(strtime) / 1000;
				log.fine("Finish time: " + time);
				Messenger.sendEvent(MT.SERIAL_TIMER_DATA, new Run(time));
				break;

			case 0xEE:
				int sensor = buf[1];
				int bytes[] = new int[] { (int)buf[2]&0xFF, (int)buf[3]&0xFF, (int)buf[4]&0xFF, (int)buf[5]&0xFF };
				long timestamp = (bytes[3]<<24) + (bytes[2]<<16) + (bytes[1]<<8) + bytes[0];
				log.finest("Timestamp: " + sensor + ", " + timestamp);
				Messenger.sendEvent(MT.SERIAL_TIMESTAMP, new TimerTimestamp(sensor, timestamp/10));
				break;

			case 0xF0: log.info("Mode changed"); break;
			case 0x90: log.info("Car started"); break;
			case 0xA0: log.info("Timer restarted"); break;
			case 0xB0: log.info("Sensors disabled"); break;
			case 0xC0: log.info("Sensors enabled"); break;

			default:
				Messenger.sendEvent(MT.SERIAL_GENERIC_DATA, buf);
		}
	}


	@Override
    public void serialEvent(SerialPortEvent e)
	{
		int type = e.getEventType();
		switch (type)
		{
		    case SerialPortEvent.DATA_AVAILABLE:
				try
				{
					buffer.readData(is);
					byte[] newline;
					while ((newline = buffer.getNextLine()) != null)
						processData(newline);
				}
				catch (IOException ex)
				{
					ioerrorbump++; // only show error if this is repeated, once is from reopening
					if (ioerrorbump > 1)
						log.log(Level.WARNING, "serial port read error: " + ex, ex);
				}
				ioerrorbump = 0;
				break;

			default:
				System.out.println("Recieved serialEvent " + type);
				break;
		}
	}   

	class ByteBuffer
	{
		byte[] buffer;
		int count;
		int search;

		public ByteBuffer()
		{
			buffer = new byte[4096];
			count = 0;
			search = 0;
		}

		public void readData(InputStream in) throws IOException
		{
			int size = is.available();
			if (size + count > buffer.length)
			{
				byte[] newbuffer = new byte[buffer.length*2];
				System.arraycopy(buffer, 0, newbuffer, 0, count);
				buffer = newbuffer;
				log.info("Increased byte buffer size to: " + buffer.length);
			}
			is.read(buffer, count, size);
			count += size;
		}

		public byte[] getNextLine()
		{
			for ( ; search < (count-1); search++)
			{
				if ((buffer[search] == '\r') && (buffer[search+1] == '\n'))
				{
					byte[] ret = new byte[search];
					System.arraycopy(buffer, 0, ret, 0, search); // copy out line from start of buffer
					search += 2; // skip the \r\n
					System.arraycopy(buffer, search, buffer, 0, count-search); // move from search back to start of buffer
					count -= search;
					search = 0;
					return ret;
				}
			}

			return null;
		}
	}

	private static Map<String, SerialDataInterface> _ports = null;

	private static void scan()
	{
		if (_ports == null)
			_ports = new HashMap<String, SerialDataInterface>();
		List<CommPortIdentifier> found = new ArrayList<CommPortIdentifier>();
		java.util.Enumeration list = CommPortIdentifier.getPortIdentifiers();
		while (list.hasMoreElements())
		{
			CommPortIdentifier c = (CommPortIdentifier)list.nextElement();
			log.fine("RXTX found " + c.getName());
			if (c.getPortType() == CommPortIdentifier.PORT_SERIAL)
				found.add(c);
		}

		// Add new
		for (CommPortIdentifier c : found)
		{
			if (!_ports.containsKey(c.getName()))
				_ports.put(c.getName(), new SerialDataInterface(c));
		}

		// Remove missing
		for (SerialDataInterface di : _ports.values())
		{
			if (!found.contains(di.commId))
				_ports.remove(di.commId.getName());
		}
	}

	private static Map<String, SerialDataInterface> getPorts()
	{
		if (_ports == null)
			scan();
		return _ports;
	}

	public static SerialDataInterface open(String name)
	{
		try
		{
			SerialDataInterface i = getPorts().get(name);
			if (i == null)
				log.warning(name + " doesn't exist");
			else if (i.open)
				log.warning(name + " already open");
			else
				i.open();
			return i;
		} 
		catch (Exception e)
		{
			log.warning("Unable to open port: " + e);
			return null;
		}
	}

	public static void close(String name)
	{
		try 
		{
			SerialDataInterface i = getPorts().get(name);
			if (i == null)
				log.warning(name + " doesn't exist");
			else if (!i.open)
				log.warning(name + " already closed");
			else
				i.close();
		} 
		catch (Exception e)
		{
			log.warning("Unable to close port: " + e);
		}
	}

	public static String selectPort(String prefName)
	{
		ArrayList<String> a, u;

		a = new ArrayList<String>();
		u = new ArrayList<String>();

		scan();
		for (String s : getPorts().keySet())
		{
			try
			{
				SerialDataInterface sdi = getPorts().get(s);
				if (sdi.open)
				{
					u.add(s);
					continue;
				}
				SerialPort p = (SerialPort)sdi.commId.open("Testing", 30000);
				p.close();
				a.add(s);
			}
			catch (PortInUseException pue)
			{
				u.add(s);
			}
		}

		PortDialog d = new PortDialog("", a, u);
		d.doDialog("Select COM Port", null);
		String s = d.getResult();
		if ((s == null) || (s.equals("")))
			return null;
		return s;
	}

	public static void main(String args[])
	{
		System.out.println(selectPort(""));
	}
}

