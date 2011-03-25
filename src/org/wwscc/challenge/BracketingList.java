/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2010 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.challenge;

import java.awt.Component;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import net.miginfocom.swing.MigLayout;
import org.wwscc.dialogs.BaseDialog;
import org.wwscc.storage.Database;
import org.wwscc.storage.Dialins;
import org.wwscc.storage.Entrant;


public class BracketingList extends BaseDialog<List<Entrant>> implements ChangeListener
{
	BracketingListModel model;
	JSpinner spinner;
	JTable table;
	int required;

	public BracketingList(int size)
	{
		super(new MigLayout("fill"), false);
		required = size;
		model = new BracketingListModel();
		spinner = new JSpinner(new SpinnerNumberModel(size, size/2+1, size, 1));
		spinner.addChangeListener(this);
		
		table = new JTable(model);
		table.setAutoCreateRowSorter(true);
		table.setDefaultRenderer(Double.class, new D3Renderer());
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.getColumnModel().getColumn(0).setMaxWidth(50);
		table.getColumnModel().getColumn(1).setMaxWidth(200);
		table.getColumnModel().getColumn(2).setMaxWidth(200);
		table.getColumnModel().getColumn(3).setMaxWidth(75);

		mainPanel.add(new JLabel("Number of Drivers"), "center, split");
		mainPanel.add(spinner, "center, gapbottom 10, wrap");
		mainPanel.add(new JLabel("Click on column header to sort"), "center, wrap");
		mainPanel.add(new JScrollPane(table), "width 300, height 600, grow");
	}
	
	@Override
	public boolean verifyData()
	{
		int size = table.getSelectedRows().length;
		if (required != size)
		{
			errorMessage = "Must select " + required + " drivers, you've selected " + size;
			return false;
		}
		return true;
	}

	@Override
	public List<Entrant> getResult()
	{
		if (!valid)
			return null;

		List<Entrant> ret = new ArrayList<Entrant>();
		for (int index : table.getSelectedRows())
			ret.add((Entrant)model.getEntrantAt(table.convertRowIndexToModel(index)));

		final Dialins dial = Database.d.loadDialins();
		Collections.sort(ret, new Comparator<Entrant>() {
			public int compare(Entrant o1, Entrant o2) {
				return Double.compare(dial.getNet(o1.getCarId()), dial.getNet(o2.getCarId()));
			}
		});

		return ret;
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		required = ((Number)spinner.getModel().getValue()).intValue();
	}
}

class D3Renderer extends DefaultTableCellRenderer
{
	NumberFormat df;
	public D3Renderer()
	{
		df = NumberFormat.getNumberInstance();
		df.setMinimumFractionDigits(3);
		df.setMaximumFractionDigits(3);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
                          boolean isSelected, boolean hasFocus, int row, int column)
	{
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		setText(df.format(value));
		return this;
	}
}


class BracketingListModel extends AbstractTableModel
{
	List<Store> data;
	class Store
	{
		Entrant entrant;
		int netposition;
		double nettime;
	}
	
	public BracketingListModel()
	{
		Map<Integer, Entrant> entrants = new HashMap<Integer, Entrant>();
		for (Entrant e : Database.d.getEntrantsByEvent())
			entrants.put(e.getCarId(), e);

		data = new ArrayList<Store>();
		Dialins d = Database.d.loadDialins();
		int pos = 1;
		for (Integer id : d.getNetOrder())
		{
			Store s = new Store();
			s.entrant = entrants.get(id);
			s.netposition = pos;
			s.nettime = d.getNet(id);
			data.add(s);
			pos++;
		}
		
	}

	@Override
	public int getRowCount()
	{
		return data.size();
	}

	@Override
	public int getColumnCount()
	{
		return 4;
	}

	public Entrant getEntrantAt(int rowIndex)
	{
		return data.get(rowIndex).entrant;
	}

	@Override
	public Class getColumnClass(int col)
	{
		switch (col)
		{
			case 0: return Integer.class;
			case 1: return String.class;
			case 2: return String.class;
			case 3: return Double.class;
		}
		return Object.class;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		Store s = data.get(rowIndex);
		switch (columnIndex)
		{
			case 0: return s.netposition;
			case 1: return s.entrant.getFirstName();
			case 2: return s.entrant.getLastName();
			case 3: return s.nettime;
		}
		return null;
	}

	@Override
	public String getColumnName(int col)
	{
		switch (col)
		{
			case 0: return "Pos";
			case 1: return "First";
			case 2: return "Last";
			case 3: return "Time";
			default: return "ERROR";
		}
	}
}
