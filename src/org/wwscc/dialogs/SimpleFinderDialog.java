/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dialogs;

import java.io.IOException;
import java.util.logging.Logger;
import net.miginfocom.swing.MigLayout;
import org.wwscc.services.FoundService;
import org.wwscc.services.JServiceList;
import org.wwscc.services.ServiceFinder;


/**
 */
public class SimpleFinderDialog extends BaseDialog<FoundService>
{
	private static Logger log = Logger.getLogger(SimpleFinderDialog.class.getCanonicalName());

	JServiceList list;
	ServiceFinder finder;
	
	/**
	 * Create the dialog
	 * 
	 * @param serviceName the service name to look for
	 */
    public SimpleFinderDialog(String serviceName)
	{
		super(new MigLayout(""), false);

		list = new JServiceList();
		try
		{
			finder = new ServiceFinder(serviceName);
			finder.setListener(list);
			new Thread(finder).start();
			mainPanel.add(list, "wrap");
		}
		catch (IOException ioe)
		{
			mainPanel.add(label("ServiceFinder failed to start: " + ioe.getMessage(), false), "wrap");
		}
		
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
		result = list.getSelectedValue();
		return (result != null);
	}
}


