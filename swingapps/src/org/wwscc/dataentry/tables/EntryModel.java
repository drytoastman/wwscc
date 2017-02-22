/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dataentry.tables;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.table.AbstractTableModel;

import org.wwscc.dataentry.DataEntry;
import org.wwscc.storage.Database;
import org.wwscc.storage.Entrant;
import org.wwscc.storage.Run;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;
import org.wwscc.util.Prefs;

/*
 * The EntryModel class represents the data storage and data for the DataEntry application
 */
public class EntryModel extends AbstractTableModel implements MessageListener
{
	private static Logger log = Logger.getLogger("org.wwscc.dataentry.EntryModel");

	List<Entrant> tableData;
	Action doubleCourseMode;

	int runoffset;
	int colCount;

	public EntryModel()
	{
		super();
		tableData = null;
		runoffset = 1; /* based on number of non run columns before runs - 1 */
		colCount = 0;

		Messenger.register(MT.EVENT_CHANGED, this);
		Messenger.register(MT.RUNGROUP_CHANGED, this);
	}

	public void addCar(UUID carid)
	{
		if (tableData == null) return;

		Entrant e = Database.d.loadEntrant(DataEntry.state.getCurrentEventId(), carid, DataEntry.state.getCurrentCourse(), true);
		if (e == null)
		{
			log.warning("Failed to fetch entrant data, perhaps try again");
			return;
		}
		
		if (tableData.contains(e))
		{
			if (!Prefs.useReorderingTable())
			{
				log.log(Level.WARNING, "Carid {0} already in table, perhaps you want to enable constant staging mode", carid);
				return;
			}
			tableData.remove(e); // remove it from position and following will readd at the end
		}
		else if (Database.d.isInOrder(DataEntry.state.getCurrentEventId(), carid, DataEntry.state.getCurrentCourse()))
		{
			log.log(Level.SEVERE, "Carid {0} already in use in another rungroup", carid);
			return;
		}
		
		tableData.add(e);

		try {
			Database.d.registerCar(DataEntry.state.getCurrentEventId(), carid, false, false);
		} catch (SQLException ioe) {
			log.log(Level.INFO, "Registration during car add failed: {0}" + ioe.getMessage(), ioe);
		}
		
		int row = tableData.size();
		fireTableRowsInserted(row, row);
    	fireEntrantsChanged();
	}

	public void replaceCar(UUID carid, int row)
	{
		/* We are being asked to swap an entrant */
		Entrant old = tableData.get(row);
		Entrant newe = Database.d.loadEntrant(DataEntry.state.getCurrentEventId(), carid, DataEntry.state.getCurrentCourse(), true);
		if (newe == null)
		{
			log.warning("Failed to fetch entrant data, perhaps try again");
			return;
		}
		
		for (Run r : old.getRuns()) {
			r.updateTo(DataEntry.state.getCurrentEvent().getEventId(), newe.getCarId(), DataEntry.state.getCurrentCourse(), r.run());
			newe.setRun(r);
			Database.d.setRun(r); // insert or update 
		}
		
		tableData.set(row, newe);
		fireRunsChanged(newe);
		fireEntrantsChanged();
		fireTableRowsUpdated(row, row);
	}

	public int getRowForEntrant(Entrant find)
	{
		for (int ii = 0; ii < tableData.size(); ii++)
		{
			if (tableData.get(ii).equals(find))
				return ii;
		}
		return -1;
	}
	
	@Override
	public int getRowCount()
	{
		if (tableData == null) return 0;
		return tableData.size();
	}

	@Override
	public int getColumnCount()
	{
		return colCount;
	}

	@Override
	public String getColumnName(int col)
	{
		if (col == 0) return "Class/#";
		if (col == 1) return "Entrant";
		return "Run " + (col - runoffset);
	}

	
	/**
	 * Interface call, return the col class, either an Entrant or a Run.
	 */
	@Override
	public Class<?> getColumnClass(int col)
	{ 
		if (col <= runoffset) return Entrant.class; 
		return Run.class; 
	}


	/**
	 * Interface call returns false as we don't allows edits in the table itself.
	 */
	@Override
	public boolean isCellEditable(int row, int col)
	{
		return false;
	}


	public boolean rowIsFull(int row)
	{
		Entrant e = tableData.get(row);
		if (e == null) return true;  // ????
		return (e.runCount() >= DataEntry.state.getCurrentEvent().getRuns());
	}
	
	public boolean isBest(int row, Run x)
	{
		Entrant e = tableData.get(row);
		if (e == null) return false;

		// check if anyone is better
		Run.NetOrder no = new Run.NetOrder(DataEntry.state.getCurrentEvent());
		for (Run r : e.getRuns())
		{
			if (r == x) continue;
			if (no.compare(x, r) > 0) return false;
		}
		return true;
	}

	/**
	 * Get a value from the mode, either Entrant or Run.
	 * @param row the row of the cell
	 * @param col the col of the cell
	 * @return an Entrant, a Run or null if invalid cell
	 */
	public Object getValueAt(int row, int col)
	{
		if (tableData == null) return null;
		if (row >= tableData.size()) return null;
		if (row < 0) return null;

		Entrant e = tableData.get(row);
		if (e == null)  
		{ 
			log.info("get("+row+","+col+") e is null");
			return null; 
		} 

		if (col <= runoffset) return e; 
		return e.getRun((col-runoffset)); 
	}


	/**
	 * Set a value in the table, either an Entrant or a Run
	 * @param aValue the value to set
	 * @param row the row of the cell
	 * @param col the col of the cell (0,1 are Entrant, the rest are Run)
	 */
	@Override
	public void	setValueAt(Object aValue, int row, int col)
	{
		if (tableData == null) return;
		if (row >= tableData.size()) return;

		Entrant e = tableData.get(row);

		// Setting the car/driver value
		if (col <= runoffset)
		{
			if (aValue instanceof Entrant)
			{
				log.warning("How did you get here?");
				Thread.dumpStack();
			}
			else if (aValue == null)
			{
				/* We are being asked to delete this entry, check if they have runs first. */
				if (e.hasRuns())
				{
					log.warning("Can't remove an entrant that has runs");
					return;
				}
				
				tableData.remove(row); // remove the row which removes from runorder upon commit
				fireTableDataChanged();
			}

   			fireEntrantsChanged();
			fireTableRowsUpdated(row, row);
		}

		// Setting a run
		else 
		{
			if (aValue instanceof Run) {
				Run r = (Run)aValue;
				r.updateTo(DataEntry.state.getCurrentEvent().getEventId(), e.getCarId(), DataEntry.state.getCurrentCourse(), col-runoffset);
				e.setRun(r);
				Database.d.setRun(r);
			} else if (aValue == null){
				e.deleteRun(col-runoffset);
				Database.d.deleteRun(DataEntry.state.getCurrentEvent().getEventId(), e.getCarId(), DataEntry.state.getCurrentCourse(), col-runoffset);
			}

			fireRunsChanged(e);
			fireTableCellUpdated(row, col);
		}

	}


	/* This will only effect the 'runorder' table and nothing else */
	public void moveRow(int start, int end, int to)
	{
		if (tableData == null) return;

		int ii, a, b, c, x;
		if ((start <= to) && (to <= (end+1))) return; // Move doesn't make sense, doesn't do anything
		
		/*
			a = start of first block
			b = start of second block
			c = start of 'third' block or end of second +1
			x = location where first block will start after move
		*/
		if (to < start) // move upwards
		{
			a = to;
			b = start;
			c = end + 1;
			x = a + (c-b);
		}
		else // move downwards
		{
			a = start;
			b = end + 1;
			c = to;
			x = a + (c-b);
		}

		ArrayList<Entrant> tmp = new ArrayList<Entrant>(b - a);
		for (ii = a; ii < b; ii++) // Copy everything from block 1
			tmp.add(tableData.get(ii));

		for (ii = 0; ii < (c - b); ii++)  // Move block 2 up into block 1, index-b goes to index-a
			tableData.set(ii+a, tableData.get(ii+b));

		for (ii = 0; ii < tmp.size(); ii++) // Copy block 1 back in after moved block 2
			tableData.set(ii+x, tmp.get(ii));

		fireTableRowsUpdated(a, c-1);
    	fireEntrantsChanged();
	}


	@Override
	public void event(MT type, Object o)
	{
		switch (type)
		{
			case EVENT_CHANGED:
				colCount = DataEntry.state.getCurrentEvent().getRuns() + 2;
				fireTableStructureChanged();
				break;

			case RUNGROUP_CHANGED:
				tableData = Database.d.getEntrantsByRunOrder(DataEntry.state.getCurrentEventId(), DataEntry.state.getCurrentCourse(), DataEntry.state.getCurrentRunGroup());
				fireTableDataChanged();
				break;
		}
	}


	/* Notifying Listeners, committing */
    public void fireEntrantsChanged()
	{
		ArrayList<UUID> ids = new ArrayList<UUID>();
		for (Entrant e : tableData)
			ids.add(e.getCarId());
		Database.d.setRunOrder(DataEntry.state.getCurrentEventId(), DataEntry.state.getCurrentCourse(), DataEntry.state.getCurrentRunGroup(), ids);
		Messenger.sendEvent(MT.ENTRANTS_CHANGED, null);
	}


	/* */
	public void fireRunsChanged(Entrant e)
	{
		Messenger.sendEvent(MT.RUN_CHANGED, e);
	}
}


