/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */


package org.wwscc.protimer;

class Result
{
	public int state;
	public double rt;
	public double sixty;
	public double finish;
	public double dial;
	public double cdial;
	public double origdial;

	public static final int NORMAL = 0;
	public static final int REDLIGHT = 1;
	public static final int NOTSTAGED = 2;

	public Result()
	{
		rt = Double.NaN;
		sixty = Double.NaN;
		finish = Double.NaN;
		dial = Double.NaN;
		cdial = Double.NaN;
		origdial = Double.NaN;
		state = NORMAL;
	}

	public Result duplicate()
	{
		Result r = new Result();
		r.rt = rt;
		r.sixty = sixty;
		r.finish = finish;
		r.dial = dial;
		r.cdial = cdial;
		r.origdial = origdial;
		r.state = state;
		return r;
	}

	public boolean deleteStart()
	{
		if (!Double.isNaN(rt) && Double.isNaN(finish))
		{
			rt = Double.NaN;
			sixty = Double.NaN;
			return true;
		}
		return false;
	}

	/**
	 * Delete the finish if it exists
	 * @return if delete is successful, a duplicate of this before the delete, null otherwise
	 */
	public Result deleteFinish()
	{
		Result ret = null;
		if (!Double.isNaN(finish))
		{
			ret = duplicate();
			finish = Double.NaN;
		}

		return ret;
	}
}

