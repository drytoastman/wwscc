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
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.miginfocom.swing.MigLayout;
import org.wwscc.services.FoundService;
import org.wwscc.services.JServiceList;
import org.wwscc.services.ServiceFinder;
import org.wwscc.util.CancelException;
import org.wwscc.util.FileChooser;


/**
 */
public class DatabaseDialog extends BaseDialog<Object> implements ListSelectionListener
{
	private static Logger log = Logger.getLogger(DatabaseDialog.class.getCanonicalName());

	ServiceFinder finder;
	JServiceList list;

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
			
			try
			{
				list = new JServiceList();
				list.addListSelectionListener(this);
				JScrollPane scroll = new JScrollPane(list);
				scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
				
				finder = new ServiceFinder("RemoteDatabase");
				finder.addListener(list);
				finder.start();
				mainPanel.add(scroll, "w 300, h 400, spanx 2, skip, wrap");
			}
			catch (Exception e)
			{
				mainPanel.add(label("Unabled to start ServiceFinder: " + e.getMessage(), false), "");
			}
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
	public void close()
	{
		if (finder != null)
			finder.stop();
		super.close();
	}
	
	/**
	 * Called when a new selection is made in the service finder. 
	 * @param e 
	 */
	@Override
	public void valueChanged(ListSelectionEvent e) 
	{
		FoundService f = list.getSelectedValue();
		if (f != null)
		{
			setEntryText("Host", f.getHost().getHostAddress());
			setEntryText("Name", String.valueOf(f.getId()));
			setSelectedRadio("Network");
		}
		else
		{
			setEntryText("Host", "");
			setEntryText("Name", "");
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent ae)
	{
		String cmd = ae.getActionCommand();

		if (cmd.equals("..."))
		{
			File res = FileChooser.open("Select Database", "Database", "db", new File(getEntryText("File")));
			if (res != null)
			{
				setEntryText("File", res.getAbsolutePath());
				setSelectedRadio("File");
			}
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
	
	
	/**
	 * Common call functionality in one place for looking up a network connected database
	 * @param title the dialog title
	 * @param defaultspec the default network spec "host/name"
	 * @return null if cancelled, otherwise the string spec split by "/"
	 * @throws CancelException 
	 */
	public static String[] netLookup(String title, String defaultspec) throws CancelException
	{
		DatabaseDialog dd = new DatabaseDialog(null, defaultspec, true);
		dd.doDialog(title, null);
		String ret = (String)dd.getResult();
		if (ret == null) throw new CancelException();
		String spec[] = ret.split("/");
		return spec;
	}
}


