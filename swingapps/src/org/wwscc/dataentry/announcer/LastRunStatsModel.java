package org.wwscc.dataentry.announcer;

import javax.swing.table.AbstractTableModel;

import org.wwscc.storage.EventResult;
import org.wwscc.util.NF;

public class LastRunStatsModel extends AbstractTableModel
{
	public static final int FIRST=1, NEXT=0, RAW=2, NET=3; 

	Double rawImprovement = null;
	Double netImprovement = null;
	EventResult myresult = null;
	EventResult topresult = null;
	boolean showLast = true;
	
	@Override
	public int getRowCount() { return showLast ? 4 : 2; }
	@Override
	public int getColumnCount() { return 2; }
	@Override
	public Object getValueAt(int row, int col)
	{
		if (col == 0)
		{
			switch (row)
			{
				case FIRST: return "From First (Raw)";
				case  NEXT: return "From Next (Raw)";
				case  RAW: return "Raw Improvement";
				case  NET: return "Net Improvement";
			}
		}
		else
		{
			if (myresult == null)
				return "";
			switch (row)
			{
				case NEXT:
					if (myresult.getPosition() != 1)
						return NF.format(myresult.getDiff()/myresult.getIndex());
					return "";
				case FIRST: 
					if (myresult.getPosition() != 1)
						return NF.format((myresult.getSum() - topresult.getSum())/myresult.getIndex());
					return "";
				case RAW: 
					if ((rawImprovement != null))
						return NF.format(rawImprovement);
					return "";
				case NET: 
					if ((netImprovement != null))
						return NF.format(netImprovement);
					return "";
			}
		}

		return "";
	}
	
	public void setData(EventResult top, EventResult mine, Double rawI, Double netI, boolean showlast)
	{
		rawImprovement = rawI;
		netImprovement = netI;
		myresult = mine;
		topresult = top;
		showLast = showlast;
		fireTableDataChanged();
	}
}
