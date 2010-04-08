/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2010 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.challenge;

import java.awt.Component;
import java.util.ArrayList;
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
import org.wwscc.storage.Entrant;


public class BracketingList extends BaseDialog<List<Entrant>> implements ChangeListener
{
	BracketingListModel model;
	JSpinner spinner;
	JTable table;
	int required;

	public BracketingList(int size)
	{
		super(new MigLayout("debug, fill"), false);
		required = size;
		model = new BracketingListModel();
		spinner = new JSpinner(new SpinnerNumberModel(size, size/2+1, size, 1));
		spinner.addChangeListener(this);
		
		table = new JTable(model);
		table.setDefaultRenderer(Object.class, new EntrantDisplay());
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.getColumnModel().getColumn(0).setMaxWidth(25);
		table.getColumnModel().getColumn(1).setMaxWidth(200);

		mainPanel.add(new JLabel("Number of Drivers"), "center, split");
		mainPanel.add(spinner, "center, gapbottom 10, wrap");
		mainPanel.add(new JScrollPane(table), "width 200, height 600, grow");
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
			ret.add((Entrant)model.getValueAt(index, 1));

		return ret;
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		required = ((Number)spinner.getModel().getValue()).intValue();
	}
}

class EntrantDisplay extends DefaultTableCellRenderer
{
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
                          boolean isSelected, boolean hasFocus, int row, int column)
	{
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		if (value instanceof Entrant)
		{
			Entrant e = (Entrant)value;
			setText(e.getFirstName() + " " + e.getLastName());
		}
		return this;
	}
}


class BracketingListModel extends AbstractTableModel
{

	List<Entrant> data;
	
	public BracketingListModel()
	{
		Map<Integer, Entrant> entrants = new HashMap<Integer, Entrant>();
		for (Entrant e : Database.d.getEntrantsByEvent())
			entrants.put(e.getCarId(), e);

		data = new ArrayList<Entrant>();
		for (Integer id : Database.d.loadDialins().getNetOrder())
			data.add(entrants.get(id));
	}

	@Override
	public int getRowCount()
	{
		return data.size();
	}

	@Override
	public int getColumnCount()
	{
		return 2;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		switch (columnIndex)
		{
			case 0: return rowIndex + 1;
			case 1: return data.get(rowIndex);
		}
		return null;
	}

	@Override
	public String getColumnName(int col)
	{
		if (col == 1) return "Name";
		return "";
	}
}
