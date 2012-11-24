/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dialogs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.miginfocom.swing.MigLayout;
import org.wwscc.services.FoundService;
import org.wwscc.services.JServiceList;
import org.wwscc.services.ServiceFinder;


/**
 * Dialog that queries for a service on the network.
 */
@SuppressWarnings("serial")
public class SimpleFinderDialog extends BaseDialog<InetSocketAddress> implements ListSelectionListener
{
	//private static final Logger log = Logger.getLogger(SimpleFinderDialog.class.getCanonicalName());

	JServiceList list;
	ServiceFinder finder;
	
	/**
	 * shortcut when only looking for a single name
	 * @param serviceName
	 */
	public SimpleFinderDialog(String serviceName)
	{
		this(Arrays.asList(new String[] { serviceName }));
	}
	
	/**
	 * Create the dialog
	 * 
	 * @param serviceNames the service names to look for
	 */
    public SimpleFinderDialog(List<String> serviceNames)
	{
		super(new MigLayout(""), false);

		list = new JServiceList();
		list.addListSelectionListener(this);
		//list.mapIcon(servicename, icon) for custom icons
		
		JScrollPane p = new JScrollPane(list);
		try
		{
			finder = new ServiceFinder(serviceNames);
			finder.addListener(list);
			finder.start();
			mainPanel.add(p, "w 300, h 400, spanx 2, wrap");
		}
		catch (IOException ioe)
		{
			mainPanel.add(label("ServiceFinder failed to start: " + ioe.getMessage(), false), "wrap");
		}
		
		mainPanel.add(label("Host", false), "");
		mainPanel.add(entry("host", ""), "growx, wrap");
		mainPanel.add(label("Port", false), "");
		mainPanel.add(ientry("port", 0), "growx, wrap");
		result = null;
    }

	@Override
	public void close()
	{
		finder.stop();
		super.close();
	}
	
	/**
	 * Called after OK to verify data before closing.
	 */ 
	@Override
	public boolean verifyData()
	{
		try {
			result = new InetSocketAddress(getEntryText("host"), getEntryInt("port"));
		} catch (Exception e) {
			result = null;
		}
		return (result != null);
	}

	@Override
	public void valueChanged(ListSelectionEvent e) 
	{
		FoundService f = list.getSelectedValue();
		if (f != null)
		{
			setEntryText("host", f.getHost().getHostAddress());
			setEntryText("port", String.valueOf(f.getPort()));
		}
		else
		{
			setEntryText("host", "");
			setEntryText("port", "");
		}
	}
}


