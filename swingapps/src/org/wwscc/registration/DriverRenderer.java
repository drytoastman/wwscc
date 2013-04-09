package org.wwscc.registration;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import org.wwscc.storage.Driver;

public class DriverRenderer extends DefaultListCellRenderer
{
	Font font = getFont().deriveFont(14f);
	Font offFont = font.deriveFont(Font.ITALIC);
	
	Color offSelect = new Color(200, 200, 200);
	public Component getListCellRendererComponent(JList<?> list, Object o, int i, boolean selected, boolean c)
	{
		super.getListCellRendererComponent(list, o, i, selected, c);
		
		if (o instanceof Driver)
		{
			setFont(font);
			setText(((Driver)o).getFullName());
			if (selected)
			{
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());			
			}
			else
			{
				setForeground(list.getForeground());
				setBackground(list.getBackground());
			}
		}
		else
		{
			setFont(offFont);
			setText(o.toString());
			if (selected)
			{
				setForeground(list.getSelectionForeground());
				setBackground(offSelect);
			}
			else
			{
				setForeground(Color.GRAY);
				setBackground(list.getBackground());
			}
		}
		
		return this;
	}
}
