/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2009 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dialogs;

import java.awt.event.ActionEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.util.logging.Logger;
import javax.swing.JButton;
import net.miginfocom.swing.MigLayout;
import org.wwscc.util.FileChooser;


/**
 */
public class DatabaseDialog extends BaseDialog<Object>
{
	private static Logger log = Logger.getLogger("org.wwscc.dialogs.DatabaseDialog");


	/**
	 * Create the dialog
	 * @param fileDefault a possible default for the file database
	 * @param netDefault a possible default for the URL database
	 * @param defaultnetwork whether to default to the network radio or not
	 */
    public DatabaseDialog(String fileDefault, String netDefault, boolean defaultnetwork)
	{
		super(new MigLayout(""), false);

		JButton chooserOpen = new JButton("...");
		chooserOpen.addActionListener(this);

		if (fileDefault != null)
		{
			if (netDefault != null)
				mainPanel.add(radio("File"));
			else
				radio("File");
			mainPanel.add(entry("File", fileDefault), "wmin 200, spanx 2, grow");
			mainPanel.add(chooserOpen, "wrap");

			if (netDefault != null)
				mainPanel.add(label("", false), "hmin 20, wrap");
		}

		if (netDefault != null)
		{
			String split[] = netDefault.split("/");

			if (fileDefault != null)
				mainPanel.add(radio("Network"), "spany 2");
			else
				radio("Network");
			mainPanel.add(label("Host", false), "");
			mainPanel.add(entry("Host", split.length>0 ? split[0]:""), "wmin 200, grow, wrap");
			mainPanel.add(label("Name", false), "");
			mainPanel.add(entry("Name", split.length>1 ? split[1]:""), "grow, wrap");
		}

		result = null;
		if ((netDefault != null) && (fileDefault != null))
			setSelectedRadio(defaultnetwork ? "Network" : "File");
		else if (fileDefault == null)
			setSelectedRadio("Network");
		else
			setSelectedRadio("File");
    }
	 

	@Override
	public void actionPerformed(ActionEvent ae)
	{
		String cmd = ae.getActionCommand();

		if (cmd.equals("..."))
		{
			File res = FileChooser.open("Select Database", "Database", "db", new File(getEntryText("File")));
			if (res != null)
				setEntryText("File", res.getAbsolutePath());
		}
		else
		{
			super.actionPerformed(ae);
		}
	}

	/**
	 * Called after OK to verify data before closing.
	 */ 
	@Override
	public boolean verifyData()
	{
		result = null;
		try
		{
			String option = getSelectedRadio();
			if (option.equals("File"))
			{
				File f = new File(getEntryText("File"));
				if (f.canWrite())
					result = f;
				else
					throw new MalformedURLException("File is not writable");
			}
			else if (option.equals("Network"))
			{
				String host = getEntryText("Host");
				String name = getEntryText("Name");
				if (host.equals("") || name.equals(""))
					throw new MalformedURLException("Need a hostname and database name");

				result = host+"/"+name;
			}
		}
		catch (MalformedURLException e)
		{
			log.severe("Can't use database source: " + e.getMessage());
		}
		catch (Exception e)
		{
			log.severe("Bad data in DatabaseDialog: " + e);
		}
		
		return (result != null);
	}


	/**
	 * Data is good, return it.
	 */
	@Override
	public Object getResult()
	{
		return result;
	}
}


