/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2010 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author bwilson
 */
public class CountedDataInputStream
{
	protected InputStream in;
	protected int counter;

    public CountedDataInputStream(InputStream i)
	{
		in = i;
		counter = 0;
    }

	public int getCounter()
	{
		return counter;
	}

    public byte[] readByteArray(int size) throws IOException
	{
		byte data[] = new byte[size];
		if (size == 0)
			return data;
		int read = in.read(data);
		if (read != size) throw new EOFException();
		counter += size;
		return data;
    }

    public boolean readBoolean() throws IOException
	{
		int ch = in.read();
		if (ch < 0) throw new EOFException();
		counter++;
		return (ch != 0);
    }

    public byte readByte() throws IOException
	{
		int ch = in.read();
		if (ch < 0) throw new EOFException();
		counter++;
		return (byte)(ch);
    }

    public short readShort() throws IOException
	{
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0) throw new EOFException();
		counter += 2;
        return (short)((ch1 << 8) + (ch2 << 0));
    }

    public int readInt() throws IOException
	{
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
		counter += 4;
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    public long readLong() throws IOException 
	{
		long ch1 = in.read();
		long ch2 = in.read();
		long ch3 = in.read();
		long ch4 = in.read();
		long ch5 = in.read();
		long ch6 = in.read();
		long ch7 = in.read();
		long ch8 = in.read();
		if ((ch1 | ch2 | ch3 | ch4 | ch5 | ch6 | ch7 | ch8) < 0) throw new EOFException();
		counter += 8;

		return ((ch1 << 56) + (ch2 << 48) + (ch3 << 40) + (ch4 << 32) +
		 (ch5 << 24) + (ch6 << 16) + (ch7 << 8) + (ch8 << 0));
    }

    public double readDouble() throws IOException
	{
		return Double.longBitsToDouble(readLong());
    }
}
