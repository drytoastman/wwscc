package org.wwscc.util;

import java.math.BigInteger;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.UUID;

public class IdGenerator 
{
	public static final UUID nullid = new UUID(0,0);
	
	private static long lastms = 0;
	private static long counter = 0;
	private static long hw = 0;
	
	static
	{
		for (int ii = 0; ii < 10; ii++)
		{
			try 
			{
				NetworkInterface ni = NetworkInterface.getByIndex(ii);
				if (ni != null) // real interface
				{
					byte[] hwaddr = ni.getHardwareAddress();
					if (hwaddr != null) // not localhost
					{
						hw = new BigInteger(hwaddr).longValue();
						break;
					}
				}
			} 
			catch (SocketException se) {}
		}
	}

	
	public synchronized static UUID generateId()
	{
		long ms = System.currentTimeMillis();
		if (lastms == ms) {
			counter++;
		} else {
			counter = 0;
		}
		lastms = ms;
		return new UUID(lastms, (counter << 48) | hw );
	}
}
