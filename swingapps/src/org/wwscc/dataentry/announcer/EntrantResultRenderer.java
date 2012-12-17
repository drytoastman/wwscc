package org.wwscc.dataentry.announcer;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * render for entrant results table
 */
class EntrantResultRenderer extends DefaultTableCellRenderer
{
	public EntrantResultRenderer()
	{
		super();
	}

	public Component getTableCellRendererComponent (JTable t, Object o, boolean is, boolean hf, int r, int c)
	{
		JLabel cell = (JLabel)super.getTableCellRendererComponent(t, o, is, hf, r, c);
		cell.setHorizontalAlignment(JLabel.CENTER);

		EntrantResultModel m = (EntrantResultModel)t.getModel();
		String label = m.getLabel(r);
		if (label != null)
		{
			if (label.equals("old"))
			{
				cell.setFont(AnnouncerPanel.boldFont);
				cell.setForeground(AnnouncerPanel.blueStroke);
				cell.setBackground(AnnouncerPanel.blueBackground);
				return cell;
			}
			else if (label.equals("current"))
			{
				cell.setFont(AnnouncerPanel.boldFont);
				cell.setForeground(Color.BLACK);
				cell.setBackground(AnnouncerPanel.grayBackground);
				return cell;
			}
			else if (label.equals("raw"))
			{
				cell.setFont(AnnouncerPanel.boldFont);
				cell.setForeground(AnnouncerPanel.redStroke);
				cell.setBackground(AnnouncerPanel.redBackground);
				return cell;
			}
		}

		cell.setFont(AnnouncerPanel.regularFont);
		cell.setForeground(Color.BLACK);
		cell.setBackground(Color.WHITE);
		return cell;
	}
}