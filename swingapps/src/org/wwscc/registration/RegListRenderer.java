package org.wwscc.registration;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.wwscc.storage.MetaCar;


class RegListRenderer extends DefaultListCellRenderer
{
	private MyPanel p = new MyPanel();
	private Color superLightGray = new Color(200, 200, 200);
	
	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
		MetaCar c = (MetaCar)value;
		String myclass = c.getClassCode() + " " + c.getIndexStr();				

		if (isSelected)
		{
			p.setBackground(list.getSelectionBackground());
			p.setForeground(list.getSelectionForeground());			
		}
		else
		{	
			p.setBackground(list.getBackground());
			p.setForeground(list.getForeground());
		}

		p.label2.setText(myclass + " #" + c.getNumber() + ": " + c.getYear() + " " + c.getModel() + " " + c.getColor());
		if (c.isInRunOrder())
			p.label1.setText("In Event");
		else if (c.isRegistered())
			p.label1.setText("Registered");
		else if(c.hasActivity())
		{
			p.label1.setText("Used");
			p.label1.setForeground(superLightGray);
		}
		else
			p.label1.setText("");
		

		return p;
	}
	
}

class MyPanel extends JPanel
{
	JLabel label1;
	JLabel label2;
	
	public MyPanel()
	{
		setLayout(new MigLayout("ins 0", "[85!][100:500:10000]", "[20!]"));
		
		label1 = new JLabel();
		label1.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
		add(label1, "gap left 5, ay center");
		
		label2 = new JLabel();
		label2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		add(label2, "ay center");
	}
	
	@Override
	public void setForeground(Color f)
	{
		super.setForeground(f);
		if (label1 != null) label1.setForeground(f);
		if (label2 != null) label2.setForeground(f);
	}
}
