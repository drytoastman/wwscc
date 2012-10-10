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
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import net.miginfocom.swing.MigLayout;
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
 *
 */
public class RoundViewer extends JInternalFrame implements MessageListener
{
	private static final Logger log = Logger.getLogger(RoundViewer.class.getCanonicalName());
	
	static Font midResultFont = new Font(Font.DIALOG, Font.BOLD, 12);
	static Font finalResultFont = new Font(Font.DIALOG, Font.BOLD, 14);

	ChallengeModel model;
	JButton stage, swap, reset;
	JLabel firstresult, secondresult, finalresult, rldiff, lrdiff;
	EntrantDisplay top, bottom;
	Id.Round roundId;
	boolean swapped;

	public RoundViewer(ChallengeModel m, Id.Round rid)
	{
		super("Round " + rid.round, false, true);
		model = m;
		roundId = rid;
		swapped = model.getRound(roundId).isSwappedStart();

		top = new EntrantDisplay(model, rid.makeUpper());
		bottom = new EntrantDisplay(model, rid.makeLower());

		firstresult = new JLabel();
		firstresult.setFont(midResultFont);
		secondresult = new JLabel();
		secondresult.setFont(midResultFont);
		finalresult = new JLabel("No comment yet");
		finalresult.setFont(finalResultFont);

		stage = new JButton("Stage");
		stage.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (model.getRound(roundId).isSwappedStart())
					model.makeActive(roundId.makeUpperRight());
				else
					model.makeActive(roundId.makeUpperLeft());

			}
		});

		swap = new JButton("Swap");
		swap.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				model.getRound(roundId).swapStart();
				swapped = model.getRound(roundId).isSwappedStart();
				updateResults();
				buildLayout();
			}
		});

		reset = new JButton("Reset Round");
		reset.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (JOptionPane.showConfirmDialog(null, "This will remove all run data for this round, are you sure?", "Are you Sure?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
					return;
				model.resetRound(roundId);
				event(MT.RUN_CHANGED, null);
				buildLayout();
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
		setLayout(new MigLayout("ins 3, fill", "[]30[]"));
		setBackground(Color.WHITE);

		EntrantDisplay left, right;
		if (swapped)
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
		
		add(stage, "span 3, split 3, al center, gapbottom 10");
		add(swap, "");
		add(reset, "wrap");


		add(left.nameLbl, "al center, split 2");
		add(left.dialLbl, "");
		add(right.nameLbl, "al center, split 2");
		add(right.dialLbl, "wrap");

		add(left.autoWin, "hmax 15, al center, split 2");
		add(left.changeDial, "hmax 15, al center");
		add(right.autoWin, "hmax 15, al center, split 2");
		add(right.changeDial, "hmax 15, al center, wrap");

		JLabel border = new JLabel(" ");
		border.setBorder(new UnderlineBorder());
		add(border, "growx, span 2, gaptop 3, gapbottom 5, wrap");
		add(new JLabel("Left Run"), "");
		add(new JLabel("Right Run"), "wrap");
		add(left.leftRun, "");
		add(right.rightRun, "wrap");

		add(firstresult, "al center center, span 2, wrap");

		add(new JLabel("Right Run"), "");
		add(new JLabel("Left Run"), "wrap");
		add(left.rightRun, "");
		add(right.leftRun, "wrap");

		add(secondresult, "al center center, span 2, wrap");
		add(finalresult, "al center center, span 2, wrap");
		
		pack();
		revalidate();
	}

	private static class ResultTuple
	{
		public String msg = "";
		public boolean defaultWin = false;
		public boolean doubleDefault = false;
		
		public ResultTuple() {}
		public ResultTuple(String m, boolean win, boolean def)
		{
			msg = m;
			defaultWin = win;
			doubleDefault = def;
		}
	}

	private ResultTuple getRunResult(boolean first)
	{
		EntrantDisplay e1, e2;
		RunDisplay r1, r2;
		double d1, d2;
		int p1, p2;
		ResultTuple ret = new ResultTuple();

		if (swapped)
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
			ret.msg = " -*- ";
			return ret;
		}

		d1 = r1.diff;
		d2 = r2.diff;
		p1 = (2 * r1.run.getCones()) + (10 * r1.run.getGates());
		p2 = (2 * r2.run.getCones()) + (10 * r2.run.getGates());

		if (Double.isNaN(d1) || Double.isNaN(d2))
		{
			ret.msg = " --- ";
		}
		else if (!r1.run.isOK() || !r2.run.isOK())
		{
			if ((r1.run.statusLevel() > 1) && (r2.run.statusLevel() > 1))
			{
				ret.doubleDefault = true;
				ret.msg = "Both entrants had light troubles";
			}
			else if (r1.run.statusLevel() > 1)
			{
				ret.defaultWin = true;
				ret.msg = e1.getName() + " has status " + r1.run.getStatus();
			}
			else if (r2.run.statusLevel() > 1)
			{
				ret.defaultWin = true;
				ret.msg = e2.getName() + " has status " + r2.run.getStatus();
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
		firstresult.setText(" Run 1 Result ");
		secondresult.setText(" Run 2 Result ");
		finalresult.setText(" Final Result ");

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
			double newdial = 0;

			if (topdiff > bottomdiff)
			{
				name = bottom.getName();
				val = topdiff - bottomdiff;
				if (round.getBottomCar().breakout())
					newdial = round.getBottomCar().getNewDial();
			}
			else
			{
				name = top.getName();
				val = bottomdiff - topdiff;
				if (round.getTopCar().breakout())
					newdial = round.getTopCar().getNewDial();
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
			
			if (newdial > 0)
				result += " and breaks out! New dialin is " + NF.format(newdial);

			if (state == RoundState.DONE) // can enter this area after first half default
				secondresult.setText(secondHalfData.msg);
			
			finalresult.setText(result);
		}
		else if (state == RoundState.INVALID)
		{
			finalresult.setText("Invalid round state");
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
