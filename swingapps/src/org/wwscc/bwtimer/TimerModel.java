/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.bwtimer;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Vector;
import java.util.logging.Logger;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import org.wwscc.storage.Run;
import org.wwscc.timercomm.RunServiceInterface;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;
import org.wwscc.util.Prefs;
import org.wwscc.util.TimerTimestamp;

/**
 *
 * @author bwilson
 */
public class TimerModel implements TableModel, TimeStorage
{
	private static Logger log = Logger.getLogger(TimerModel.class.getCanonicalName());

	private static final int TIMER_UPDATE = 99;  // timer # when just sending an update

	Vector<TableModelListener> tableListeners = new Vector<TableModelListener>();
	Vector<ListDataListener> listListeners = new Vector<ListDataListener>();
	Vector<RunServiceInterface> runListeners = new Vector<RunServiceInterface>();

	int lights;
	LinkedList<Long> timestamps[];
	long lasttick; // used to 'sync' 'live' display with hardware counter
	int start, finish;

	public TimerModel() throws IOException
	{
		setLights(Prefs.getLightCount());
		Messenger.register(MT.SERIAL_TIMESTAMP, this);
	}

	public void addRunServerListener(RunServiceInterface l)
	{
		runListeners.add(l);
	}
	
	@SuppressWarnings("unchecked")
	public void setLights(int sensorsused)
	{
		lights = sensorsused;
		timestamps = new LinkedList[lights]; 
		for (int ii = 0; ii < lights; ii++)
			timestamps[ii] = new LinkedList<Long>();
		lasttick = 0;
		start = 0;
		finish = lights-1;
		allChanged();
		Prefs.setLightCount(sensorsused);
	}

	@Override
	public void event(MT type, Object data)
	{
		switch (type)
		{
			case SERIAL_TIMESTAMP:
				TimerTimestamp t = (TimerTimestamp)data;
				if (t.getSensor() == TIMER_UPDATE)
				{
					updateRunningTimers(t.getTimestamp());
					break;
				}
				
				if (t.getSensor() > finish)
				{
					log.fine("Recevied invalid sensor number: " + t.getSensor());
					return; // invalid sensor
				}

				log.fine("Received timer " + t.getSensor() + " at " + t.getTimestamp());
				if (t.getSensor() == start)
					doStart(t.getTimestamp());
				else if (t.getSensor() == finish)
					doFinish(t.getTimestamp());
				else
					doSegment(t.getSensor(), t.getTimestamp());

				break;
		}
	}

	public void updateRunningTimers(long time)
	{
		lasttick = time;
		if (getSequencedCars() > 0)
			rowsUpdated(timestamps[finish].size(), timestamps[start].size()-1);
	}

	/*** Starts *****/
	public void fakeStart()
	{
		doStart(lasttick);
	}

	private void doStart(long time)
	{
		timestamps[start].add(time);
		int srow = timestamps[start].size() - 1;
		rowInserted(srow);
	}

	public void deleteStart()
	{
		int startsize = timestamps[start].size();
		if (startsize > timestamps[finish].size())
		{
			timestamps[start].removeLast();
			rowDeleted(startsize-1);
		}
	}

	public void reset()
	{
		setLights(lights);
	}


	//** Segments ****/
	
	private void doSegment(int s, long time)
	{
		// Segment timer, non-critical, try and keep in sync if errors
		if (timestamps[s].size() >= timestamps[s-1].size())
			return; // impossible finish/segment before previous
		timestamps[s].add(time);
		int srow = timestamps[s].size() - 1;
		rowUpdated(srow);
	}


	/*** Finishes ****/
	public void fakeFinish()
	{
		doFinish(lasttick);
	}

	private void doFinish(long time)
	{
		// Only valid if finish list is smaller than start list (active runs)
		if (timestamps[finish].size() >= timestamps[start].size()) return; // impossible finish before start
		timestamps[finish].add(time);
		int fsize = timestamps[finish].size();

		// Zero out segment times if we already got a finish, they'd be invalid now
		for (int ii = 1; ii < finish; ii++)
			while (timestamps[ii].size() < fsize)
				timestamps[ii].add(0L);

		rowUpdated(fsize-1);
		newFinish(fsize-1);
		for (RunServiceInterface l : runListeners)
			l.sendRun(getRun(fsize-1));
	}

	public void deleteFinish()
	{
		int finishsize = timestamps[finish].size();
		if (finishsize > 0)
		{
			for (RunServiceInterface l : runListeners)
				l.deleteRun(getRun(finishsize-1));
			timestamps[finish].removeLast();
			rowUpdated(finishsize-1);
			newFinish(finishsize-1);
		}
	}

	public void periodic()
	{
		if (getSequencedCars() > 0)
			rowUpdated(timestamps[lights-1].size());
	}

	public int getSequencedCars()
	{
		return timestamps[0].size() - timestamps[lights-1].size();
	}

	//***************************************************************
	// Fire off events to table/list listeners
	//

	protected void rowsUpdated(int rstart, int rend)
	{
		fireChange(new TableModelEvent(this, rstart, rend, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE),
					new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, rstart, rend));
	}

	protected void rowUpdated(int row)
	{
		fireChange(new TableModelEvent(this, row, row, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE),
					new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, row, row));
	}

	protected void rowInserted(int row)
	{
		fireChange(new TableModelEvent(this, row, row, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT),
					new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, row, row));
	}

	protected void rowDeleted(int row)
	{
		fireChange(new TableModelEvent(this, row, row, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE),
					new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, row, row));
	}

	protected void allChanged()
	{
		fireChange(new TableModelEvent(this, TableModelEvent.HEADER_ROW),
					new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, timestamps[start].size()));
	}

	protected void newFinish(int row)
	{
		fireChange(new TableModelEvent(this, row, row, TableModelEvent.ALL_COLUMNS, 42),
					new ListDataEvent(this, 42, row, row));
	}

	public void fireChange(TableModelEvent te, ListDataEvent le)
	{
		for (TableModelListener l : tableListeners)
			l.tableChanged(te);
		
		for (ListDataListener l : listListeners)
			switch (le.getType())
			{
				case ListDataEvent.CONTENTS_CHANGED: l.contentsChanged(le); break;
				case ListDataEvent.INTERVAL_REMOVED: l.intervalAdded(le); break;
				case ListDataEvent.INTERVAL_ADDED: l.intervalRemoved(le); break;
			}
	}

	
	/****************************************************************
	 * Timer Storage
	 */

	 /**
	  * @param row
	  * @return the run at the given 'row' index
	  */
	@Override
	public Run getRun(int row)
	{
		if (row >= timestamps[finish].size())
			return null;
		
		Run r = new Run((timestamps[finish].get(row) - timestamps[start].get(row))/1000.0);
		r.setCourse(0);
		if (lights > 2)
		{
			for (int ii = 0; ii < finish; ii++)
			{
				long end = timestamps[ii+1].get(row);
				long begin = timestamps[ii].get(row);
				if (begin != 0 && end != 0)
					r.setSegment(ii+1, (end - begin)/1000.0);
			}
		}
		return r;
	}

	@Override
	public int getFinishedCount()
	{
		return timestamps[finish].size();
	}

	@Override
	public void remove(int row) 
	{
		if (row >= timestamps[finish].size())
			return;

		for (int ii = 0; ii < lights; ii++)
			timestamps[ii].remove(row);
		rowDeleted(row);
	}


	/*****************************************************************
	 * Table Model - a table of doubles
	 */
	
	@Override
	public int getRowCount() 
	{
		return timestamps[start].size();
	}

	@Override
	public int getColumnCount() 
	{
		if (lights <= 2)
			return 1;
		return lights;
	}

	@Override
	public String getColumnName(int col) 
	{
		if (lights <= 2)
			col = 1; // fake for missing first column

		if (col == (lights-1))
			return "Total";
		else
			return "Segment" + (col+1);
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) 
	{
		return Double.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) 
	{
		return false;
	}

	@Override
	public Object getValueAt(int row, int col)
	{
		long a = 0;
		long b = 0;

		if (lights <= 2)
			col = 1; // fake for missing first column

		try
		{
			if (col == (lights-1)) // Get difference from finish to start
			{
				if (row >= timestamps[col].size())
					b = lasttick; //fakeCurrentTimeStamp();  // Live timer, show up to date count
				else
					b = timestamps[col].get(row);

				a = timestamps[0].get(row);
			}
			else  // Get difference between seg X timer and seg X+1 timer
			{
				b = timestamps[col+1].get(row);
				a = timestamps[col].get(row);
			}
		}
		catch (IndexOutOfBoundsException e)
		{
		}

		if ((b == 0) || (a == 0))
			return null;
		return (b-a)/1000.0;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
		throw new UnsupportedOperationException("Writing not supported.");
	}

	@Override
	public void addTableModelListener(TableModelListener l)
	{
		tableListeners.add(l);
	}

	@Override
	public void removeTableModelListener(TableModelListener l)
	{
		tableListeners.remove(l);
	}


	/*****************************************************************
	 * List Model - a list of Runs
	 */

	@Override
	public int getSize()
	{
		return timestamps[start].size();
	}

	@Override
	public Run getElementAt(int row)
	{
		if (row < timestamps[finish].size())
			return getRun(row);

		if (row >= timestamps[start].size())
			return null;

		// Live timer, show up to date double count
		long end = lasttick;
		long beg = timestamps[start].get(row);
		return new Run((end-beg)/1000.0);
	}

	@Override
	public void addListDataListener(ListDataListener l)
	{
		listListeners.add(l);
	}

	@Override
	public void removeListDataListener(ListDataListener l)
	{
		listListeners.remove(l);
	}
}
