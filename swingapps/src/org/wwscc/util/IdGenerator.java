package org.wwscc.util;

import java.math.BigInteger;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Random;
import java.util.UUID;

public class IdGenerator 
{
	public static final UUID nullid = new UUID(0,0);
	
	// Upper long
	public static final long TIME_LOW_MASK = 0xFFFFFFFF00000000L;
	public static final long TIME_MID_MASK = 0x00000000FFFF0000L;
	public static final long VERSION_MASK  = 0x000000000000F000L;;
	public static final long TIME_HI_MASK  = 0x0000000000000FFFL;
	public static final long VERSION1      = 0x1000;
	
	// Lower long
	public static final long VARIANT_MASK  = 0xC000000000000000L;
	public static final long CLKSEQ_MASK   = 0x3FFF000000000000L;
	public static final long NODE_MASK     = 0x0000FFFFFFFFFFFFL;
	public static final long VARIANT1      = 0x8000000000000000L;
	
	// internal state
	private static long lasttime = 0;
	private static long counter = 0;
	private static long hwseq = 0;
	
	// Called at application start to initialize the static lower half
	static
	{
		for (int ii = 0; ii < 20; ii++)
		{
			try 
			{
				NetworkInterface ni = NetworkInterface.getByIndex(ii);
				if (ni != null) // real interface
				{
					byte[] hwaddr = ni.getHardwareAddress();
					String dname  = ni.getDisplayName();
					if (dname.startsWith("Microsoft")) continue;
					if (dname.startsWith("VMware")) continue;
					if (hwaddr == null) continue;

					// HW will be somewhat unique
					hwseq = (VARIANT1 & VARIANT_MASK)| (new Random().nextLong() & CLKSEQ_MASK) | (new BigInteger(1, hwaddr).longValue() & NODE_MASK);
					break;
				}
			} 
			catch (SocketException se) {}
		}
	}

	public synchronized static UUID generateId()
	{
		long ms = (System.currentTimeMillis() * 10000) + 0x01B21DD213814000L; // UUIDv1 uses .1uS increments from 15 Oct 1582
		if (lasttime == ms) {
			counter++; 
		} else {
			counter = 0;
		}
		lasttime = ms;
		
		//  counter acts as a 100ns timer to deal with things happening faster than Java 1ms time
		long nstime = lasttime + counter;
		long timever = ((nstime << 32) & TIME_LOW_MASK) | ((nstime >> 16) & TIME_MID_MASK) | (VERSION1 & VERSION_MASK) | ((nstime >> 48) & TIME_HI_MASK);

		return new UUID(timever, hwseq);
	}
}
