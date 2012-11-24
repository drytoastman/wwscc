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
import javax.swing.JPanel;
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
	static Font reactionFont = new Font("SansSerif", Font.PLAIN, 12);
	static Font timeFont = new Font("SansSerif", Font.PLAIN, 14);
	static Font titleFont = new Font("SansSerif", Font.BOLD, 10);
	static Font bigTitleFont = new Font("SansSerif", Font.BOLD, 12);
	static Font comboFont = new Font("SansSerif", Font.PLAIN, 10);
	
	static Border plainBorder = new LineBorder(Color.GRAY, 1);
	static Border nextBorder = new MatteBorder(1, 4, 1, 4, new Color(10, 200, 10));
	static Border next2Border = new MatteBorder(1, 3, 1, 3, new Color(40, 80, 10));
	
	FocusButton nextButton;
	JPanel runPanel;
	JLabel reaction;
	JLabel sixty;
	TimeTextField value;
	JComboBox<Integer> cones;
	JComboBox<Integer> gates;
	JComboBox<String> status;
	JLabel rundiff;

	ChallengeModel model;
	ChallengeRun run;
	Id.Run runId;		
	double diff;
	double savetime;
	boolean updating;
	
	
	public RunDisplay(ChallengeModel m, Id.Run rid)
	{
		runId = rid;
		run = null;
		diff = Double.NaN;
		model = m;
		savetime = 0;
		updating = false;

		InternalListener l = new InternalListener();
		
		reaction = new JLabel("", JLabel.CENTER);
		reaction.setFont(reactionFont);
		
		sixty = new JLabel("", JLabel.CENTER);
		sixty.setFont(reactionFont);

		value = new TimeTextField("00.000", 5);
		value.addFocusListener(l);
		value.setFont(timeFont);

		cones = new JComboBox<Integer>(new Integer[] {0, 1, 2, 3, 4, 5});
		cones.addActionListener(l);
		cones.setFont(comboFont);

		gates = new JComboBox<Integer>(new Integer[] {0, 1, 2, 3, 4, 5});
		gates.addActionListener(l);
		gates.setFont(comboFont);

		status = new JComboBox<String>(new String[] {"OK", "RL", "NS", "DNF"});
		status.addActionListener(l);		
		status.setFont(comboFont);

		rundiff = new JLabel("", JLabel.CENTER);
		rundiff.setFont(timeFont);

		setLayout(new MigLayout());
		runPanel = new JPanel(new MigLayout("ins 0"));
		runPanel.setBorder(plainBorder);

		JLabel title = new JLabel();
		title.setFont(bigTitleFont);
		title.setText(rid.isLeft() ? "Left Run" : "Right Run");

		nextButton = new FocusButton(rid);
		nextButton.setFont(titleFont);
		
		add(title, "split 3, w 33%");
		add(nextButton, "w 70!, wrap");
		add(runPanel, "wrap");
		
		runPanel.add(title("start"), "center");
		runPanel.add(title("time"), "center");
		runPanel.add(title("cones"), "center");
		runPanel.add(title("gates"), "center");
		runPanel.add(title("status"), "center");
		runPanel.add(title("diff"), "center, wrap");

		int reactionWidth = reaction.getFontMetrics(reaction.getFont()).stringWidth("0.000");
		int reactionHeight = reaction.getFontMetrics(reaction.getFont()).getHeight();
		int diffSize = rundiff.getFontMetrics(rundiff.getFont()).stringWidth("-20.000");
		
		runPanel.add(reaction, String.format("flowy, split 2, gapright 5, gapleft 5, width %d!, height %d!", reactionWidth, reactionHeight));
		runPanel.add(sixty, String.format("gapleft 5, height %d!", reactionHeight));
		runPanel.add(value, "");
		runPanel.add(cones, "width 34!");
		runPanel.add(gates, "width 34!");
		runPanel.add(status, "");
		runPanel.add(rundiff, String.format("width %d!, gapleft 5, gapright 5", diffSize));
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
			reaction.setText("");
			sixty.setText("");
			value.setText("00.000");
			cones.setSelectedIndex(0);
			gates.setSelectedIndex(0);
			status.setSelectedIndex(0);
			rundiff.setText("");

			run = model.getRun(runId);
			if (run == null)
				return;

			savetime = run.getRaw();			
			if (!Double.isNaN(run.getReaction()))
				reaction.setText(NF.format(run.getReaction()));
			if (!Double.isNaN(run.getSixty()))
				sixty.setText(NF.format(run.getSixty()));
			if (!Double.isNaN(run.getRaw()))
				value.setTime(run.getRaw());
			cones.setSelectedItem(run.getCones());
			gates.setSelectedItem(run.getGates());
			status.setSelectedItem(run.getStatus());
			diff = run.getNet() - model.getDial(runId);
			if (Double.isNaN(diff))
				rundiff.setText("");
			else if (diff > 900)
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
		nextButton.setStage(model.getState(runId));
		nextButton.repaint();
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
