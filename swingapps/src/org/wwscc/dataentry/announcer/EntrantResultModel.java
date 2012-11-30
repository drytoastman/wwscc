package org.wwscc.dataentry.announcer;

import javax.swing.table.AbstractTableModel;

import org.wwscc.storage.Entrant;
import org.wwscc.storage.Run;
import org.wwscc.util.NF;

/**
 * Data model to hold results for a single entrant
 */
public class EntrantResultModel extends AbstractTableModel
{
	Entrant data;

	public EntrantResultModel()
	{
		data = null;
	}

	public void setData(Entrant e)
	{
		data = e;
		fireTableDataChanged();
	}

	public Run getRun(int row)
	{
		if (data == null)
			return null;
		return data.getRun(row+1);
	}

	public int getRowCount()
	{
		if (data == null) return 0;
		return data.runCount();
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
		if (data == null) return "";
		r = data.getRun(row+1);
		if (r == null) return "";

		switch (col)
		{
			case 0: return NF.format(r.getRaw());
			case 1: return r.getCones();
			case 2: return r.getGates();
			case 3: return NF.format(r.getNet());
		}
		return null;
	}
}