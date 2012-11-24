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
import net.miginfocom.swing.MigLayout;
import org.wwscc.challenge.ChallengeModel;
import org.wwscc.challenge.Id;
import org.wwscc.storage.Entrant;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;
import org.wwscc.util.NF;

/**
 * Simple collection of pieces used to display each entrant in a round viewer
 */
class EntrantDisplay extends JComponent
{
	static Font nameFont = new Font(Font.DIALOG, Font.BOLD, 13);
	static Font dialFont = new Font(Font.DIALOG, Font.PLAIN, 12);
	static Font buttonFont = new Font(Font.DIALOG, Font.PLAIN, 10);
	
	String name;
	JLabel nameLbl;
	JLabel dialLbl;
	JButton autoWin;
	JButton changeDial;
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
		autoWin.setFont(buttonFont);
		autoWin.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				Messenger.sendEvent(MT.AUTO_WIN, entryId);
			}
		});

		changeDial = new JButton("Dial");
		changeDial.setFont(buttonFont);
		changeDial.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
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
		nameLbl.setFont(nameFont);
		dialLbl = new JLabel(NF.format(model.getDial(entryId)));
		dialLbl.setFont(dialFont);
		
		setLayout(new MigLayout());
		add(nameLbl, "al center, split 2");
		add(dialLbl, "wrap");
		add(autoWin, "hmax 15, al center, split 2");
		add(changeDial, "hmax 15, al center");
	}
}
