/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
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
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import net.miginfocom.swing.MigLayout;
import org.wwscc.components.DoubleLabel;
import org.wwscc.storage.ChallengeRound;
import org.wwscc.storage.ChallengeRun;
import org.wwscc.storage.Entrant;
import org.wwscc.util.IconButton;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;
import org.wwscc.util.TimeTextField;

/**
 *
 */
public class RoundViewer extends JInternalFrame implements MessageListener
{
	static NumberFormat df;
	static
	{
		df = NumberFormat.getNumberInstance();
		df.setMinimumFractionDigits(3);
		df.setMaximumFractionDigits(3);
	}

	public static ImageIcon goIcon;
	public static ImageIcon goOverIcon;
	public static ImageIcon goPressIcon;
	static Color next = new Color(10, 150, 10);
	static Color next2 = new Color(150, 50, 0);

	ChallengeModel model;
	JLabel round, result, rldiff, lrdiff;
	EntrantDisplay top, bottom;
	Id.Round roundId;

	public RoundViewer(ChallengeModel m, Id.Round rid)
	{
		super("Round X", false, true);
		model = m;
		roundId = rid;
		
		top = new EntrantDisplay(rid.makeUpper());
		bottom = new EntrantDisplay(rid.makeLower());

		round = new JLabel("Rnd " + rid.round);
		round.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
		result = new JLabel("No comment yet");
		result.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
		
		setLayout(new MigLayout("ins 3", "[70][]"));
		//setBorder(new SoftBevelBorder(BevelBorder.RAISED));
		//setBorder(new LineBorder(Color.GRAY, 2));
		
		setBackground(Color.WHITE);
		
		add(top.nameLbl, "wmax 70");
		add(top.leftRun, "growx, wrap");
		add(top.autoWin, "");
		add(top.rightRun, "growx, wrap");

		add(round, "al center center");
		add(result, "al center center, wrap");
		
		add(bottom.nameLbl, "wmax 90");
		add(bottom.leftRun, "growx, wrap");
		add(bottom.autoWin, "");
		add(bottom.rightRun, "growx, wrap");
		
		Messenger.register(MT.ACTIVE_CHANGE, this);
		Messenger.register(MT.RUN_CHANGED, this);

		event(MT.ACTIVE_CHANGE, null);
		event(MT.RUN_CHANGED, null);
		addInternalFrameListener(new InternalFrameAdapter() {
			public void internalFrameClosed(InternalFrameEvent e)
			{
				System.out.println("unregistering");
				Messenger.unregister(MT.ACTIVE_CHANGE, RoundViewer.this);
				Messenger.unregister(MT.RUN_CHANGED, RoundViewer.this);
			}
		});

		pack();
		setVisible(true);
	}

	
	public void updateResults()
	{	
		String name;		
		double val;
		double topdiff = top.getDiff();
		double bottomdiff = bottom.getDiff();
		double newdial = 0;

		ChallengeRound r = model.getRound(roundId);
		if (topdiff > bottomdiff)
		{
			name = bottom.getFirstName();
			val = topdiff - bottomdiff;
			if (r.getCar2().breakout())
				newdial = r.getCar2().getNewDial();
		}
		else
		{
			name = top.getFirstName();
			val = bottomdiff - topdiff;
			if (r.getCar1().breakout())
				newdial = r.getCar1().getNewDial();
		}

		
		switch (r.getState())
		{
			case NONE:
				result.setText("No comment yet");
				break;
				
			case HALFNORMAL:
			case HALFINVERSE:
				result.setText(name + " leads by " + df.format(val));
				break;
				
			case DONE:
				if (newdial > 0)
					result.setText(name + " wins by " + df.format(val) + "  NewDial: " + df.format(newdial));
				else
					result.setText(name + " wins by " + df.format(val));
				break;
				
			case INVALID:
				result.setText("Invalid round state");
				break;
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
		public DoubleLabel nameLbl;
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
				name = model.getEntrant(eid).getFirstName();
			else
				name = "(none)";
			autoWin = new JButton("AutoWin");
			autoWin.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
			autoWin.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Messenger.sendEvent(MT.AUTO_WIN, entryId);
				}
			});
			nameLbl = new DoubleLabel(name, df.format(dial));
			nameLbl.setOpaque(false);
			nameLbl.setFont(new Font(Font.DIALOG, Font.BOLD, 13), new Font(Font.DIALOG, Font.PLAIN, 12));
			leftRun = new RunDisplay(eid.makeLeft(), dial);
			rightRun = new RunDisplay(eid.makeRight(), dial);
		}
		
		public String getFirstName()
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
		JButton activate;
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

			if (goIcon == null)
			{
				goIcon = new ImageIcon(getClass().getResource("/org/wwscc/images/go.png"));
				goOverIcon = new ImageIcon(getClass().getResource("/org/wwscc/images/go-hover.png"));
				goPressIcon = new ImageIcon(getClass().getResource("/org/wwscc/images/go-press.png"));
			}

			activate = new IconButton(goIcon, goOverIcon, goPressIcon);
			activate.addActionListener(this);
			
			savetime = 0;
			value = new TimeTextField("00.000", 5);
			value.addFocusListener(this);
			value.setFont(new Font("SansSerif", Font.PLAIN, 12));

			cones = new JComboBox(new Integer[] {0, 1, 2, 3, 4, 5});
			cones.addActionListener(this);
			status = new JComboBox(new String[] {"OK", "RL", "NS", "DNF"});
			status.addActionListener(this);
			Font f = new Font("SansSerif", Font.PLAIN, 10);
			cones.setFont(f);
			status.setFont(f);
			
			info = new JLabel("0.000");

			setLayout(new MigLayout(""));
			setBorder(new SoftBevelBorder(BevelBorder.RAISED));

			add(activate);
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
			run = model.getRun(runId);
			if (run == null)
				return;
		
			try
			{
				updating = true;
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
			if (o == activate)
				model.makeActive(runId);
			else if (o == cones)
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
