/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dataentry;

import java.util.Vector;
import javax.swing.AbstractListModel;
import org.wwscc.bwtimer.TimeStorage;
import org.wwscc.storage.Run;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;

/**
 *
 * @author bwilson
 */
public class SimpleTimeListModel extends AbstractListModel<Run> implements TimeStorage
{
	Vector<Run> data;
	int forCourse;
	
	/**
	 * Create a new time list that is linked to a course
	 * @param course the course number
	 */
	public SimpleTimeListModel(int course)
	{
		data = new Vector<Run>();
		forCourse = course;
		Messenger.register(MT.TIMER_SERVICE_RUN, this);
		Messenger.register(MT.TIMER_SERVICE_DELETE, this);
		Messenger.register(MT.SERIAL_TIMER_DATA, this);
	}

	@Override
	public void event(MT type, Object o)
	{
		if (!(o instanceof Run))
			return;

		Run r = (Run)o;
		if ((forCourse > 0) && (r.course() != forCourse))
			return;
		if (r.getRaw() < 1)
			return;

		switch (type)
		{
			case SERIAL_TIMER_DATA:
				data.add(r);
				fireIntervalAdded(this, data.size()-1, data.size()-1);
				break;

			case TIMER_SERVICE_RUN:
				data.add(r);
				fireIntervalAdded(this, data.size()-1, data.size()-1);
				break;

			case TIMER_SERVICE_DELETE:
				data.remove(r);
				fireIntervalRemoved(this, data.size(), data.size());
				break;
		}
	}

	@Override
	public Run getElementAt(int row)
	{
		return data.get(row);
	}

	@Override
	public int getSize()
	{
		return data.size();
	}


	@Override
	public Run getRun(int row)
	{
		return data.get(row);
	}

	@Override
	public int getFinishedCount()
	{
		return data.size();
	}

	@Override
	public void remove(int row)
	{
		data.remove(row);
		fireIntervalRemoved(this, row, row);
	}
}
