/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2017 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.tray;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.wwscc.util.Logging;
import org.wwscc.util.Prefs;

public class TrayMonitor implements ActionListener
{
	private static final Logger log = Logger.getLogger(TrayMonitor.class.getName());
	
    MenuItem mQuit;
    DockerMonitor monitor;
    Image coneok, conewarn;
    TrayIcon trayIcon;
    PopupMenu trayPopup;
    String cmdline[];
    
	public TrayMonitor(String args[])
	{
	    if (!SystemTray.isSupported()) 
	    {
	    	log.severe("TrayIcon is not supported, unable to run Scorekeeper monitor application.");
	    	System.exit(-1);
	    }
	    	    
	    cmdline = args;
        
        trayPopup   = new PopupMenu();        
        Menu launch = new Menu("Launch");
        trayPopup.add(launch);
        
        newMenuItem("DataEntry",     "DataEntry",    launch);
        newMenuItem("Registration",  "Registration", launch);
        newMenuItem("Syncronizer",   "Synchronizer", launch);
        newMenuItem("ProTimer",      "ProTimer",     launch);
        newMenuItem("BWTimer",       "BWTimer",      launch);
        newMenuItem("ChallengeGUI",  "ChallengeGUI", launch);
                
        mQuit      = newMenuItem("Quit",                "quit",      trayPopup);

        coneok   = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/org/wwscc/images/conesmall.png"));
        conewarn = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/org/wwscc/images/conewarn.png"));
        
        trayIcon = new TrayIcon(conewarn, "Scorekeeper Monitor", trayPopup);
        trayIcon.setImageAutoSize(true);
        
        // Do a check when opening the context menu
        trayIcon.addMouseListener(new MouseAdapter() {
    	    private void docheck(MouseEvent e) { if (e.isPopupTrigger()) { synchronized (monitor) { monitor.notify(); }}}
    	    @Override
    	    public void mouseReleased(MouseEvent e) { docheck(e); }
    	    @Override
    	    public void mousePressed(MouseEvent e)  { docheck(e); }
        });

        try {
        	SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
	    	log.severe("Failed to install TrayIcon: " + e);
	    	System.exit(-2);
        }

        monitor = new DockerMonitor();
        monitor.start();
	}

	private MenuItem newMenuItem(String initial, String cmd, Menu parent)
	{
		MenuItem m = new MenuItem(initial);
		m.setActionCommand(cmd);
		m.addActionListener(this);
		parent.add(m);
		return m;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		String cmd = e.getActionCommand();
		switch (cmd)
		{
			case "quit":
				if (JOptionPane.showConfirmDialog(null, "This will stop the datbase server and web server.  Is that ok?", 
					"Quit Scorekeeper", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
				{
                    synchronized (monitor)
                    {
                        monitor.done = true;
                        monitor.notify();
                    }
				}
				
			default:
				launch(cmd);
		}
	}
	
	protected void launch(String app)
	{
		try {
			ArrayList<String> cmd = new ArrayList<String>();
			if (Prefs.isWindows())
				cmd.add("javaw");
			else
				cmd.add("java");
			cmd.add("-cp");
			cmd.add(System.getProperty("java.class.path"));
			cmd.add("org.wwscc.util.Launcher");
			cmd.add(app);
			cmd.addAll(Arrays.asList(cmdline));
			log.info(String.format("Running %s", cmd));
			ProcessBuilder starter = new ProcessBuilder(cmd);
			starter.redirectErrorStream(true);
			starter.redirectOutput(Redirect.appendTo(new File(Prefs.getLogDirectory(), "jvmlaunches.log")));
			Process p = starter.start();
			Thread.sleep(1000);
			if (!p.isAlive()) {
				throw new Exception("Process not alive after 1 second, see jvmlaunched.log");
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, String.format("Failed to launch %s",  app), e);
		}
	}
	
	/**
	 * Thread to keep pinging our services to check their status.  It pauses for 3 seconds but can
	 * be woken by anyone calling notify on the class object.
	 */
    class DockerMonitor extends Thread
    {
    	boolean done = false;
        
        Image currentIcon;
        ProcessBuilder up;
        ProcessBuilder down;
        ProcessBuilder top;
        ProcessBuilder ps;

    	public DockerMonitor()
    	{
    		super("Docker Monitor");
            up   = new ProcessBuilder("docker-compose", "-p", "test2", "-f", "docker-compose.yaml", "up", "-d");
            down = new ProcessBuilder("docker-compose", "-p", "test2", "-f", "docker-compose.yaml", "down");
            top  = new ProcessBuilder("docker-compose", "-p", "test2", "-f", "docker-compose.yaml", "top");
            ps   = new ProcessBuilder("docker-compose", "-p", "test2", "-f", "docker-compose.yaml", "ps");
            currentIcon = null;
    	}
    	
    	@Override
    	public void run()
    	{
            try {
                log.info(String.format("Running %s", up.command().toString()));
                Process p = up.start();
                log.info("docker up returns " + p.waitFor());
            } catch (Exception ioe) {
                log.log(Level.SEVERE, "Unable to start services: " + ioe, ioe);
            }

    		while (!done)
    		{
    			try {
                    Process p = ps.start();
			        log.info(String.format("Running %s", ps.command().toString()));
                    log.info("docker ps returns " + p.waitFor());
                    InputStream is = p.getInputStream();
                    byte buf[] =  new byte[8192];
                    is.read(buf);
                    System.out.println(new String(buf));
                    is.close();
                        
                    Image next = coneok;
                    if (next != currentIcon) {
    					trayIcon.setImage(next); 
                        currentIcon = next;
                    }

    				synchronized (this) { this.wait(5000); }

				} catch (InterruptedException e) {
                } catch (IOException ioe) {
                }
    		}

            try {
                Process p = down.start();
                log.info("docker down returns " + p.waitFor());
            } catch (Exception ioe) {
                log.log(Level.SEVERE, "Unable to shutdown: " + ioe, ioe);
            }

            System.exit(0);
        }
    }
	

    /**
     * Main entry point.
     * @param args passed to any launched application, ignored otherwise
     */
	public static void main(String args[])
	{
		System.setProperty("swing.defaultlaf", UIManager.getSystemLookAndFeelClassName());
		Logging.logSetup("traymonitor");
		new TrayMonitor(args);
	}
}
