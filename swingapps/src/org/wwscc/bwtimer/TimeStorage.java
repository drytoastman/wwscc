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
 */
public interface TimeStorage extends ListModel<Run>, MessageListener
{
	/**
	 * Get the run value at this row
	 * @param row the row to find
	 * @return a Run object
	 */
	public Run getRun(int row);
	
	/**
	 * @return the number of time entries in this model
	 */
	public int getFinishedCount();
	
	/**
	 * @param row the row to remove from the model.
	 */
	public void remove(int row);
}
