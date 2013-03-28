/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.Box;
import javax.swing.JPanel;
import org.wwscc.storage.Driver;
import org.wwscc.storage.DriverField;

/**
 * Core functions for all dialogs.
 */
public class DriverDialog extends BaseDialog<Driver>
{
	List<DriverField> xfields;
	
	/**
	 * Create the dialog.
	 * @param parent	the parent Frame if any
	 * @param d			the driver data to source initially
	 */
    public DriverDialog(Driver d, List<DriverField> f)
	{
        super(new GridBagLayout(), true);

		if (d == null) d = new Driver();
		xfields = f;

		GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(2,4,2,4);

		/* row 0 */
		c.gridx = 0; c.gridy = 0; c.gridwidth = 1; c.weightx = 0; mainPanel.add(label("First Name", true), c);
		c.gridx = 1; c.gridy = 0; c.gridwidth = 1; c.weightx = 1; mainPanel.add(entry("firstname", d.getFirstName()), c);

		c.gridx = 2; c.gridy = 0; c.gridwidth = 1; c.weightx = 0; mainPanel.add(label("Last Name", true), c);
		c.gridx = 3; c.gridy = 0; c.gridwidth = 1; c.weightx = 1; mainPanel.add(entry("lastname", d.getLastName()), c);

		/* row 1 */
		c.gridx = 0; c.gridy = 1; c.gridwidth = 1; c.weightx = 0; mainPanel.add(label("Email", false), c);
		c.gridx = 1; c.gridy = 1; c.gridwidth = 3; c.weightx = 1; mainPanel.add(entry("email", d.getEmail()), c);

		/* row 2 */
		c.gridx = 0; c.gridy = 2; c.gridwidth = 1; c.weightx = 0; mainPanel.add(label("Address", false), c);
		c.gridx = 1; c.gridy = 2; c.gridwidth = 3; c.weightx = 1; mainPanel.add(entry("address", d.getAddress()), c);

		/* row 3 */
		c.gridx = 0; c.gridy = 3; c.gridwidth = 1; c.weightx = 0; mainPanel.add(label("City", false), c);
		c.gridx = 1; c.gridy = 3; c.gridwidth = 1; c.weightx = 1; mainPanel.add(entry("city", d.getCity()), c);

		JPanel szp = new JPanel(new GridBagLayout());
		c.gridx = 0; c.gridy = 0; c.gridwidth = 1; c.weightx = 0; szp.add(label("State", false), c);
		c.gridx = 1; c.gridy = 0; c.gridwidth = 1; c.weightx = 1; szp.add(entry("state", d.getState()), c);
		c.gridx = 2; c.gridy = 0; c.gridwidth = 1; c.weightx = 0; szp.add(label("Zip", false), c);
		c.gridx = 3; c.gridy = 0; c.gridwidth = 1; c.weightx = 1; szp.add(entry("zip", d.getZip()), c);

		c.insets = new Insets(0,0,0,0);
		c.gridx = 2; c.gridy = 3; c.gridwidth = 2; c.weightx = 0; mainPanel.add(szp, c);
		c.insets = new Insets(2,4,2,4);

		/* row 4 */
		c.gridx = 0; c.gridy = 4; c.gridwidth = 1; c.weightx = 0; mainPanel.add(label("Phone", false), c);
		c.gridx = 1; c.gridy = 4; c.gridwidth = 3; c.weightx = 1; mainPanel.add(entry("phone", d.getPhone()), c);

		/* row 5 */
		c.gridx = 0; c.gridy = 5; c.gridwidth = 1; c.weightx = 0; mainPanel.add(label("Brag Fact", false), c);
		c.gridx = 1; c.gridy = 5; c.gridwidth = 3; c.weightx = 1; mainPanel.add(entry("brag", d.getBrag()), c);

		c.gridx = 0; c.gridy = 6; c.gridwidth = 1; c.weightx = 0; mainPanel.add(label("Sponsor", false), c);
		c.gridx = 1; c.gridy = 6; c.gridwidth = 3; c.weightx = 1; mainPanel.add(entry("sponsor", d.getSponsor()), c);
		
		c.gridx = 0; c.gridy = 7; c.gridwidth = 1; c.weightx = 0; mainPanel.add(label("Membership", false), c);
		c.gridx = 1; c.gridy = 7; c.gridwidth = 3; c.weightx = 1; mainPanel.add(entry("membership", d.getMembership()), c);

		int row = 8;
		for (DriverField field : xfields)
		{
			c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0; mainPanel.add(label(field.getTitle(), false), c);
			c.gridx = 1; c.gridy = row; c.gridwidth = 3; c.weightx = 1; mainPanel.add(entry(field.getName(), d.getExtra(field.getName())), c);
			row++;
		}

		/* row 7 (vertical filler) */
		c.gridx = 0; c.gridy = row; c.gridwidth = 4; c.weightx = 0; c.weighty = 1; mainPanel.add(Box.createHorizontalStrut(450), c);

		result = d;
    }
	 

	/**
	 * Called after OK to verify data before closing.
	 */ 
	@Override
	public boolean verifyData()
	{
		if (getEntryText("firstname").equals("")) return false;
		if (getEntryText("lastname").equals("")) return false;
		return true;
	}


	/**
	 * Called after OK is pressed and before the dialog is closed.
	 */
	@Override
	public Driver getResult()
	{
		if (!valid)
			return null;
		
		result.setFirstName(getEntryText("firstname"));
		result.setLastName(getEntryText("lastname"));
		result.setEmail(getEntryText("email"));
		result.setAddress(getEntryText("address"));
		result.setCity(getEntryText("city"));
		result.setState(getEntryText("state"));
		result.setZip(getEntryText("zip"));
		result.setPhone(getEntryText("phone"));
		result.setBrag(getEntryText("brag"));
		result.setSponsor(getEntryText("sponsor"));
		result.setMembership(getEntryText("membership"));
		for (DriverField field : xfields)
		{
			result.setExtra(field.getName(), getEntryText(field.getName()));
		}
		return result;
	}

}

