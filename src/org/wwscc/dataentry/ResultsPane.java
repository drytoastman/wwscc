/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */


package org.wwscc.dataentry;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.wwscc.storage.Database;
import org.wwscc.storage.Entrant;
import org.wwscc.storage.EventResult;
import org.wwscc.storage.Run;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;


/** 
 * Overall class that represents the panel in the list of tabs
 */
public class ResultsPane extends JPanel implements MessageListener
{
	static NumberFormat df;
	static
	{
		df = NumberFormat.getNumberInstance();
		df.setMinimumFractionDigits(3);
		df.setMaximumFractionDigits(3);
	}

	JLabel nameLabel;
	JLabel classLabel;
	AnnouncerStrings details;

	JTable nameTable;
	JTable classTable;

	EntrantResultModel nameModel;
	EventResultModel classModel;

	HighlightRenderer renderer;

	JPanel upperPanel;
	DecimalFormat diffForm;

	public ResultsPane()
	{
		super(new BorderLayout());
		Messenger.register(MT.RUN_CHANGED, this);

		diffForm = new DecimalFormat("##0.000");
		diffForm.setPositivePrefix("+");

		nameLabel = new JLabel("", SwingConstants.CENTER);
		nameLabel.setFont(new Font("dialog", Font.BOLD, 16));
		nameLabel.setBorder(new LineBorder(Color.GRAY));
		nameLabel.setAlignmentX(0.5f);
		nameLabel.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));

		nameModel = new EntrantResultModel();

		nameTable = new JTable(nameModel);
		nameTable.setDefaultRenderer(Object.class, new EntrantResultRenderer());
		nameTable.setRowHeight(20);
		nameTable.setAlignmentX(0.5f);
		nameTable.setRowSelectionAllowed(false);
		nameTable.setColumnSelectionAllowed(false);
		TableColumnModel cm1 = nameTable.getColumnModel();
		cm1.getColumn(0).setPreferredWidth(250);
		cm1.getColumn(1).setPreferredWidth(80);
		cm1.getColumn(2).setPreferredWidth(200);
		cm1.getColumn(3).setPreferredWidth(250);
		cm1.getColumn(4).setPreferredWidth(80);

		JLabel announcerLabel = new JLabel("Run Results", JLabel.CENTER);
		announcerLabel.setFont(new Font("dialog", Font.BOLD, 16));
		announcerLabel.setBorder(new LineBorder(Color.GRAY));
		announcerLabel.setAlignmentX(0.5f);
		announcerLabel.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));

		details = new AnnouncerStrings();
		details.setAlignmentX(0.5f);

		classLabel = new JLabel("", SwingConstants.CENTER);
		classLabel.setFont(new Font("dialog", Font.BOLD, 16));
		classLabel.setBorder(new LineBorder(Color.GRAY));
		classLabel.setText("Class Results");
		classLabel.setAlignmentX(0.5f);
		classLabel.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));

		classModel = new EventResultModel();
		classTable = new JTable(classModel);

		renderer = new HighlightRenderer();
		classTable.setDefaultRenderer(Object.class, renderer);
		classTable.setRowHeight(20);
		classTable.setRowSelectionAllowed(false);
		classTable.setColumnSelectionAllowed(false);

		TableColumnModel cm = classTable.getColumnModel();
		cm.getColumn(0).setPreferredWidth(50);
		cm.getColumn(1).setPreferredWidth(500);
		cm.getColumn(2).setPreferredWidth(250);
		cm.getColumn(3).setPreferredWidth(250);

		upperPanel = new JPanel();
		upperPanel.setLayout(new BoxLayout(upperPanel, BoxLayout.PAGE_AXIS));
		upperPanel.add(nameLabel);
		upperPanel.add(nameTable.getTableHeader());
		upperPanel.add(nameTable);
		
		upperPanel.add(announcerLabel);
		upperPanel.add(Box.createRigidArea(new Dimension(0,5)));
		upperPanel.add(details);
		upperPanel.add(Box.createRigidArea(new Dimension(0,20)));
		upperPanel.add(classLabel);
		upperPanel.setOpaque(true); 
		upperPanel.setBackground(Color.WHITE);

		add(upperPanel, BorderLayout.NORTH);
		add(new JScrollPane(classTable), BorderLayout.CENTER);
	}

	@Override
	public void event(MT type, Object o)
	{
		switch (type)
		{
			case RUN_CHANGED:
				Entrant e = (Entrant)o;
				String classcode = e.getClassCode();

				nameLabel.setText("Driver: " + e.getName());
				nameModel.setData(e);

				details.setData(e.getRuns());
				
				classLabel.setText("Class: " + classcode);
				classModel.setData(Database.d.getResultsForClass(classcode));
				renderer.setHighlightValue(e.getFirstName() + " " + e.getLastName());

				break;
		}
	} 


	class AnnouncerStrings extends JLabel
	{
		public AnnouncerStrings()
		{
			super("", CENTER);
			setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
		}

		public void setData(Run[] r)
		{
			if ((r == null) || (r.length <= 1))
			{
				setText("");
				return;
			}

			Run one, two, last;

			one = two = last = r[r.length-1];
			for (int ii = 0; ii < r.length; ii++)
			{
				if (r[ii] == null)
					continue;

				if (r[ii].getNetOrder() == 1)
					one = r[ii];
				else if (r[ii].getNetOrder() == 2)
					two = r[ii];

				//if (r[ii].iorder == 1)
				//	raw = r[ii];
			}

			double ndiff,rdiff;

			String txt = "<HTML>";
			if (last.run() != one.run())
			{
				ndiff = last.getNet() - one.getNet();
				rdiff = last.getRaw() - one.getRaw();
				if (rdiff < 0)
				{
					txt += "Run "+last.run()+" raw is " + df.format(-rdiff) + " faster than run "+one.run();
					txt += "<br>However, net is " + df.format(ndiff) + " slower";
				}
				else
				{
					txt += "Run "+last.run()+" raw is " + df.format(rdiff) + " slower than run "+one.run();
					if (ndiff != rdiff)
						txt += "<br>Net is " + df.format(ndiff) + " slower";
				}
			}
			else
			{
				ndiff = two.getNet() - last.getNet();
				rdiff = two.getRaw() - last.getRaw();
				if (rdiff < 0)
				{
					txt += "Run "+last.run()+" raw is " + df.format(-rdiff) + " slower than run "+two.run();
					txt += "<br>However, net is " + df.format(ndiff) + " faster";
				}
				else
				{
					txt += "Run "+last.run()+" raw is " + df.format(rdiff) + " faster than run "+two.run();
					if (ndiff != rdiff)
						txt += "<br>Net is " + df.format(ndiff) + " faster";
				}
			}

			setText(txt);
		}
	}


	/**
	 * render for entrant results table
	 */
	class EntrantResultRenderer extends DefaultTableCellRenderer
	{
		String match;
		Font regular;
		Font bold;
		Color mygray;

		public EntrantResultRenderer()
		{
			super();
			match = "";
			regular = (Font)UIManager.get("Label.font");
			bold = ((Font)UIManager.get("Label.font")).deriveFont(Font.BOLD, 12.0f);
			mygray = new Color(240,240,240);
		}

		public Component getTableCellRendererComponent (JTable t, Object o, boolean is, boolean hf, int r, int c)
		{
			JLabel cell = (JLabel)super.getTableCellRendererComponent(t, o, is, hf, r, c);
			cell.setHorizontalAlignment(JLabel.CENTER);

			EntrantResultModel m = (EntrantResultModel)t.getModel();
			Run run = m.getRun(r);
			if ((run != null) && (run.getNetOrder() == 1))
			{
				cell.setFont(bold);
				cell.setBackground(mygray);
			}
			else
			{
				cell.setFont(regular);
				cell.setBackground(Color.WHITE);
			}

			return cell;
		}
	}


	/**
	 * Special render that causes the row that was just updated to be bold and of a larger font
	 */
	class HighlightRenderer extends DefaultTableCellRenderer
	{
		String match;
		Font regular;
		Font bold;
		Color mygray;

		public HighlightRenderer()
		{
			super();
			match = "";
			regular = (Font)UIManager.get("Label.font");
			bold = ((Font)UIManager.get("Label.font")).deriveFont(Font.BOLD, 12.0f);
			mygray = new Color(240,240,240);
		}

		public void setHighlightValue(String s)
		{
			match = s;
		}

		public Component getTableCellRendererComponent (JTable t, Object o, boolean is, boolean hf, int r, int c)
		{
			Component cell = super.getTableCellRendererComponent(t, o, is, hf, r, c);

			EventResultModel m = (EventResultModel)t.getModel();
			String s = (String)m.getValueAt(r, 1);
			if (s.equals(match))
			{
				cell.setFont(bold);
				cell.setBackground(mygray);
			}
			else
			{
				cell.setFont(regular);
				cell.setBackground(Color.WHITE);
			}

			return cell;
		}
	}


	/**
	 * Data model to hold results for a single entrant
	 */
	class EntrantResultModel extends AbstractTableModel
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
			return 5;
		}

		public String getColumnName(int col)
		{
			switch (col)
			{
				case 0: return "Raw";
				case 2: return "Pen";
				case 3: return "Net";
			}
			return "";
		}

		public Object getValueAt(int row, int col)
		{
			Run r = null;
			Run last = null;

			if (data == null) return "";
			r = data.getRun(row+1);
			if (r == null) return "";

			if (row > 0) last = data.getRun(row);

			switch (col)
			{
				case 0: return df.format(r.getRaw());
				case 2: return new String("("+r.getCones()+","+r.getGates()+")");
				case 3: return df.format(r.getNet());

				case 1:
					if (r.getRawOrder() == 1)
						return "*";
					else
						return "";

				case 4:
					if (r.getNetOrder() <= 2)
						return r.getNetOrder();
					else
						return "";
			}
			return null;
		}
	}


	/**
	 * Data model to hold results for a single class
	 */
	class EventResultModel extends AbstractTableModel
	{
		List<EventResult> data;

		public EventResultModel()
		{
			data = new ArrayList<EventResult>();
		}

		public void setData(List<EventResult> v)
		{
			data = v;
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
				case 2: return "Sum";
				case 3: return "Diff";
			}
			return "";
		}

		public Object getValueAt(int row, int col)
		{
			EventResult e = data.get(row);
			switch (col)
			{
				case 0: return new Integer(row+1);
				case 1: return e.getFullName();
				case 2: return df.format(e.getSum());
				case 3: return df.format(e.getDiff());
			}
			return null;
		}
	}
}


