/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2009 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dialogs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import net.miginfocom.swing.MigLayout;


/**
 */
public class PortDialog extends BaseDialog<String>
{
	//private static Logger log = Logger.getLogger(PortDialog.class.getCanonicalName());

	/**
	 * Create the dialog
	 * @param fileDefault a possible default for the file database
	 * @param urlDefault a possible default for the URL database
	 */
    public PortDialog(String def, Collection<String> available, Collection<String> unavailable)
	{
		super(new MigLayout(""), false);

		ArrayList<String> ports = new ArrayList<String>();
		ports.addAll(available);
		ports.addAll(unavailable);
		Collections.sort(ports);

		for (String p : ports)
		{
			mainPanel.add(radio(p), "w 150!, gapleft 20, wrap");
			if (unavailable.contains(p))
				radioEnable(p, false);
		}

		result = null;
		setSelectedRadio(def);
    }

	/**
	 * Called after OK to verify data before closing.
	 */ 
	@Override
	public boolean verifyData()
	{
		result = getSelectedRadio();
		return (result != null);
	}


	/**
	 * Data is good, return it.
	 */
	@Override
	public String getResult()
	{
		return result;
	}
}


