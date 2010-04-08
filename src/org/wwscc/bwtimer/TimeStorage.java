/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2009 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.bwtimer;

import javax.swing.ListModel;
import org.wwscc.storage.Run;
import org.wwscc.util.MessageListener;

/**
 * @author bwilson
 */
public interface TimeStorage extends ListModel, MessageListener
{
	public Run getRun(int row);
	public int getFinishedCount();
	public void remove(int row);
}
