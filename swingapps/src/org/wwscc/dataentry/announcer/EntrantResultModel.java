package org.wwscc.dataentry.announcer;

import javax.swing.table.AbstractTableModel;

import org.wwscc.storage.AnnouncerData;
import org.wwscc.storage.Entrant;
import org.wwscc.storage.Run;
import org.wwscc.util.NF;

/**
 * Data model to hold results for a single entrant
 */
public class EntrantResultModel extends AbstractTableModel
{
	Run runs[];
	String labels[];

	public EntrantResultModel()
	{
		runs = null;
	}

	public void setData(Entrant e, AnnouncerData a)
	{
		/* FINISH ME
		runs = e.getRuns();
		labels = new String[runs.length];
		
		for (int ii = 0; ii < runs.length; ii++)
        {
			if (runs[ii] == null)
				continue;
            if (runs[ii].getNetOrder() == 1) 
            	labels[ii] = "current";
            if (runs[ii].getNetOrder() == 2 && a.getOldSum() > 0)
            	labels[ii] = "old";
        }
        
        if (runs[runs.length-1].getNetOrder() != 1 && a.getPotentialSum() > 0)
        	labels[runs.length-1] = "raw";
		*/
		fireTableDataChanged();
	}

	public String getLabel(int row)
	{
		if (labels == null)
			return null;
		return labels[row];
	}

	public int getRowCount()
	{
		if (runs == null) return 0;
		return runs.length;
	}

	public int getColumnCount()
	{
		return 4;
	}

	public String getColumnName(int col)
	{
		switch (col)
		{
			case 0: return "Raw";
			case 1: return "C";
			case 2: return "G";
			case 3: return "Net";
		}
		return "";
	}

	public Object getValueAt(int row, int col)
	{
		Run r = null;
		if (runs == null) return "";
		r = runs[row];
		if (r == null) return "";

		switch (col)
		{
			case 0: return NF.format(r.getRaw());
			case 1: return r.getCones();
			case 2: return r.getGates();
			case 3: return 0; // FINISH ME return NF.format(r.getNet());
		}
		return null;
	}
}