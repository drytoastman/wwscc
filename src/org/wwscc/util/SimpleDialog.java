/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.util;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Similar to showOptionPane but allows us to change initial focus or other things.
 */
public class SimpleDialog extends JDialog implements ActionListener
{
	protected boolean ok;

	/**
	 * Create the dialog.
	 * @param parent	the parent Frame if any
	 * @param title		the title to use for the window
	 * @param useCancel	whether to show a cancel button or not
	 */
    public SimpleDialog(String title, JComponent panel)
	{
        super();
		setTitle(title);
		setModal(true);
		//setModalityType(Dialog.ModalityType.APPLICATION_MODAL);  // As of 1.6, we compile for 1.5

		ok = false;
		
		JButton okb = new JButton("OK");
		JButton cancel = new JButton("Cancel");
		okb.addActionListener(this);
		cancel.addActionListener(this);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okb);
		buttonPanel.add(cancel);

		Icon ic = UIManager.getIcon("OptionPane.questionIcon");
		JLabel icon = new JLabel(ic);
		icon.setBorder(new EmptyBorder(0,5,0,5));

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(10,10,5,10));
		content.add(icon, BorderLayout.WEST);
		content.add(buttonPanel, BorderLayout.SOUTH);

		setContentPane(content);
		getRootPane().setDefaultButton(okb);
		
		if (panel != null)
			setPanel(panel);
    }
	
	public SimpleDialog(String title)
	{
		this(title, null);
	}
 
	public void setPanel(JComponent panel)
	{
		getContentPane().add(panel, BorderLayout.CENTER);
	}
	
	public boolean wasOK()
	{
		return ok;
	}
 
	public boolean runDialog()
	{
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
		dispose();
		return ok;
	}
	
	public void actionPerformed(ActionEvent ae)
	{
		String button = ae.getActionCommand();
	
		if (button.equals("Cancel"))
		{
			setVisible(false);
		}
		else if (button.equals("OK"))
		{
			ok = true;
			setVisible(false);
		}
	}
}

