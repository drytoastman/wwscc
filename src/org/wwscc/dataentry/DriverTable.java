/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2010 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dataentry;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import org.wwscc.storage.Entrant;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;





/**
 * Table used for DataEntry 
 */
public class DriverTable extends JTable implements MessageListener
{
	ListSelectionModel rowSel;
	ListSelectionModel colSel;

	EntryModel model;
	String activeSearch;
	
	public DriverTable(EntryModel m)
	{
		super(m);
		setAutoCreateColumnsFromModel( false );

		model = m;
		activeSearch = "";

		rowSel = getSelectionModel();
		colSel = getColumnModel().getSelectionModel();

		/* Selection and DnD/cut/paste */
		setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		setCellSelectionEnabled(true);
		setDragEnabled(true);
		setDropMode(DropMode.INSERT);
		setTransferHandler(new DriverTransferHandler());

		/* Drawing and other misc drawing stuff */
		getTableHeader().setReorderingAllowed(false);
		setDefaultRenderer(Entrant.class, new EntrantRenderer());
		setRowHeight(36);
		
		
		InputMap im = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "cut"); // delete is same as Ctl+X

		addMouseListener(new DClickWatch());
		getTableHeader().addMouseListener( new RowHeaderTableResizer() );
		
		Messenger.register(MT.CAR_ADD, this);
		Messenger.register(MT.CAR_CHANGE, this);
		Messenger.register(MT.FIND_ENTRANT, this);
	}


	class DClickWatch extends MouseAdapter implements ActionListener
	{
		JPopupMenu driverPopup;
		JPopupMenu runPopup;
		Entrant selectedE;

		public DClickWatch()
		{
			driverPopup = new JPopupMenu("");
			driverPopup.add(createItem("Cut", KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK)));
			driverPopup.add(createItem("Add Text Runs", null));
			selectedE = null;
			//copy = createItem("Copy", KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
			//paste = createItem("Paste", KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
		}

		protected JMenuItem createItem(String title, KeyStroke ks)
		{
			JMenuItem item = new JMenuItem(title);
			item.addActionListener(this);
			if (item != null) item.setAccelerator(ks);
			return item;
		}

		protected Object getObject()
		{
			int row = rowSel.getMinSelectionIndex();
			int col = colSel.getMinSelectionIndex();
			return getValueAt(row, col);
		}

		public void doPopup(MouseEvent e)
		{
			int row = rowAtPoint(e.getPoint());
			int col = columnAtPoint(e.getPoint());
			if ((row == -1) || (col == -1)) return;
			if (!rowSel.isSelectedIndex(row)) return;
			if (!colSel.isSelectedIndex(col)) return;
			
			selectedE = (Entrant)getValueAt(row, col);
			driverPopup.show(DriverTable.this, e.getX(), e.getY());
		}
		
		@Override
		public void mousePressed(MouseEvent e)
		{
			Messenger.sendEvent(MT.OBJECT_CLICKED, getObject());
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
		public void mouseClicked(MouseEvent e)
		{
			if (e.getClickCount() == 2)
				Messenger.sendEvent(MT.OBJECT_DCLICKED, getObject());
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			String cmd = e.getActionCommand();
			if (cmd.equals("Cut"))
			{
				e.setSource(DriverTable.this); // redirect as cut action on Table
				TransferHandler.getCutAction().actionPerformed(e);
			}
		}
		


	}

	class ScrollMe implements Runnable
	{
		public int row;
		public int col;
		public ScrollMe(int r, int c) { row = r; col = c; }
		public void run() { scrollRectToVisible(getCellRect(row, col, true)); };
	}

	public void scrollTable(int row, int col)
	{
		SwingUtilities.invokeLater( new ScrollMe(row, col) );
	}
	
	@Override
	public boolean getScrollableTracksViewportWidth() 
	{
		if (getParent() instanceof JViewport)
		{
			return (((JViewport)getParent()).getWidth() > getMinimumSize().width);
		}

		return false;
	}

	public void setColumnWidths(TableColumn col, int min, int pref, int max)
	{
		if (col == null) return;
		col.setMinWidth(min);
		col.setPreferredWidth(pref);
		col.setMaxWidth(max);
	}

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
				model.addCar((Integer)o);
				scrollTable(getRowCount(), 0);
				break;

			case CAR_CHANGE:
				int row = rowSel.getMinSelectionIndex();
				if ((row >= 0) && (row < getRowCount()))
					model.replaceCar((Integer)o, row);
				break;

			case FIND_ENTRANT:
				activeSearch = (String)o;
				repaint();
				break;
		}
	}
	
	@Override
	public void tableChanged(TableModelEvent e)
	{
		if (e == null || e.getFirstRow() == TableModelEvent.HEADER_ROW)
		{
			TableColumnModel tcm = getColumnModel();
			TableModel m = getModel();
			
			if (m != null)
			{
				// Remove any current columns
				while (tcm.getColumnCount() > 0) {
					tcm.removeColumn(tcm.getColumn(0));
				}

				// Create new columns from the data model info
				for (int ii = 0; ii < m.getColumnCount() && ii < 2; ii++) {
					TableColumn newColumn = new TableColumn(ii);
					addColumn(newColumn);
				}
			}
		}
		
		super.tableChanged(e);
	}
}


class RowHeaderTableResizer extends MouseAdapter
{
	// extra adapter
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
 * Basically, this has boiled down to allow only drag movements (insertions)
 * in the driver column and copy/cut/paste in the runs columns
 */
class DriverTransferHandler extends TransferHandler
{
	private static Logger log = Logger.getLogger(DriverTransferHandler.class.getCanonicalName());
	private int[] rowsidx = null;
	//private int[] colsidx = null;
	private boolean isCut = false;


	@Override
	public int getSourceActions(JComponent c)
	{
		return COPY_OR_MOVE;
	}


	@Override
	public void exportAsDrag(JComponent comp, InputEvent e, int action)
	{
		log.fine("export as drag");
		isCut = false;
		super.exportAsDrag(comp, e, action);
	}

	
	@Override
	public void exportToClipboard(JComponent comp, Clipboard cb, int action)
	{
		isCut = true;
		log.fine("export to clipboard");
		super.exportToClipboard(comp, cb, action);
	}

	/******* Export Side *******/

	/* Create data from the selected rows and columns */
	@Override
	protected Transferable createTransferable(JComponent c)
	{
		JTable table = (JTable)c;
		rowsidx = table.getSelectedRows();

		Entrant store[] = new Entrant[rowsidx.length];
		for (int ii = 0; ii < rowsidx.length; ii++)
			store[ii] = (Entrant)table.getValueAt(rowsidx[ii], 0);

		return new EntrantDataTransfer(store);
	}

	
	@Override
	protected void exportDone(JComponent c, Transferable data, int action)
	{
		if (rowsidx == null)
			return;
		if (rowsidx.length == 0)
			return;

		/* MOVE means Drag or cut (use isCut to determine) */
		if ((action == MOVE) && isCut)
		{
			DriverTable t = (DriverTable)c;
			log.fine("cut driver");
			for (int ii = 0; ii < rowsidx.length; ii++)
				t.setValueAt(null, rowsidx[ii], 0);
		}

		rowsidx = null;
	}


	/******* Import Side *******/

	/* Called to allow drop operations */
	@Override
	public boolean canImport(TransferHandler.TransferSupport support)
	{
		JTable.DropLocation dl = (JTable.DropLocation)support.getDropLocation();
		JTable target = (JTable)support.getComponent();

		 // allow driver drag full range of rows except for last (Add driver box)
		if (dl.getRow() > target.getRowCount()) return false;  
		return true;
	}


	/* Called for drop and paste operations */
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

/**
 * Class used for data transfer during Drag/Drop/Copy
 */
class EntrantDataTransfer implements Transferable, ClipboardOwner
{
	Entrant data[];
	String string;
	static DataFlavor myFlavor;

	static
	{
		myFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + "; class=java.lang.Object", "EntrantArray");
	}

	public EntrantDataTransfer(Entrant data[])
	{
		int ii, jj;

		this.data = data;
		this.string = new String();
		for (ii = 0; ii < data.length; ii++)
		{
			this.string += data[ii] + "\n";
		}
	}

	@Override
	public DataFlavor[] getTransferDataFlavors()
	{
		DataFlavor[] flavors = new DataFlavor[2];
		flavors[0] = myFlavor;
		flavors[1] = DataFlavor.stringFlavor;
		return flavors;
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor)
	{
		return (flavor.equals(myFlavor) || flavor.equals(DataFlavor.stringFlavor));
	}

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
	{
		if (flavor.equals(myFlavor))
			return data;
		return string;
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents)
	{
	}
}
