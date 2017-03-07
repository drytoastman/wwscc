/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2017 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.tray;

import java.awt.Desktop;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.wwscc.util.Prefs;

public class CherryPyControl implements ServiceControl
{
	private static final Logger log = Logger.getLogger(CherryPyControl.class.getName());
	ProcessBuilder starter;
	Process runner;
	Path python, server, pidfile, logfile;
	List<String> stopcommand;
	
	public CherryPyControl()
	{
		python  = FileSystems.getDefault().getPath(Prefs.getInstallRoot(), "webserver/Scripts/python");
		server  = FileSystems.getDefault().getPath(Prefs.getInstallRoot(), "webserver/Scripts/webserver");
		pidfile = FileSystems.getDefault().getPath(Prefs.getInstallRoot(), "logs/webserver.pid");
		logfile = FileSystems.getDefault().getPath(Prefs.getInstallRoot(), "logs/webserver.log");
		stopcommand = new ArrayList<String>();
		
		starter = new ProcessBuilder(python.toString(), server.toString());
		if (Prefs.isWindows())
		{
			stopcommand.add("taskkill");
			stopcommand.add("/T");
			stopcommand.add("/F"); 
			stopcommand.add("/PID");
			stopcommand.add("0");
		}
		else if (Prefs.isLinux())
		{
			stopcommand.add("kill");
			stopcommand.add("0");
		}
		else
		{
			JOptionPane.showMessageDialog(null, "Don't know how to control cherrypy on " + System.getProperty("os.name"),
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Check to see if our process handler is valid and running, otherwise check by socket connection (someone else started it
	 */
	@Override
	public boolean check()
	{
		boolean ret = false;
		try
		{
			if ((runner != null) && runner.isAlive())
				return true;
			
			Socket s = new Socket("127.0.0.1", 8080);
			ret = true;
			s.close();
		}
		catch (ConnectException ce) { /* can't connect, fall out to false */ }
		catch (Exception e) { log.info("cherrypy check failure: " + e); }
		
		return ret;
	}
	
	
	@Override
	public boolean start()
	{
		try {
			log.info("Running " + starter.command().toString());
			runner = starter.start();
			return true;
		} catch (IOException e) {
			log.info("failed to start cherrypy server: " + e);
		}
		return false;
	}
	
	/**
	 * If we have a valid handle, kill from there, otherwise we try to kill using the pidfile
	 */
	@Override
	public boolean stop()
	{
		try {
			if (runner != null)
			{
				runner.destroy();
				runner = null;
				return true;
			}
			
			List<String> lines = Files.readAllLines(pidfile, Charset.defaultCharset());
			Integer pid = new Integer(lines.get(0));
			stopcommand.set(stopcommand.size()-1, pid.toString());
			
			log.info("Running " + stopcommand.toString());
			return new ProcessBuilder(stopcommand).start().waitFor() != 0;
		} catch (Exception e) {
			log.info("failed to stop cherrypy server: " + e);
		}
		return false;
	}

	@Override
	public void openlog() 
	{
		try {
			Desktop.getDesktop().open(logfile.toFile());
		} catch (IOException e) {
			log.warning("Unable to open log file: " + e);
		}
	}
}
