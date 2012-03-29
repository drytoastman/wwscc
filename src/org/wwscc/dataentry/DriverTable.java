/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2010 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dataentry;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.DropMode;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.wwscc.dataentry.TableBase.SimpleDataTransfer;
import org.wwscc.dialogs.TextRunsDialog;
import org.wwscc.storage.Database;
import org.wwscc.storage.Entrant;
import org.wwscc.storage.Run;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;


/**
 * Table showing the driver entries.  Takes two columns and is placed into the scroll panel row header
 */
public class DriverTable extends TableBase implements MessageListener
{
	String activeSearch;
	
	public DriverTable(EntryModel m)
	{
		super(m, new EntrantRenderer(), new DriverTransferHandler(), 0, 2);
		activeSearch = "";

		setDragEnabled(true);
		setDropMode(DropMode.INSERT);
		
		InputMap im = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "cut"); // delete is same as Ctl+X

		addMouseListener(new ContextMenu());
		getTableHeader().addMouseListener( new RowHeaderTableResizer() );
		
		Messenger.register(MT.CAR_ADD, this);
		Messenger.register(MT.CAR_CHANGE, this);
		Messenger.register(MT.FIND_ENTRANT, this);
	}

	@Override
	public void setColumnSizes(TableColumnModelEvent e)
	{
		TableColumnModel tcm = (TableColumnModel)e.getSource();
		int cc = tcm.getColumnCount();
		if (cc <= 1) return;
		
		setColumnWidths(tcm.getColumn(0), 40, 60, 75);
		setColumnWidths(tcm.getColumn(1), 80, 250, 400);
		doLayout();
	}

	@Override
	public void event(MT type, Object o)
	{
		switch (type)
		{
			case CAR_ADD:
				Sounds.playBlocked();
				((EntryModel)getModel()).addCar((Integer)o);
				scrollTable(getRowCount(), 0);
				break;

			case CAR_CHANGE:
				int row = getSelectedRow();
				if ((row >= 0) && (row < getRowCount()))
					((EntryModel)getModel()).replaceCar((Integer)o, row);
				break;

			case FIND_ENTRANT:
				activeSearch = (String)o;
				repaint();
				break;
		}
	}
	
	
	/**
	 * Create a simple context menu for the driver columns to allow pasting
	 * of textual run data from other sources (like national pro solo)
	 */
	class ContextMenu extends MouseAdapter implements ActionListener
	{
		JPopupMenu driverPopup;
		JPopupMenu runPopup;
		Entrant selectedE;

		public ContextMenu()
		{
			driverPopup = new JPopupMenu("");
			JMenuItem item = new JMenuItem("Add Text Runs");
			item.addActionListener(this);
			driverPopup.add(item);
			selectedE = null;
		}

		public void doPopup(MouseEvent e)
		{
			int row = rowAtPoint(e.getPoint());
			int col = columnAtPoint(e.getPoint());
			if ((row == -1) || (col == -1)) return;
			if (DriverTable.this.getSelectedRow() != row) return;
			if (DriverTable.this.getSelectedColumn() != col) return;
			
			selectedE = (Entrant)getValueAt(row, col);
			driverPopup.show(DriverTable.this, e.getX(), e.getY());
		}
		
		@Override
		public void mousePressed(MouseEvent e)
		{
			if (e.isPopupTrigger())
				doPopup(e);
		}

		@Override
		public void mouseReleased(MouseEvent e)
		{
			if (e.isPopupTrigger())
				doPopup(e);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			TextRunsDialog trd = new TextRunsDialog();
			trd.doDialog("Textual Run Input", null);
			List<Run> runs = trd.getResult();
			if (runs == null) return;
			Map<Integer, Run> course1, course2;
			course1 = new HashMap<Integer,Run>();
			course2 = new HashMap<Integer,Run>();

			Database.d.setCurrentCourse(1);
			Entrant ent = Database.d.loadEntrant(selectedE.getCarId(), false);
			for (Run r : trd.getResult())
			{
				r.updateTo(Database.d.getCurrentEvent().getId(), r.course(), r.run(), selectedE.getCarId(), ent.getIndex());
				if (r.course() == 2) course2.put(r.run(), r); else course1.put(r.run(), r);
			}

			ent.setRuns(course1);
			if (course2.size() > 0)
			{
				Database.d.setCurrentCourse(2);
				ent = Database.d.loadEntrant(selectedE.getCarId(), false);
				ent.setRuns(course2);
			}

			Database.d.setCurrentCourse(1);
			Messenger.sendEvent(MT.RUNGROUP_CHANGED, 1);
		}
	}
}


/**
 * Special mouse listener that lets the user adjust the width of the row table header in a scroll
 * pane which is where this static two column driver table is placed.
 */
class RowHeaderTableResizer extends MouseAdapter
{
	TableColumn column;
	int columnWidth;
	int pressedX;
		
	@Override
	public void mousePressed(MouseEvent e)
	{
		JTableHeader header = (JTableHeader)e.getComponent();
		TableColumnModel tcm = header.getColumnModel();
		int columnIndex = tcm.getColumnIndexAtX( e.getX() );
		Cursor cursor = header.getCursor();

		if (columnIndex == tcm.getColumnCount() - 1
		&&  cursor == Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR))
		{
			column = tcm.getColumn( columnIndex );
			columnWidth = column.getWidth();
			pressedX = e.getX();
			header.addMouseMotionListener( this );
		}
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		JTableHeader header = (JTableHeader)e.getComponent();
		header.removeMouseMotionListener( this );
	}

	@Override
	public void mouseDragged(MouseEvent e)
	{
		int width = columnWidth - pressedX + e.getX();
		column.setPreferredWidth( width );
		JTableHeader header = (JTableHeader)e.getComponent();
		JTable table = header.getTable();
		table.setPreferredScrollableViewportSize(table.getPreferredSize());
		JScrollPane scrollPane = (JScrollPane)table.getParent().getParent();
		scrollPane.revalidate();
	}
}


/**
 * Render for both columns in the driver table, it differs its display based
 * column being 0 or 1.
 */
class EntrantRenderer extends JComponent implements TableCellRenderer 
{
	private Color background;
	private Color backgroundSelect;
	private Color backgroundFound;
	private Color backgroundFoundSelect;
	private String topLine;
	private String bottomLine;
	private Font topFont;
	private Font bottomFont;
	
	public EntrantRenderer()
	{
		super();
		background = new Color(240, 240, 240);
		backgroundSelect = new Color(120, 120, 120);
		backgroundFound = new Color(255, 255, 220);
		backgroundFoundSelect = new Color(255, 255, 120);
		topLine = null;
		bottomLine = null;
		
		topFont = new Font(Font.DIALOG, Font.BOLD, 11);
		bottomFont = new Font(Font.DIALOG, Font.PLAIN, 11);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
						boolean isSelected, boolean hasFocus, int row, int column) 
	{
		setBackground((isSelected) ?  backgroundSelect : background);

		if (value instanceof Entrant)
		{
			Entrant e = (Entrant)value;
		 	switch (column)
			{
				case 0:
					topLine = e.getClassCode();
					bottomLine = ""+e.getNumber();
					break;

				case 1:
					topLine = e.getFirstName() + " " + e.getLastName();
					String index = e.getIndexCode();
					if (index.equals(""))
						bottomLine = e.getCarDesc();
					else
						bottomLine = e.getCarDesc() + " ("+index+") ";
					break;

				default:	
					topLine = "What?";
					bottomLine = null;
					break;
			}

			if (matchMe(topLine, bottomLine, ((DriverTable)table).activeSearch))
				setBackground((isSelected) ?  backgroundFoundSelect : backgroundFound);
		}
		else if (value != null)
		{
			setBackground(Color.red);
			topLine = value.toString();
		}
		else
		{
			setBackground(Color.red);
			topLine = "ERROR";
			bottomLine = "No data for this cell";
		}
		return this;
	}

	protected boolean matchMe(String top, String bottom, String search)
	{
		if (search.equals("")) return false;
		for (String p : search.toLowerCase().split("\\s+"))
		{
			if ((!top.toLowerCase().contains(p)) &&
				(!bottom.toLowerCase().contains(p))) return false;
		}
		return true;
	}

	@Override
	public void paint(Graphics g1)
	{
		Graphics2D g = (Graphics2D)g1;

		Dimension size = getSize();
		g.setColor(getBackground());
		g.fillRect(0, 0, size.width, size.height);
		g.setColor(new Color(40,40,40));
		
		FontMetrics tm = g.getFontMetrics(topFont);
		FontMetrics bm = g.getFontMetrics(bottomFont);
		
		if (topLine != null)
		{
			g.setFont(topFont);
			g.drawString(topLine, 5, size.height/2 - 2);
		}
		if (bottomLine != null)
		{
			g.setFont(bottomFont);
			g.drawString(bottomLine, 5, size.height/2 + bm.getHeight() - 2);
		}
	}
	
	// The following methods override the defaults for performance reasons
	@Override
	public void validate() {}
	@Override
	public void revalidate() {}
	@Override
	protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
	@Override
	public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}
}



/**
 * Class to enable special DnD handling in our JTable.
 * Allow only cut drag movements (insertions) in the driver columns
 */
class DriverTransferHandler extends TransferHandler
{
	private static Logger log = Logger.getLogger(DriverTransferHandler.class.getCanonicalName());
	private static DataFlavor flavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + "; class=org.wwscc.storage.Driver", "DriverData");
	private int[] rowsidx = null;
	private boolean isCut = false;

	@Override
	public int getSourceActions(JComponent c)
	{
		return COPY_OR_MOVE;
	}

	@Override
	public void exportAsDrag(JComponent comp, InputEvent e, int action)
	{
		isCut = false;
		super.exportAsDrag(comp, e, action);
	}
	
	@Override
	public void exportToClipboard(JComponent comp, Clipboard cb, int action)
	{
		isCut = true;
		super.exportToClipboard(comp, cb, action);
	}

	/******* Export Side *******/

	/* Create data from the selected rows */
	@Override
	protected Transferable createTransferable(JComponent c)
	{
		JTable table = (JTable)c;
		rowsidx = table.getSelectedRows();

		Entrant store[] = new Entrant[rowsidx.length];
		for (int ii = 0; ii < rowsidx.length; ii++)
			store[ii] = (Entrant)table.getValueAt(rowsidx[ii], 0);

		return new SimpleDataTransfer(flavor, store);
	}

	
	@Override
	protected void exportDone(JComponent c, Transferable data, int action)
	{
		if ((rowsidx == null)|| (rowsidx.length == 0))
			return;

		/* use isCut to determine if we cut or were just dragging columns around */
		if (isCut)
		{
			DriverTable t = (DriverTable)c;
			log.fine("cut driver");
			for (int ii = 0; ii < rowsidx.length; ii++)
				t.setValueAt(null, rowsidx[ii], 0);
		}

		rowsidx = null;
	}

	/******* Import Side *******/

	/**
	 * Called to allow drop operations, allow driver drag full range of rows
	 * except for last (Add driver box).
	 */
	@Override
	public boolean canImport(TransferHandler.TransferSupport support)
	{
		JTable.DropLocation dl = (JTable.DropLocation)support.getDropLocation();
		JTable target = (JTable)support.getComponent();

		if (dl.getRow() > target.getRowCount()) return false;  
		return true;
	}


	/**
	 * Called for drop and paste operations 
	 */
	@Override
	public boolean importData(TransferHandler.TransferSupport support)
	{
		try
		{
			JTable target = (JTable)support.getComponent();
			EntryModel model = (EntryModel)target.getModel();
		
			if (support.isDrop())
			{
				JTable.DropLocation dl = (JTable.DropLocation)support.getDropLocation();
				model.moveRow(rowsidx[0], rowsidx[rowsidx.length-1], dl.getRow());
				target.clearSelection();
			}

			return true;
		}
		catch (Exception e) { log.warning("General error during driver drag:" + e); }

		return false;
	}
}
