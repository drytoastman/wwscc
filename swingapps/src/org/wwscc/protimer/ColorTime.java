/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */


package org.wwscc.protimer;

public class ColorTime
{
	public double time;
	public double dial;
	public int state;

	public static final int NORMAL = 0;
	public static final int REDLIGHT = 1;
	public static final int NOTSTAGED = 2;

	public ColorTime()
	{
		time = Double.NaN;
		dial = Double.NaN;
		state = 0;
	}

	public ColorTime(double inTime)
	{
		time = inTime;
		dial = 0.0;
		state = 0;
	}
	
	public ColorTime(double inTime, int inState)
	{
		time = inTime;
		dial = Double.NaN;
		state = inState;
	}

	public String getColorString()
	{
		switch (state)
		{
			case REDLIGHT: return "RED";
			case NOTSTAGED: return "BLUE";
			default: return "BLACK";
		}
	}

	public String getColorMsg()
	{
		switch (state)
		{
			case ColorTime.REDLIGHT: return "RedLight"; 
			case ColorTime.NOTSTAGED: return "Not Staged";
		}
		return "Error";
	}
}

