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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.JCheckBox;
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
import org.wwscc.util.NF;



public class BracketingList extends BaseDialog<List<BracketEntry>> implements ChangeListener
{
	BracketingListModel model;
	JSpinner spinner;
	JCheckBox ladiesCheck;
	JCheckBox openCheck;
	JCheckBox bonusCheck;
	
	JTable table;
	int required;
	
	public BracketingList(String cname, int size)
	{
		super(new MigLayout("fill"), false);

		model = new BracketingListModel();
		required = size;
		
		spinner = new JSpinner(new SpinnerNumberModel(size, size/2+1, size, 1));
		spinner.addChangeListener(this);
		
		ladiesCheck = new JCheckBox("Ladies Classes", true);
		ladiesCheck.addChangeListener(this);
		
		openCheck = new JCheckBox("Open Classes", true);
		openCheck.addChangeListener(this);
		
		bonusCheck = new JCheckBox("Bonus Style Dialins", true);		
		bonusCheck.addChangeListener(this);
		
		table = new JTable(model);
		table.setAutoCreateRowSorter(true);
		table.setDefaultRenderer(Double.class, new D3Renderer());
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.getColumnModel().getColumn(0).setMaxWidth(50);
		table.getColumnModel().getColumn(1).setMaxWidth(200);
		table.getColumnModel().getColumn(2).setMaxWidth(200);
		table.getColumnModel().getColumn(3).setMaxWidth(75);
		table.getColumnModel().getColumn(4).setMaxWidth(75);

		mainPanel.add(new JLabel("Number of Drivers"), "split");
		mainPanel.add(spinner, "gapbottom 10, wrap");

		mainPanel.add(ladiesCheck, "wrap");
		mainPanel.add(openCheck, "wrap");
		mainPanel.add(bonusCheck, "gapbottom 10, wrap");
		
		mainPanel.add(new JLabel("Click on column header to sort"), "center, wrap");
		mainPanel.add(new JScrollPane(table), "width 400, height 600, grow");
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
	public List<BracketEntry> getResult()
	{
		if (!valid)
			return null;

		List<BracketEntry> ret = new ArrayList<BracketEntry>();
		for (int index : table.getSelectedRows())
			ret.add(model.getBracketEntry(table.convertRowIndexToModel(index)));

		// sort by their net times
		final Dialins dial = Database.d.loadDialins(ChallengeGUI.state.getCurrentEventId());
		Collections.sort(ret, new Comparator<BracketEntry>() {
			public int compare(BracketEntry o1, BracketEntry o2) {
				return Double.compare(dial.getNet(o1.entrant.getCarId()), dial.getNet(o2.entrant.getCarId()));
			}
		});

		return ret;
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		if (e.getSource() == spinner)
			required = ((Number)spinner.getModel().getValue()).intValue();
		else
			model.reload(openCheck.isSelected(), ladiesCheck.isSelected(), bonusCheck.isSelected());
	}
}
class D3Renderer extends DefaultTableCellRenderer
{
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
                          boolean isSelected, boolean hasFocus, int row, int column)
	{
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		if (value instanceof Double)
			setText(NF.format((Double)value));
		else
			setText(""+value);
		return this;
	}
}


class BracketingListModel extends AbstractTableModel
{
	List<Store> data;
	final static class Store
	{
		Entrant entrant;
		int netposition;
		double nettime;
		double dialin;
	}

	public BracketingListModel()
	{
		reload(true, true, true);
	}
	
	public void reload(boolean useOpen, boolean useLadies, boolean bonusStyle)
	{
		Map<UUID, Entrant> entrants = new HashMap<UUID, Entrant>();
		for (Entrant e : Database.d.getEntrantsByEvent(ChallengeGUI.state.getCurrentEventId()))
		{
			if ((useLadies && (e.getClassCode().startsWith("L"))) ||
				(useOpen && (!e.getClassCode().startsWith("L"))))
				entrants.put(e.getCarId(), e);
		}

		data = new ArrayList<Store>();
		Dialins d = Database.d.loadDialins(ChallengeGUI.state.getCurrentEventId());
		int pos = 1;
		for (UUID id : d.getNetOrder())
		{
			Store s = new Store();
			if (!entrants.containsKey(id))
				continue;
			s.entrant = entrants.get(id);
			s.netposition = pos;
			s.nettime = d.getNet(id);
			s.dialin = d.getDial(id, bonusStyle);
			data.add(s);
			pos++;
		}
		
		fireTableDataChanged();
	}

	@Override
	public int getRowCount()
	{
		return data.size();
	}

	@Override
	public int getColumnCount()
	{
		return 6;
	}

	public BracketEntry getBracketEntry(int rowIndex)
	{
		Store s = data.get(rowIndex);
		return new BracketEntry(null, s.entrant, s.dialin);
	}

	@Override
	public Class<?> getColumnClass(int col)
	{
		switch (col)
		{
			case 0: return Integer.class;
			case 1: return String.class;
			case 2: return String.class;
			case 3: return String.class;
			case 4: return Double.class;
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
			case 3: return s.entrant.getClassCode();
			case 4: return s.nettime;
			case 5: return s.dialin;
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
			case 3: return "Class";
			case 4: return "Net";
			case 5: return "Dialin";
			default: return "ERROR";
		}
	}
}
