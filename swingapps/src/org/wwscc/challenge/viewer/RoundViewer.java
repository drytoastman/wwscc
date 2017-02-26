/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2011 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.challenge.viewer;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import net.miginfocom.swing.MigLayout;
import org.wwscc.challenge.ActivationRequest;
import org.wwscc.challenge.ChallengeModel;
import org.wwscc.challenge.Id;
import org.wwscc.components.UnderlineBorder;
import org.wwscc.storage.ChallengeRound;
import org.wwscc.storage.ChallengeRound.RoundState;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;
import org.wwscc.util.NF;

/**
 *  Implementation for the internal window used to view rounds as the challenge is run
 */
public class RoundViewer extends JInternalFrame implements MessageListener
{
	private static final Logger log = Logger.getLogger(RoundViewer.class.getCanonicalName());
	
	static Font midResultFont = new Font(Font.DIALOG, Font.BOLD, 14);
	static Font finalResultFont = new Font(Font.DIALOG, Font.BOLD, 16);

	ChallengeModel model;
	JButton stage, reset;
	JCheckBox swap;
	JLabel firstresult, secondresult, finalresult, rndial, lndial;
	EntrantStruct top, bottom;
	Id.Round roundId;

	/**
	 * Create a new round viewer based on the model and specified round
	 * @param m the model for data 
	 * @param rid the identifier to a specific round
	 */
	public RoundViewer(ChallengeModel m, Id.Round rid)
	{
		super("Round " + rid.round, false, true);
		model = m;
		roundId = rid;

		top = new EntrantStruct(model, rid.makeUpper());
		bottom = new EntrantStruct(model, rid.makeLower());

		firstresult = new JLabel();
		firstresult.setFont(midResultFont);
		secondresult = new JLabel();
		secondresult.setFont(midResultFont);
		finalresult = new JLabel("No comment yet");
		finalresult.setFont(finalResultFont);

		lndial = new JLabel("");
		lndial.setFont(finalResultFont);
		lndial.setForeground(Color.RED);
		
		rndial = new JLabel("");
		rndial.setFont(finalResultFont);
		rndial.setForeground(Color.RED);

		stage = new JButton("Stage");
		stage.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (model.getRound(roundId).isSwappedStart())
					Messenger.sendEvent(MT.ACTIVE_CHANGE_REQUEST, new ActivationRequest(roundId.makeUpperRight(), true, true));
				else
					Messenger.sendEvent(MT.ACTIVE_CHANGE_REQUEST, new ActivationRequest(roundId.makeUpperLeft(), true, true));
			};
		});

		swap = new JCheckBox("Swapped Start", model.getRound(roundId).isSwappedStart());
		swap.setOpaque(false);
		swap.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				model.getRound(roundId).setSwappedStart(swap.isSelected());
				updateResults();
				buildLayout();
			}
		});

		reset = new JButton("Reset Round");
		reset.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (JOptionPane.showConfirmDialog(RoundViewer.this, "This will remove all run data for this round, are you sure?", "Are you Sure?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
					return;
				model.resetRound(roundId);
				event(MT.RUN_CHANGED, null);
			}
		});

		buildLayout();
		
		Messenger.register(MT.ACTIVE_CHANGE, this);
		Messenger.register(MT.RUN_CHANGED, this);
		event(MT.ACTIVE_CHANGE, null);
		event(MT.RUN_CHANGED, null);
		addInternalFrameListener(new InternalFrameAdapter() {
			@Override
			public void internalFrameClosed(InternalFrameEvent e)
			{
				Messenger.unregister(MT.ACTIVE_CHANGE, RoundViewer.this);
				Messenger.unregister(MT.RUN_CHANGED, RoundViewer.this);
			}
		});

		setVisible(true);
	}

	protected void buildLayout()
	{
		getContentPane().removeAll();
		setLayout(new MigLayout("ins 5, fill", "[]30[]"));
		setBackground(Color.WHITE);

		EntrantStruct left, right;
		if (swap.isSelected())
		{
			log.fine("building layout with swapped start order");
			left = bottom;
			right = top;
		}
		else
		{
			log.fine("building layout with regular start order");
			left = top;
			right = bottom;
		}
		
		add(stage, "span 2, split 2, al center");
		add(reset, "wrap");
		add(swap, "span 2, center, wrap");

		add(left.display, "center");
		add(right.display, "center, wrap");
		
		JLabel border = new JLabel(" ");
		border.setBorder(new UnderlineBorder());
		add(border, "growx, span 2, h 2!, wrap");
		add(left.leftRun, "");
		add(right.rightRun, "wrap");

		add(firstresult, String.format("al center center, span 2, wrap, gapbottom 5, h %d!", compHeight(firstresult)));

		add(left.rightRun, "");
		add(right.leftRun, "wrap");
		
		add(secondresult, String.format("al center center, span 2, wrap, h %d!", compHeight(secondresult)) );
		add(finalresult, String.format("al center center, span 2, wrap, h %d!", compHeight(finalresult)));
		add(lndial, String.format("center, h %d!", compHeight(lndial)));
		add(rndial, "center, wrap");
		pack();
		revalidate();
	}

	/**
	 * Utility for calculating height of font based component
	 * @param c the component we are using
	 * @return the pixel height based on current font
	 */
	public static int compHeight(JComponent c)
	{
		return c.getFontMetrics(c.getFont()).getHeight();
	}
	
	
	private static class ResultTuple
	{
		public String msg = "";
		public boolean defaultWin = false;
		public boolean doubleDefault = false;
		
		public ResultTuple() {}
		/*public ResultTuple(String m, boolean win, boolean def)
		{
			msg = m;
			defaultWin = win;
			doubleDefault = def;
		}*/
	}

	private ResultTuple getRunResult(boolean first)
	{
		EntrantStruct e1, e2;
		RunDisplay r1, r2;
		double d1, d2;
		int p1, p2;
		ResultTuple ret = new ResultTuple();

		if (swap.isSelected())
		{
			e1 = bottom;
			e2 = top;
		}
		else
		{
			e1 = top;
			e2 = bottom;
		}

		if (first)
		{
			r1 = e1.leftRun;
			r2 = e2.rightRun;
		}
		else
		{
			r1 = e1.rightRun;
			r2 = e2.leftRun;
		}

		if ((r1.run == null) || (r2.run == null))
		{
			ret.msg = "";
			return ret;
		}

		d1 = r1.diff;
		d2 = r2.diff;
		p1 = (2 * r1.run.getCones()) + (10 * r1.run.getGates());
		p2 = (2 * r2.run.getCones()) + (10 * r2.run.getGates());

		if (Double.isNaN(d1) || Double.isNaN(d2))
		{
			ret.msg = "Uh oh. Someone had NaN!";
		}
		else if (!r1.run.isOK() || !r2.run.isOK())
		{
			if ((r1.run.statusLevel() > 1) && (r2.run.statusLevel() > 1))
			{
				ret.doubleDefault = true;
				ret.msg = "Both entrants had light troubles";
			}
			else if (r1.run.statusLevel() > r2.run.statusLevel())
			{
				ret.defaultWin = true;
				ret.msg = e1.getName() + " has status " + r1.run.getStatus();
			}
			else if (r1.run.statusLevel() < r2.run.statusLevel())
			{
				ret.defaultWin = true;
				ret.msg = e2.getName() + " has status " + r2.run.getStatus();
			}
			else
			{
				ret.doubleDefault = true;
				ret.msg = "Both entrants DNF'd";
			}
		}
		else if (d1 < d2)
		{
			ret.msg = e1.getName() + " returns faster by " + NF.format(d2 - d1);
			if ((p1 != 0) || (p2 != 0))
				ret.msg += " (" + NF.format(d2 - d1 - p2 + p1) + ") ";
		}
		else if (d2 < d1)
		{
			ret.msg = e2.getName() + " returns faster by " + NF.format(d1 - d2);
			if ((p1 != 0) || (p2 != 0))
				ret.msg += " (" + NF.format(d1 - d2 - p1 + p2) + ") ";
		}
		else
			ret.msg = "Both return with the same net time!";

		return ret;
	}

	
	public void updateResults()
	{	
		setTitle(String.format("%s  VS  %s", top.getName(), bottom.getName()));
		firstresult.setText("");
		secondresult.setText("");
		finalresult.setText("");
		lndial.setText(" ");
		rndial.setText(" ");

		ChallengeRound round = model.getRound(roundId);
		RoundState state = round.getState();
		ResultTuple firstHalfData = new ResultTuple();

		// Do we need first run results ?
		for (RoundState rs : new RoundState[] { RoundState.HALFNORMAL, RoundState.HALFINVERSE, RoundState.PARTIAL2, RoundState.DONE })
		{
			if (state == rs)  // much easier in python :)
			{
				firstHalfData = getRunResult(true);
				firstresult.setText(firstHalfData.msg);
				break;
			}
		}
		
		// Do we need second run and final results ?
		if ((state == RoundState.DONE) || (firstHalfData.defaultWin) || (firstHalfData.doubleDefault))
		{
			String name;
			double val;
			double topdiff = top.getDiff();
			double bottomdiff = bottom.getDiff();

			if (topdiff > bottomdiff)
			{
				name = bottom.getName();
				val = topdiff - bottomdiff;
			}
			else
			{
				name = top.getName();
				val = bottomdiff - topdiff;
			}

			String result;
			ResultTuple secondHalfData = new ResultTuple();

			if (state == RoundState.DONE) // can enter this area after first half default
				secondHalfData = getRunResult(false);

			if (firstHalfData.doubleDefault || secondHalfData.doubleDefault)
				result = "Both entrants are disqualified";
			else if (firstHalfData.defaultWin || secondHalfData.defaultWin)
				result = name + " wins by default";
			else if (val > 0)
				result = name + " wins by " + NF.format(val);
			else
				result = "It's a tie!";
			
			if (state == RoundState.DONE) // can enter this area after first half default
			{
				if (model.brokeout(round.getTopCar()))
				{
					String nd = "Breakout! New Dial: " + NF.format(model.getNewDial(round.getTopCar()));
					if (round.isSwappedStart())
						rndial.setText(nd);
					else
						lndial.setText(nd);
				}

				if (model.brokeout(round.getBottomCar()))
				{
					String nd = "Breakout! New Dial: " + NF.format(model.getNewDial(round.getBottomCar()));
					if (round.isSwappedStart())
						lndial.setText(nd);
					else
						rndial.setText(nd);
				}

				secondresult.setText(secondHalfData.msg);
			}
			
			finalresult.setText(result);
		}
		else if (state == RoundState.INVALID)
		{
			//finalresult.setText("Invalid round state");  don't show this at this point incase of odd run order
		}
	}
	
	@Override
	public void event(MT type, Object data)
	{
		switch (type)
		{
			case ACTIVE_CHANGE:
				top.updateColors();
				bottom.updateColors();
				break;
				
			case RUN_CHANGED:
				top.updateRun();
				bottom.updateRun();
				updateResults();
				break;
		}
	}
}
