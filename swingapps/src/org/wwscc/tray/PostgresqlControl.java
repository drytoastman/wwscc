/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2017 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.tray;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.wwscc.util.Prefs;

public class PostgresqlControl implements ServiceControl
{
	private static final Logger log = Logger.getLogger(PostgresqlControl.class.getName());
	ProcessBuilder starter, stopper, checker;
	String logfile;
	
	public PostgresqlControl()
	{
		if (Prefs.isWindows())
		{
			String pgctl = new File(Prefs.getInstallRoot(), "postgresql/bin/pg_ctl").getAbsolutePath();
			String dbdir = new File(Prefs.getInstallRoot(), "database").getAbsolutePath();
			logfile = new File(Prefs.getLogDirectory(), "postgresql.log").getAbsolutePath();
			checker = new ProcessBuilder(pgctl,  "-D", dbdir, "status");
			starter = new ProcessBuilder(pgctl,  "-D", dbdir, "-l", logfile, "start");
			stopper = new ProcessBuilder(pgctl,  "-D", dbdir, "stop");
		}
		else if (Prefs.isLinux())
		{
			checker = new ProcessBuilder("/etc/init.d/postgresql", "status");
			starter = new ProcessBuilder("/etc/init.d/postgresql", "start");
			stopper = new ProcessBuilder("/etc/init.d/postgresql", "stop");
		}
		else
		{
			JOptionPane.showMessageDialog(null, "Don't know how to control Postgresql on " + System.getProperty("os.name"),
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	/*
	public boolean createDatabase()
	{
		String initdb  = new File(installdir, "initdb").getAbsolutePath();
		ProcessBuilder create  = new ProcessBuilder(initdb, "-D", dbdir, "-U", "postgres");
		try {
			return create.start().waitFor() != 0;
		} catch (InterruptedException | IOException e) {
			log.warning("failed to start create database directory: " + e);
		}
		return false;
	}
	*/
	
	@Override
	public boolean check()
	{
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

		return false;
	}
	
	@Override
	public boolean start()
	{
		try {
			log.info("Running " + starter.command().toString());
			return starter.start().waitFor() == 0;
		} catch (InterruptedException | IOException e) {
			log.info("failed to start postgresql: " + e);
		}
		return false;
	}
	
	@Override
	public boolean stop()
	{
		try {
			log.info("Running " + stopper.command().toString());
			return stopper.start().waitFor() == 0;
		} catch (InterruptedException | IOException e) {
			log.info("failed to stop postgresql: " + e);
		}
		return false;
	}
	
	public void openlog()
	{
		try {
			Desktop.getDesktop().open(new File(logfile));
		} catch (IOException e) {
			log.warning("Unable to open log file: " + e);
		};
	}
}
