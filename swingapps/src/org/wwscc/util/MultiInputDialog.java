/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.util;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author bwilson
 */
public class MultiInputDialog extends SimpleDialog
{
	private static Logger log = Logger.getLogger(MultiInputDialog.class.getCanonicalName());
	JPanel panel;
	HashMap <String,JTextField> texts;
	
	
	public MultiInputDialog(String title)
	{
		this(title, null, null);
	}
	
	public MultiInputDialog(String title, String instruction)
	{
		this(title, null, instruction);
	}
	
	public MultiInputDialog(String title, String instruction1, String instruction2)
	{
		super(title);
		
		panel = new JPanel(new MigLayout("", "[align right][160,fill][]"));
		
		if (instruction1 != null)
			panel.add(new JLabel(instruction1), "spanx 3, wrap");
		if (instruction2 != null)
			panel.add(new JLabel(instruction2), "spanx 3, wrap, gapbottom 10");

		texts = new HashMap<String, JTextField>();
		setPanel(panel);
	}

	public void addString(String prompt)
	{
		addString(prompt, null, true);
	}
	
	public void addString(String prompt, String defaultValue)
	{
		addString(prompt, defaultValue, true);
	}
	
	public void addString(String prompt, String defaultValue, boolean echo)
	{
		JTextField tf;
		
		if (echo)
			tf = new JTextField();
		else
			tf = new JPasswordField();

		if (defaultValue != null)
			tf.setText(defaultValue);
		
		panel.add(new JLabel(prompt), "");
		panel.add(tf, "wrap");
		
		texts.put(prompt, tf);
	}

	public void addFile(String prompt)
	{
		addFile(prompt, null);
	}
	
	public void addFile(String prompt, String defaultValue)
	{
		JTextField tf = new JTextField();
		if (defaultValue != null)
			tf.setText(defaultValue);

		JButton but = new JButton("...");
		but.setActionCommand("sf-"+prompt);
		but.addActionListener(this);
		
		panel.add(new JLabel(prompt), "");
		panel.add(tf, "");
		panel.add(but, "wrap");
		
		texts.put(prompt, tf);
	}

	public String getResponse(String prompt)
	{
		return texts.get(prompt).getText();
	}
	
	@Override
	public void actionPerformed(ActionEvent ae)
	{
		String cmd = ae.getActionCommand();
		if (cmd.startsWith("sf-"))
		{
			try
			{
				String prompt = cmd.substring(3);
				File f = FileChooser.open("Select a file", null, null, 
						new File(texts.get(prompt).getText()));
				if (f != null)
					texts.get(prompt).setText(f.getAbsolutePath());
			}
			catch (Exception e)
			{
				log.info("Error selecting file: " + e);
			}
		}
		else
			super.actionPerformed(ae);
	}
}
