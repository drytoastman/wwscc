/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2011 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.util;

import java.io.IOException;

public class CancelException extends IOException
{
	public CancelException(){ super(); }
	public CancelException(String msg){ super(msg); }
}