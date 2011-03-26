/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2011 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.challenge;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.NumberFormat;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import net.miginfocom.swing.MigLayout;
import org.wwscc.components.UnderlineBorder;
import org.wwscc.storage.ChallengeRound;
import org.wwscc.storage.ChallengeRound.RoundState;
import org.wwscc.storage.ChallengeRun;
import org.wwscc.storage.Entrant;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;
import org.wwscc.util.TimeTextField;

/**
 *
 */
public class RoundViewer extends JInternalFrame implements MessageListener
{
	private static final Logger log = Logger.getLogger(RoundViewer.class.getCanonicalName());
	
	static final NumberFormat df;
	static
	{
		df = NumberFormat.getNumberInstance();
		df.setMinimumFractionDigits(3);
		df.setMaximumFractionDigits(3);
	}

	static Color next = new Color(10, 150, 10);
	static Color next2 = new Color(150, 50, 0);
	static Font midResultFont = new Font(Font.DIALOG, Font.BOLD, 12);
	static Font finalResultFont = new Font(Font.DIALOG, Font.BOLD, 14);
	static Font timeFont = new Font("SansSerif", Font.PLAIN, 12);

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

		top = new EntrantDisplay(rid.makeUpper());
		bottom = new EntrantDisplay(rid.makeLower());

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
		
		add(stage, "span 3, split 3, al center");
		add(swap, "");
		add(reset, "wrap");


		add(left.nameLbl, "al center, split 2");
		add(left.dialLbl, "");
		add(right.nameLbl, "al center, split 2");
		add(right.dialLbl, "wrap");

		add(left.autoWin, "hmax 15, al center");
		add(right.autoWin, "hmax 15, al center, wrap");

		JLabel border = new JLabel(" ");
		border.setBorder(new UnderlineBorder());
		add(border, "growx, span 2, wrap");
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
	}

	private ResultTuple getRunResult(boolean first)
	{
		EntrantDisplay e1, e2;
		RunDisplay r1, r2;
		double d1, d2;
		int c1, c2;
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

		if ((r1.getRun() == null) || (r2.getRun() == null))
		{
			ret.msg = " -*- ";
			return ret;
		}

		d1 = r1.getDiff();
		d2 = r2.getDiff();
		c1 = r1.getRun().getCones();
		c2 = r2.getRun().getCones();

		if (Double.isNaN(d1) || Double.isNaN(d2))
		{
			ret.msg = " --- ";
		}
		else if (!r1.getRun().getStatus().equals("OK"))
		{
			ret.defaultWin = true;
			ret.msg = e1.getName() + " has status " + r1.getRun().getStatus();
		}
		else if (!r2.getRun().getStatus().equals("OK"))
		{
			ret.defaultWin = true;
			ret.msg = e2.getName() + " has status " + r2.getRun().getStatus();
		}
		else if (d1 < d2)
		{
			ret.msg = e1.getName() + " returns faster by " + df.format(d2 - d1);
			if ((c1 != 0) || (c2 != 0))
				ret.msg += " (" + df.format(d2 - d1 - (2*c2) + (2*c1)) + ") ";
		}
		else if (d2 < d1)
		{
			ret.msg = e2.getName() + " returns faster by " + df.format(d1 - d2);
			if ((c1 != 0) || (c2 != 0))
				ret.msg += " (" + df.format(d1 - d2 - (2*c1) + (2*c2)) + ") ";
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
		if ((state == RoundState.DONE) || (firstHalfData.defaultWin))
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

			if (firstHalfData.defaultWin || secondHalfData.defaultWin)
				result = name + " wins by default";
			else
				result = name + " wins by " + df.format(val);

			if (newdial > 0)
				result += " and breaks out! New dialin is " + df.format(newdial);

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

	class EntrantDisplay extends JComponent
	{
		public String name;
		public JLabel nameLbl;
		public JLabel dialLbl;
		public JButton autoWin;
		public double dial;
		public RunDisplay leftRun, rightRun;
		public Id.Entry entryId;

		public EntrantDisplay(Id.Entry eid)
		{
			entryId = eid;
		
			dial = model.getDial(eid);
			Entrant e = model.getEntrant(eid);
			if (e != null)
				name = e.getFirstName() + " " + e.getLastName();
			else
				name = "(none)";
			autoWin = new JButton("AutoWin");
			autoWin.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
			autoWin.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Messenger.sendEvent(MT.AUTO_WIN, entryId);
				}
			});
			nameLbl = new JLabel(name);
			nameLbl.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
			dialLbl = new JLabel(df.format(dial));
			dialLbl.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
			leftRun = new RunDisplay(eid.makeLeft(), dial);
			rightRun = new RunDisplay(eid.makeRight(), dial);
		}
		
		public String getName()
		{
			return name;
		}
		
		public void updateRun()
		{
			leftRun.updateRun();
			rightRun.updateRun();
		}
		
		public void updateColors()
		{
			leftRun.updateColor();
			rightRun.updateColor();
		}
		
		public double getDiff()
		{
			double d1 = leftRun.getDiff();
			double d2 = rightRun.getDiff();
			if (Double.isNaN(d1) && Double.isNaN(d2))
				return 0.0;
			else if (Double.isNaN(d1))
				return d2;
			else if (Double.isNaN(d2))
				return d1;
			else
				return d1 + d2;
		}
	}

	class RunDisplay extends JComponent implements ActionListener, FocusListener
	{	
		TimeTextField value;
		JComboBox cones;
		JComboBox status;
		JLabel info;
		
		ChallengeRun run;
		Id.Run runId;		
		double diff;
		double dial = 1.000;
		double savetime;
		
		private boolean updating = false;

		public RunDisplay(Id.Run rid, double inDial)
		{
			runId = rid;
			dial = inDial;
			run = null;
			diff = Double.NaN;
			
			savetime = 0;
			value = new TimeTextField("00.000", 5);
			value.addFocusListener(this);
			value.setFont(timeFont);

			cones = new JComboBox(new Integer[] {0, 1, 2, 3, 4, 5});
			cones.addActionListener(this);
			status = new JComboBox(new String[] {"OK", "RL", "NS", "DNF"});
			status.addActionListener(this);
			Font f = new Font("SansSerif", Font.PLAIN, 10);
			cones.setFont(f);
			status.setFont(f);
			
			info = new JLabel("0.000");
			info.setFont(timeFont);

			setLayout(new MigLayout(""));
			setBorder(new SoftBevelBorder(BevelBorder.RAISED));

			add(value, "");
			add(cones, "width 34!");
			add(status, "");
			add(info, "");
		}
		
		public ChallengeRun getRun()
		{
			return run;
		}

		public double getDiff()
		{
			return diff;
		}
		
		public void updateRun()
		{
			try
			{
				updating = true;
				value.setText("00.000");
				cones.setSelectedIndex(0);
				status.setSelectedIndex(0);
				info.setText("0.000");

				run = model.getRun(runId);
				if (run == null)
					return;

				savetime = run.getRaw();
				value.setTime(run.getRaw());
				cones.setSelectedItem(run.getCones());
				status.setSelectedItem(run.getStatus());
				diff = run.getNet() - dial;
				info.setText(df.format(diff));
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

		@Override
		public void actionPerformed(ActionEvent e) 
		{
			if (updating)
				return;
			
			Object o = e.getSource();
			if (o == cones)
				model.setCones(runId, (Integer)cones.getSelectedItem());
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
