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
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
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
	static Font titleFont = new Font("SansSerif", Font.BOLD, 10);
	static Border plainBorder = new LineBorder(Color.GRAY, 1);
	static Border nextBorder = new MatteBorder(1, 4, 1, 4, new Color(10, 200, 10));
	static Border next2Border = new MatteBorder(1, 3, 1, 3, new Color(40, 80, 10));
	
	
	JLabel reaction;
	JLabel sixty;
	TimeTextField value;
	JComboBox<Integer> cones;
	JComboBox<Integer> gates;
	JComboBox<String> status;
	JLabel rundiff;

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
		
		reaction = new JLabel("0.000");
		sixty = new JLabel("0.000");
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

		rundiff = new JLabel("0.000");
		rundiff.setFont(timeFont);

		setLayout(new MigLayout("ins 0"));
		
		add(title("start"), "center");
		add(title("time"), "center");
		add(title("cones"), "center");
		add(title("gates"), "center");
		add(title("status"), "center");
		add(title("diff"), "center, wrap");
		
		add(reaction, "flowy, split 2, gapright 5, gapleft 5");
		add(sixty, "gapleft 5");
		add(value, "");
		add(cones, "width 34!");
		add(gates, "width 34!");
		add(status, "");
		add(rundiff, "width 36!, gapleft 5, gapright 5");
	}

	private JLabel title(String s)
	{
		JLabel l = new JLabel(s);
		l.setFont(titleFont);
		return l;
	}
	
	public void updateRun()
	{
		updating = true;
		try
		{
			reaction.setText("0.000");
			sixty.setText("0.000");
			value.setText("00.000");
			cones.setSelectedIndex(0);
			gates.setSelectedIndex(0);
			status.setSelectedIndex(0);
			rundiff.setText("0.000");

			run = model.getRun(runId);
			if (run == null)
				return;

			savetime = run.getRaw();			
			reaction.setText(NF.format(run.getReaction()));
			sixty.setText(NF.format(run.getSixty()));
			value.setTime(run.getRaw());
			cones.setSelectedItem(run.getCones());
			gates.setSelectedItem(run.getGates());
			status.setSelectedItem(run.getStatus());
			diff = run.getNet() - model.getDial(entryId);
			if (diff > 900)
				rundiff.setText("DEF");
			else
				rundiff.setText(NF.format(diff));
		} 
		finally
		{
			updating = false;
		}
	}

	public void updateColor()
	{
		switch (model.getState(runId))
		{
			case ACTIVE: setBorder(nextBorder); break;
			case PENDING: setBorder(next2Border); break;
			default: setBorder(plainBorder); break;
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
