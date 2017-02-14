/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2017 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.util;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class PostgresqlControl implements ServiceControl
{
	private static final Logger log = Logger.getLogger(PostgresqlControl.class.getName());
	ProcessBuilder create, starter, stopper, checker;
	
	public PostgresqlControl()
	{
		File installdir = new File(Prefs.getInstallRoot());
		String pgctl  = new File(installdir, "postgresql-9.6.2/bin/pg_ctl").getAbsolutePath();
		String initdb = new File(installdir, "pgdb").getAbsolutePath();
		String dbdir  = new File(installdir, "scorekeeperdb").getAbsolutePath();
		create  = new ProcessBuilder(initdb, "-D", dbdir, "-U", "postgres");
		checker = new ProcessBuilder(pgctl,  "-D", dbdir, "status");
		starter = new ProcessBuilder(pgctl,  "-D", dbdir, "start");
		stopper = new ProcessBuilder(pgctl,  "-D", dbdir, "stop");
	}

	public boolean createDatabase()
	{
		try {
			return create.start().waitFor() != 0;
		} catch (InterruptedException | IOException e) {
			log.warning("failed to start create database directory: " + e);
		}
		return false;
	}
	
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
			return starter.start().waitFor() != 0;
		} catch (InterruptedException | IOException e) {
			log.info("failed to start postgresql: " + e);
		}
		return false;
	}
	
	@Override
	public boolean stop()
	{
		try {
			return stopper.start().waitFor() != 0;
		} catch (InterruptedException | IOException e) {
			log.info("failed to stop postgresql: " + e);
		}
		return false;
	}
}
