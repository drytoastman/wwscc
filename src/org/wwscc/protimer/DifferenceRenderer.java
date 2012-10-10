/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */


package org.wwscc.protimer;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import org.wwscc.util.NF;

/**
 * Cell renderer for a Double that always creates three decimal places (123.000)
 */
public class DifferenceRenderer extends JPanel implements TableCellRenderer
{
	private JLabel overleft, overlbl, overright;
	private JLabel coverleft, coverlbl, coverright;
	private JLabel newdialleft, newdiallbl, newdialright;
	private JLabel msg1;
	private JLabel msg2;

	private Color colorA;
	private Color colorB;

	/**
	 * Create the renderer using a particular font, if given.
	 */
	public DifferenceRenderer()
	{
		super();

		colorA = new Color(210, 150, 0);
		colorB = new Color(0, 140, 160);

		//Border b = new LineBorder(Color.BLACK);

		setOpaque(true);
		//setBackground(new Color(222, 222, 222));
		setLayout(new GridBagLayout());

		Font bf = new Font("fixed", Font.PLAIN, 18);
		Font fix = new Font("fixed", Font.PLAIN, 18);
		Font lblfont = new Font("serif", Font.PLAIN, 18);
		Font msgfont = new Font("serif", Font.PLAIN, 20);

		overleft = new JLabel();
		overleft.setFont(fix);

		overlbl = new JLabel();
		overlbl.setFont(lblfont);

		overright = new JLabel();
		overright.setFont(fix);

		coverleft = new JLabel();
		coverleft.setFont(fix);

		coverlbl = new JLabel();
		coverlbl.setFont(lblfont);

		coverright = new JLabel();
		coverright.setFont(fix);

		newdialleft = new JLabel();
		newdialleft.setFont(fix);
		newdialleft.setForeground(Color.RED);

		newdiallbl = new JLabel();
		newdiallbl.setFont(lblfont);

		newdialright = new JLabel();
		newdialright.setFont(fix);
		newdialright.setForeground(Color.RED);

		msg1 = new JLabel();
		msg1.setFont(msgfont);

		msg2 = new JLabel();
		msg2.setFont(msgfont);

		GridBagConstraints g = new GridBagConstraints();
		g.insets = new Insets(2, 2, 2, 2);
		g.weightx = 1;
		g.gridwidth = 1;

		g.anchor = GridBagConstraints.EAST;   g.gridx = 0; g.gridy = 0; add(overleft, g);
		g.anchor = GridBagConstraints.CENTER; g.gridx = 1; g.gridy = 0; add(overlbl, g);
		g.anchor = GridBagConstraints.WEST;   g.gridx = 2; g.gridy = 0; add(overright, g);

		g.anchor = GridBagConstraints.EAST;   g.gridx = 0; g.gridy = 1; add(coverleft, g);
		g.anchor = GridBagConstraints.CENTER; g.gridx = 1; g.gridy = 1; add(coverlbl, g);
		g.anchor = GridBagConstraints.WEST;   g.gridx = 2; g.gridy = 1; add(coverright, g);

		g.anchor = GridBagConstraints.EAST;   g.gridx = 0; g.gridy = 2; add(newdialleft, g);
		g.anchor = GridBagConstraints.CENTER; g.gridx = 1; g.gridy = 2; add(newdiallbl, g);
		g.anchor = GridBagConstraints.WEST;   g.gridx = 2; g.gridy = 2; add(newdialright, g);

		g.anchor = GridBagConstraints.CENTER; 
		g.gridwidth = 3;
		g.gridx = 0; g.gridy = 3; add(msg1, g); 
		g.gridx = 0; g.gridy = 4; add(msg2, g);
	}



	protected String side(int type)
	{
		if (type == DualResult.LEFT) 
			return "Left";
		else if (type == DualResult.RIGHT)
			return "Right";
		else
			return "???";
	}


	protected String format(double num)
	{
		if (Double.isNaN(num))
			return "...";
		else
			return NF.format(num);
	}

	
	protected String returnsFaster(DualResult dr)
	{
		double left = dr.getLeftDial();
		double right = dr.getRightDial();

		if (right < left)
			return "Right returns faster by " + format(dr.getLeftDial() - dr.getRightDial());
		else
			return "Left returns faster by " + format(dr.getRightDial() - dr.getLeftDial());
	}

	
	protected String winsBy(DualResult dr)
	{
		ColorTime left = dr.getLeftFinish();
		ColorTime right = dr.getRightFinish();

		if ((left.state != 0) && (right.state != 0))
			return "Nobody wins";
		else if (left.state != 0)
			return "Right wins by default";
		else if (right.state != 0)
			return "Left wins by default";
		else if (!Double.isNaN(left.time) && !Double.isNaN(right.time))
		{
			if (right.time < left.time)
				return "Right wins by " + format(left.time - right.time);
			else
				return "Left wins by " + format(right.time - left.time);
		}

		return "";
	}

	protected String cWinsBy(DualResult dr)
	{
		double left = dr.getLeftChallengeDial();
		double right = dr.getRightChallengeDial();

		if (Double.isNaN(left) && Double.isNaN(right))
			return "Nobody wins";
		else if (Double.isNaN(left))
			return "Right wins by default";
		else if (Double.isNaN(right))
			return "Left wins by default";
		else if (!Double.isNaN(left) && !Double.isNaN(right))
		{
			if (right < left)
				return "Right wins Challenge by " + format(left - right);
			else
				return "Left wins Challenge by " + format(right - left);
		}

		return "";
	}


	@Override
	public Component getTableCellRendererComponent(JTable table, Object o, boolean isSel, boolean hasFocus, int row, int col)
	{
		overleft.setText("");
		overlbl.setText("");
		overright.setText("");
		coverleft.setText("");
		coverlbl.setText("");
		coverright.setText("");
		newdialleft.setText("");
		newdiallbl.setText("");
		newdialright.setText("");
		msg1.setText("");
		msg2.setText("");


		if (o instanceof DualResult)
		{
			DualResult dr = (DualResult)o;

			if (dr.useDial())
			{
				overleft.setText(format(dr.getLeftDial()));
				overlbl.setText("Run OverDial");
				overright.setText(format(dr.getRightDial()));

				overleft.setForeground(colorA);
				overright.setForeground(colorB);
			}

			if (dr.useChallengeDial())
			{
				double left = dr.getLeftChallengeDial();
				double right = dr.getRightChallengeDial();

				coverleft.setText(format(left));
				coverlbl.setText("Total OverDial");
				coverright.setText(format(right));

				if ((left < 0) || (right < 0))
				{
					if (left < 0) newdialleft.setText(format(dr.getLeftOrigDial()+(left/2*1.5)));
					newdiallbl.setText("New Dialin");
					if (right < 0) newdialright.setText(format(dr.getRightOrigDial()+(right/2*1.5)));
				}

				overleft.setForeground(colorB);
				coverleft.setForeground(colorB);
				overright.setForeground(colorA);
				coverright.setForeground(colorA);
			}

			int cwin = dr.getChallengeWin();
			int lead = dr.getLead();
			int win = dr.getWin();

			if (cwin != DualResult.NONE)
			{
				msg2.setText(cWinsBy(dr));
				msg1.setText(returnsFaster(dr));
			}
			else if (lead != DualResult.NONE)
			{
				msg1.setText(returnsFaster(dr));
			}
			else if (win != DualResult.NONE)
			{
				msg1.setText(winsBy(dr));
			}

		}


		return this;
	}
}


