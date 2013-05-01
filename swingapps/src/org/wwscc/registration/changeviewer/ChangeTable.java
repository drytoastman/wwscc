package org.wwscc.registration.changeviewer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.List;

import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import org.wwscc.components.DriverCarPanel;
import org.wwscc.storage.Car;
import org.wwscc.storage.Change;
import org.wwscc.storage.Driver;
import org.wwscc.util.TableUtilities;

class ChangeTable extends JTable
{
	public ChangeTable()
	{
		super();
		setDefaultRenderer(Object.class, new TextAreaCellRenderer());
		setBackground(new Color(235,235,245));
	}

	public void setData(List<Change> changes)
	{
		DefaultTableModel model = new DefaultTableModel(new Object[] { "Item", "Type", "Args" }, 0) {
			public boolean isCellEditable(int x, int y) { return false; }
		};
		
		int row = 0;
		for (Change c : changes) {
			String type = c.getSqlMap();
			Serializable args[] = c.getArgs();
			Object data[] = new Object[] { ++row, type, null };
			
			if (type.equals("REGISTERCAR"))
				data[2] = String.format("carid=%d paid=%b", (Object[])args);
			else if (type.equals("UNREGISTERCAR"))
				data[2] = String.format("carid=%d", args[0]);
			else if (type.equals("INSERTCAR") || type.equals("UPDATECAR") || type.equals("DELETECAR"))
				data[2] = String.format("%s\n[driverid=%d]", DriverCarPanel.carDisplay((Car)args[0]), ((Car)args[0]).getDriverId());
			else if (type.equals("INSERTDRIVER") || type.equals("UPDATEDRIVER"))
				data[2] = DriverCarPanel.driverDisplay((Driver)args[0]);
			else
				data[2] = args[0];
			
			model.addRow(data);
		}

		setModel(model);
		getColumnModel().getColumn(0).setPreferredWidth(50);
		getColumnModel().getColumn(1).setPreferredWidth(180);
		getColumnModel().getColumn(2).setPreferredWidth(700);
		TableUtilities.setPreferredRowHeights(this);
	}
}


/**
 *  Render with JTextArea so we can use output with newlines.
 */
class TextAreaCellRenderer implements TableCellRenderer
{
	JTextArea stamp = new JTextArea();
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column)
	{
		if (isSelected)
		{
			stamp.setForeground(table.getSelectionForeground());
			stamp.setBackground(table.getSelectionBackground());
		}
		else
		{
			stamp.setForeground(table.getForeground());
			stamp.setBackground(table.getBackground());
		}
		stamp.setText(value.toString());
		return stamp;
	}
	
	// Following DefaultTableCellRender method for performance when using as a stamp
    public void invalidate() {}
    public void validate() {}
    public void revalidate() {}
    public void repaint(long tm, int x, int y, int width, int height) {}
    public void repaint(Rectangle r) { }
    public void repaint() {}
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) { }
}
