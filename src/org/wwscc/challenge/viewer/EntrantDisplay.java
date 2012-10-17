/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */
package org.wwscc.challenge.viewer;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import org.wwscc.challenge.ChallengeModel;
import org.wwscc.challenge.Id;
import org.wwscc.storage.Entrant;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;
import org.wwscc.util.NF;

/**
 */
class EntrantDisplay extends JComponent
{
	String name;
	JLabel nameLbl;
	JLabel dialLbl;
	JButton autoWin;
	JButton changeDial;
	RunDisplay leftRun, rightRun;
	Id.Entry entryId;
	ChallengeModel model;
	
	public EntrantDisplay(ChallengeModel m, Id.Entry eid)
	{
		entryId = eid;
		model = m;
		Entrant e = model.getEntrant(eid);
		if (e != null)
			name = e.getFirstName() + " " + e.getLastName();
		else
			name = "(none)";

		autoWin = new JButton("AutoWin");
		autoWin.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
		autoWin.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Messenger.sendEvent(MT.AUTO_WIN, entryId);
			}
		});

		changeDial = new JButton("Dial");
		changeDial.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
		changeDial.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String ret = (String)JOptionPane.showInputDialog("Override Dialin Value", model.getDial(entryId));
				if (ret != null) {
					double dial = Double.valueOf(ret);
					dialLbl.setText(NF.format(dial));
					model.overrideDial(entryId, dial);
					Messenger.sendEventNow(MT.RUN_CHANGED, null);
				}
			}
		});

		nameLbl = new JLabel(name);
		nameLbl.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
		dialLbl = new JLabel(NF.format(model.getDial(entryId)));
		dialLbl.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
		leftRun = new RunDisplay(model, eid, eid.makeLeft());
		rightRun = new RunDisplay(model, eid, eid.makeRight());
	}

	@Override
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
		double d1 = leftRun.diff;
		double d2 = rightRun.diff;
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
