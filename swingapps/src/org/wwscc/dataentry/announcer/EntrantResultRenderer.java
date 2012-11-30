package org.wwscc.dataentry.announcer;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.wwscc.storage.Run;

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
		Run run = m.getRun(r);
		if ((run != null) && (run.getNetOrder() == 1))
		{
			cell.setFont(AnnouncerPanel.boldFont);
			cell.setBackground(AnnouncerPanel.grayBackground);
		}
		else if ((run != null) && (run.getRawOrder() == 1))
		{
			cell.setFont(AnnouncerPanel.boldFont);
			cell.setBackground(AnnouncerPanel.redBackground);
		}
		else
		{
			cell.setFont(AnnouncerPanel.regularFont);
			cell.setBackground(Color.WHITE);
		}

		return cell;
	}
}