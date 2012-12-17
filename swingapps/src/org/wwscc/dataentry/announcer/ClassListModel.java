package org.wwscc.dataentry.announcer;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.wwscc.storage.EventResult;
import org.wwscc.util.NF;

/**
 * Data model to hold results for a single class
 */
public class ClassListModel extends AbstractTableModel
{
	List<EventResult> data;
	List<Double> toFirstRaw;
	List<Double> toFirstIndex;

	public ClassListModel()
	{
		data = new ArrayList<EventResult>();
		toFirstRaw = new ArrayList<Double>();
		toFirstIndex = new ArrayList<Double>();
	}

	public EventResult getResultAtRow(int r)
	{
		return data.get(r);
	}
	
	public void setData(List<EventResult> v)
	{
		if (v != null)
		{
			data = v;
			toFirstRaw = new ArrayList<Double>();
			toFirstIndex = new ArrayList<Double>();
			double topindex;
			if (data.size() > 0)
				topindex = data.get(0).getSum();
			else
				topindex = 0;
			
			for (EventResult r : data)
			{
				double tofirst = r.getSum() - topindex;
				toFirstIndex.add(tofirst);
				toFirstRaw.add(tofirst/r.getIndex());
			}
		}
		fireTableDataChanged();
	}

	public int getRowCount()
	{
		return data.size();
	}

	public int getColumnCount()
	{
		return 4;
	}

	public String getColumnName(int col)
	{
		switch (col)
		{
			case 0: return "";
			case 1: return "Name";
			case 2: return "";
			case 3: return "Net";
		}
		return "";
	}

	public Object getValueAt(int row, int col)
	{
		EventResult e = data.get(row);
		switch (col)
		{
			case 0:
				if (e instanceof FakeResult)
					return ((FakeResult) e).getType();
				return e.getPosition();
			case 1: return e.getFullName();
			case 2: return e.getIndexStr();
			case 3: return NF.format(e.getSum());
		}
		return null;
	}
}