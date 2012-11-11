/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wwscc.barcodes;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ScannerConfig 
{
	private static final Logger log = Logger.getLogger(ScannerConfig.class.getCanonicalName());
	
	char stx;
	char etx; 
	int delay;

	public ScannerConfig()
	{
		stx = '\002';
		etx = '\003';
		delay = 100;
	}
	
	public ScannerConfig(char s, char e, int d) 
	{ 
		stx = s; 
		etx = e; 
		delay = d; 
	}
	
	public String encode()
	{
		return String.format("%d;%d;%d", (int)stx, (int)etx, delay);
	}
	
	public void decode(String s)
	{
		try
		{
			String p[] = s.split(";");
			stx = (char)Integer.parseInt(p[0]);
			etx = (char)Integer.parseInt(p[1]);
			delay = Integer.parseInt(p[2]);
		}
		catch (Exception e)
		{
			log.log(Level.INFO, "Failed to decode scanner config: {0}", e.getMessage());
		}
	}
}
