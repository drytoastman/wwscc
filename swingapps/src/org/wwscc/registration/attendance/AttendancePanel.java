/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2013 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.registration.attendance;

import java.awt.Color;
import java.awt.Font;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import org.wwscc.components.UnderlineBorder;
import org.wwscc.registration.attendance.AttendanceResult.AttendanceResultPiece;
import org.wwscc.storage.Driver;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;
import org.wwscc.util.Prefs;

import net.miginfocom.swing.MigLayout;

/**
 * Display for attendance calculations and values
 */
public class AttendancePanel extends JPanel
{
	private static final Logger log = Logger.getLogger(AttendancePanel.class.getCanonicalName());
	
	List<AttendanceCalculation> calcs;
	List<AttendanceEntry> entries;
	
	public AttendancePanel()
	{
		super(new MigLayout("fillx, w 170"));
		setBorder(new LineBorder(Color.GRAY));		
		Messenger.register(MT.ATTENDANCE_SETUP_CHANGE, new ChangeListener());
	}
	

	class ChangeListener implements MessageListener
	{
		@Override
		public void event(MT type, Object data)
		{
			try {
				removeAll();
				calcs = Syntax.scanAll(Prefs.getAttendanceCalculations());
				entries = Attendance.scanFile(Attendance.defaultfile);
			} catch (Exception e) {
				log.severe("Failed to read attendance calculation setup: " + e.getMessage());
			}
			
			Collections.sort(calcs);
			for (AttendanceCalculation c : calcs)
			{
				add(new CalculationPanel(c), "grow, wrap");
			}
		}
	}
	
	class CalculationPanel extends JPanel
	{
		AttendanceCalculation calc;
		JLabel title, decision;
		
		public CalculationPanel(AttendanceCalculation c)
		{
			super(new MigLayout("gap 0, fill"));
			calc = c;
			title = new JLabel(c.processname);
			title.setFont(title.getFont().deriveFont(Font.BOLD, 12f));
			title.setBorder(new UnderlineBorder());
			decision = new JLabel();
			decision.setHorizontalAlignment(JLabel.CENTER);
			decision.setFont(title.getFont());
			decision.setBorder(new UnderlineBorder());

			Messenger.register(MT.DRIVER_SELECTED, new MessageListener() { public void event(MT type, Object data) { update((Driver)data); }});
		}
		
		public void update(Driver d)
		{
			removeAll();
			if (d != null)
			{
				add(title, "grow");
				add(decision, "grow, wrap");
				
				AttendanceResult result = calc.getResult(d.getFirstName(), d.getLastName(), entries);
				decision.setText(result.result ? "Yes":"No");
				for (AttendanceResultPiece p : result.pieces)
				{
					add(new JLabel(p.name), "al right");
					add(new JLabel(p.value), "gap 10px, wrap");
				}
				
				revalidate();
			}
		}
	}


}
