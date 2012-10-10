/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */
package org.wwscc.challenge.viewer;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import net.miginfocom.swing.MigLayout;
import org.wwscc.challenge.ChallengeModel;
import org.wwscc.challenge.Id;
import org.wwscc.storage.ChallengeRun;
import org.wwscc.util.NF;
import org.wwscc.util.TimeTextField;

/**
 */
class RunDisplay extends JComponent
{
	static Font timeFont = new Font("SansSerif", Font.PLAIN, 12);
	static Color next = new Color(10, 150, 10);
	static Color next2 = new Color(150, 50, 0);
	
	TimeTextField value;
	JComboBox<Integer> cones;
	JComboBox<Integer> gates;
	JComboBox<String> status;
	JLabel info;

	ChallengeModel model;
	ChallengeRun run;
	Id.Entry entryId;
	Id.Run runId;		
	double diff;
	double savetime;
	boolean updating;

	public RunDisplay(ChallengeModel m, Id.Entry eid, Id.Run rid)
	{
		runId = rid;
		run = null;
		diff = Double.NaN;
		model = m;
		entryId = eid;
		savetime = 0;
		updating = false;

		InternalListener l = new InternalListener();
		
		value = new TimeTextField("00.000", 5);
		value.addFocusListener(l);
		value.setFont(timeFont);
		cones = new JComboBox<Integer>(new Integer[] {0, 1, 2, 3, 4, 5});
		cones.addActionListener(l);
		gates = new JComboBox<Integer>(new Integer[] {0, 1, 2, 3, 4, 5});
		gates.addActionListener(l);
		status = new JComboBox<String>(new String[] {"OK", "RL", "NS", "DNF"});
		status.addActionListener(l);
		
		Font f = new Font("SansSerif", Font.PLAIN, 10);
		cones.setFont(f);
		gates.setFont(f);
		status.setFont(f);

		info = new JLabel("0.000");
		info.setFont(timeFont);

		setLayout(new MigLayout(""));
		setBorder(new SoftBevelBorder(BevelBorder.RAISED));

		add(value, "");
		add(cones, "width 34!");
		add(gates, "width 34!");
		add(status, "");
		add(info, "");
	}

	public void updateRun()
	{
		updating = true;
		try
		{
			value.setText("00.000");
			cones.setSelectedIndex(0);
			gates.setSelectedIndex(0);
			status.setSelectedIndex(0);
			info.setText("0.000");

			run = model.getRun(runId);
			if (run == null)
				return;

			savetime = run.getRaw();
			value.setTime(run.getRaw());
			cones.setSelectedItem(run.getCones());
			gates.setSelectedItem(run.getGates());
			status.setSelectedItem(run.getStatus());
			diff = run.getNet() - model.getDial(entryId);
			info.setText(NF.format(diff));
		} 
		finally
		{
			updating = false;
		}
	}

	public void updateColor()
	{
		setBackground(state2Color(model.getState(runId)));
	}

	private Color state2Color(ChallengeModel.RunState s)
	{
		switch (s)
		{
			case ACTIVE: return next;
			case PENDING: return next2;
			default: return Color.GRAY;
		}
	}

	/**
	 * Hide listener from outsiders
	 */
	class InternalListener implements ActionListener, FocusListener
	{
		@Override
		public void actionPerformed(ActionEvent e) 
		{
			if (updating)
				return;
			Object o = e.getSource();
			if (o == cones)
				model.setCones(runId, (Integer)cones.getSelectedItem());
			else if (o == gates)
				model.setGates(runId, (Integer)gates.getSelectedItem());
			else if (o == status)
				model.setStatus(runId, (String)status.getSelectedItem());
		}
	
		@Override
		public void focusGained(FocusEvent arg0) {}

		@Override
		public void focusLost(FocusEvent arg0)
		{
			if (updating) return;
			if (value.getTime() == savetime) return;
			model.setTime(runId, value.getTime());
		}
	}
}
