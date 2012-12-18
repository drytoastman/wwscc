package org.wwscc.registration;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import org.wwscc.storage.MetaCar;


class RegListRenderer extends DefaultListCellRenderer
{
	private Font font = getFont().deriveFont(12f);
	private Color red = Color.RED;
	private Color hired = new Color(255, 220, 220);
	
	// ugly snot but dual label jpanel gave me a two hour layout auto resizing headache and I gave up
	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		setFont(font);
		
		MetaCar c = (MetaCar)value;
		String myclass = c.getClassCode() + " " + c.getIndexStr();				
		String t = myclass + " #" + c.getNumber() + ": " + c.getYear() + " " + c.getModel() + " " + c.getColor();
		if (c.hasActivity() | c.isInRunOrder())
		{
			setText("<html><b>In Use &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</b> " + t);
			if (isSelected) setForeground(hired);
			else setForeground(red);
		}
		else if (c.isRegistered())
			setText("<html><b>Registered</b> " + t);
		else
			setText("<html><b>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</b>" + t);
		return this;
	}
}
