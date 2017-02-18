/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2017 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.util;

import java.io.File;

public class CherryPyControl implements ServiceControl
{
	//private static final Logger log = Logger.getLogger(CherryPyControl.class.getName());
	ProcessBuilder starter, stopper, checker;
	
	public CherryPyControl()
	{
		File installdir = new File(Prefs.getInstallRoot());
		String cpctl = new File(installdir, "python/bin/cherrypy").getAbsolutePath();
		checker = new ProcessBuilder(cpctl, "status");
		starter = new ProcessBuilder(cpctl, "start");
		stopper = new ProcessBuilder(cpctl, "stop");
	}

	@Override
	public boolean check()
	{
		/*
		try {
			Process p = checker.start();
			int returncode = p.waitFor();
			switch (returncode) 
			{
				case 0:  return true;
				case 3:  return false;
				default: log.info(String.format("postgresql check returned %d\n", returncode));
			}
		} catch (InterruptedException | IOException e) {
			log.info("failed to get postgresql status: " + e);
		}
		*/
		return true;
	}
	
	@Override
	public boolean start()
	{
		/*
		try {
			return starter.start().waitFor() != 0;
		} catch (InterruptedException | IOException e) {
			log.info("failed to start cherrypy server: " + e);
		} */
		return false;
	}
	
	@Override
	public boolean stop()
	{
		/*
		try {
			return stopper.start().waitFor() != 0;
		} catch (InterruptedException | IOException e) {
			log.info("failed to stop cherrypy server: " + e);
		} */
		return false;
	}

	@Override
	public void openlog() 
	{
		// TODO Auto-generated method stub
	}
}
