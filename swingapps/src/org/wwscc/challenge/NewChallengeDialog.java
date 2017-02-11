/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wwscc.challenge;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import net.miginfocom.swing.MigLayout;
import org.wwscc.dialogs.BaseDialog;
import org.wwscc.storage.Challenge;
import org.wwscc.storage.Database;

/**
 *
 * @author bwilson
 */
public class NewChallengeDialog extends BaseDialog<String>
{
	public NewChallengeDialog()
	{
		super(new MigLayout("", "[70, align right][100, fill]"), true);
		
		ok.setText("Create");

		mainPanel.add(label("Name", false), "");
		List<String> defaults = new ArrayList<String>();
		defaults.add("Open Challenge");
		defaults.add("Ladies Challenge");
		defaults.add("Bonus Challenge");
		mainPanel.add(autoentry("name", "", defaults), "wrap");
		
		mainPanel.add(label("Size", false), "");
		mainPanel.add(select("size", 4, new Integer[] { 4, 8, 16, 32, 64 }, this), "wrap");
	}
	
	
	@Override
	public boolean verifyData()
	{
		List<Challenge> current = Database.d.getChallengesForEvent(null);
		for (Challenge c : current)
		{
			if (c.getName().equals(getChallengeName()))
			{
				JOptionPane.showMessageDialog(this, "Name already exists, can not create challenge", "Name Exists", JOptionPane.OK_CANCEL_OPTION);
				return false;
			}
		}
		
		return true;
	}
	
	public String getChallengeName() { return getEntryText("name"); }
	public int getChallengeSize() { return (Integer)getSelect("size"); }
}
