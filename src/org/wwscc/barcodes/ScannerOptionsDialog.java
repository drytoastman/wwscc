/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.barcodes;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import net.miginfocom.swing.MigLayout;
import org.wwscc.dialogs.BaseDialog;


/**
 * Core functions for all dialogs.
 */
public class ScannerOptionsDialog extends BaseDialog<ScannerConfig>
{
	private static final Logger log = Logger.getLogger(ScannerOptionsDialog.class.getCanonicalName());
	private static List<String> options = Arrays.asList(new String[] { "STX", "ETX", "CR", "NL" });
	private static List<Character> matches = Arrays.asList(new Character[] { '\002', '\003', '\r', '\n' });
	
	public ScannerOptionsDialog(ScannerConfig config)
	{
		super(new MigLayout(""), true);
		
		mainPanel.add(label("Start Character", true), "");
		mainPanel.add(select("stx", options.get(matches.indexOf(config.stx)), options, null), "wrap");
		mainPanel.add(label("End Character", true), "");
		mainPanel.add(select("etx", options.get(matches.indexOf(config.etx)), options, null), "wrap");
		mainPanel.add(label("Max Delay (ms)", true), "");
		mainPanel.add(ientry("delay", config.delay), "growx, wrap");
	}
	
	@Override
	public boolean verifyData()
	{
		return true;
	}

	/**
	 * OK was pressed, data was verified, now return it.
	 */
	@Override
	public ScannerConfig getResult()
	{
		result = new ScannerConfig();
		result.stx = matches.get(options.indexOf(getSelect("stx")));
		result.etx = matches.get(options.indexOf(getSelect("etx")));
		result.delay = getEntryInt("delay");
		return result;
	}
}
