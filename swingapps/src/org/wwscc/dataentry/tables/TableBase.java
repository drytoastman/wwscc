/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */
package org.wwscc.dataentry.tables;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;

/**
 * Include common functions between Run and Driver tables
 */
public class TableBase extends JTable
{
	protected int startModelColumn;
	protected int endModelColumn;
	
	public TableBase(TableModel m, TableCellRenderer renderer, TransferHandler transfer, int start, int end)
	{
		super(m);

		startModelColumn = start;
		endModelColumn = end;
		setDefaultRenderer(Object.class, renderer);
		setTransferHandler(transfer);
		setAutoCreateColumnsFromModel( false );
		addMouseListener(new DClickWatch());
		getTableHeader().setReorderingAllowed(false);
		setCellSelectionEnabled(true);
		setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		setRowHeight(36);
	}
	
	/*** automatic adjust of column widths ****/
	
	public void setColumnSizes(TableColumnModelEvent e)
	{
		// to be overriden if desired
	}
	
	public void setColumnWidths(TableColumn col, int min, int pref, int max)
	{
		if (col == null) return;
		col.setMinWidth(min);
		col.setPreferredWidth(pref);
		col.setMaxWidth(max);
	}

	@Override
	public void columnAdded(TableColumnModelEvent e)
	{
		setColumnSizes(e);
		super.columnAdded(e);
	}

	@Override
	public void columnRemoved(TableColumnModelEvent e)
	{
		setColumnSizes(e);
		super.columnRemoved(e);
	}	
	
	
	/**** Scrolling ****/
	
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
	
	/*** Double and single click notifications ****/
	
	class DClickWatch extends MouseAdapter
	{
		protected Object getObject()
		{
			int row = TableBase.this.getSelectedRow();
			int col = TableBase.this.getSelectedColumn();
			return getValueAt(row, col);
		}

		@Override
		public void mousePressed(MouseEvent e)
		{
			Messenger.sendEvent(MT.OBJECT_CLICKED, getObject());
		}

		@Override
		public void mouseClicked(MouseEvent e)
		{
			if (e.getClickCount() == 2)
				Messenger.sendEvent(MT.OBJECT_DCLICKED, getObject());
		}
	}
	
	
	/***
	 * Intercept changes to model so we can re-add our columns
	 * @param e 
	 */
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
				for (int ii = startModelColumn; ii < m.getColumnCount() && ii < endModelColumn; ii++) {
					TableColumn newColumn = new TableColumn(ii);
					addColumn(newColumn);
				}
			}
		}
		
		super.tableChanged(e);
	}
	
	/**
	 * Simple class for DnD data transfer, only usable inside the application.
	 */
	public static class SimpleDataTransfer implements Transferable, ClipboardOwner
	{
		DataFlavor myFlavor;
		Object data;

		public SimpleDataTransfer(DataFlavor flavor, Object o) { myFlavor = flavor; data = o; }
		@Override
		public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[] { myFlavor }; }
		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) { return flavor.equals(myFlavor); }
		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException { return data; }
		@Override
		public void lostOwnership(Clipboard clipboard, Transferable contents) {}
	}
}
