/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2013 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.registration.attendance;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
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
public class AttendancePanel extends JPanel implements MessageListener
{
	private static final Logger log = Logger.getLogger(AttendancePanel.class.getCanonicalName());
	
	public static final Font titleFont = ((Font)UIManager.getFont("Label.font")).deriveFont(Font.BOLD, 14f);
	public static final Font filterFont = ((Font)UIManager.getFont("Label.font")).deriveFont(Font.BOLD, 12f);
	public static final Font variableFont = ((Font)UIManager.getFont("Label.font")).deriveFont(12f);
	
	List<AttendanceCalculation> calcs;
	List<AttendanceEntry> entries;
	
	public AttendancePanel()
	{
		super(new MigLayout("fillx, w 170"));
		setBorder(new LineBorder(Color.GRAY));		
		Messenger.register(MT.ATTENDANCE_SETUP_CHANGE, this);
		Messenger.register(MT.DRIVER_SELECTED, this);
	}
	
	@Override
	public void event(MT type, Object data)
	{
		switch (type)
		{
			case ATTENDANCE_SETUP_CHANGE:
				try {
					removeAll();
					calcs = Syntax.scanAll(Prefs.getAttendanceCalculations());
					entries = Attendance.scanFile(Attendance.defaultfile);
				} catch (Exception e) {
					log.severe("Failed to read attendance calculation setup: " + e.getMessage());
				}
				
				Collections.sort(calcs);
				for (AttendanceCalculation c : calcs)
					add(new CalculationPanel(c), "grow, wrap");
				break;
				
			case DRIVER_SELECTED:
				for (Component c : AttendancePanel.this.getComponents())
					((CalculationPanel)c).update((Driver)data);
				break;
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
			title.setFont(titleFont);
			title.setBorder(new UnderlineBorder());
			decision = new JLabel();
			decision.setHorizontalAlignment(JLabel.CENTER);
			decision.setFont(titleFont);
			decision.setBorder(new UnderlineBorder());
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
					if (p.value == null)
					{
						JLabel n = new JLabel(p.name);
						n.setFont(filterFont);
						
						add(n, "spanx 2, center, wrap");
					}
					else
					{
						JLabel n = new JLabel(p.name);
						n.setFont(variableFont);
						JLabel v = new JLabel(p.value);
						v.setFont(variableFont);
						
						add(n, "al right");
						add(v, "gap 10px, wrap");
					}
				}
				
				revalidate();
			}
		}
	}


}
