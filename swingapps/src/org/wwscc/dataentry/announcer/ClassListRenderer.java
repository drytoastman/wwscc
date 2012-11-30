package org.wwscc.dataentry.announcer;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.wwscc.storage.Entrant;
import org.wwscc.storage.EventResult;

/**
 * Special render that causes the row that was just updated to be bold and of a larger font
 */
public class ClassListRenderer extends DefaultTableCellRenderer
{
	Entrant match;	
	public ClassListRenderer()
	{
		super();
		match = null;
	}

	public void setHighlightValue(Entrant e)
	{
		match = e;
	}

	public Component getTableCellRendererComponent (JTable t, Object o, boolean is, boolean hf, int r, int c)
	{
		Component cell = super.getTableCellRendererComponent(t, o, is, hf, r, c);

		ClassListModel m = (ClassListModel)t.getModel();
		EventResult er = m.getResultAtRow(r);
		
		cell.setForeground(Color.BLACK);
		
		if (er instanceof FakeResult)
		{
			FakeResult fr = (FakeResult)er;
			if (fr.getType().equals("raw"))
			{
				cell.setBackground(AnnouncerPanel.redBackground);
				cell.setForeground(AnnouncerPanel.redStroke);
				cell.setFont(AnnouncerPanel.boldFont);				
			}
			else
			{
				cell.setBackground(AnnouncerPanel.blueBackground);
				cell.setForeground(AnnouncerPanel.blueStroke);
				cell.setFont(AnnouncerPanel.boldFont);
			}
		}
		else if ((match != null) && (er.getCarId() == match.getCarId()))
		{
			cell.setFont(AnnouncerPanel.boldFont);
			cell.setBackground(AnnouncerPanel.grayBackground);
		}
		else
		{
			cell.setFont(AnnouncerPanel.regularFont);
			cell.setBackground(Color.WHITE);
		}

		return cell;
	}
}
