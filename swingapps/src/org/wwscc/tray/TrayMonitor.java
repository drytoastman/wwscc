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
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.wwscc.util.CherryPyControl;
import org.wwscc.util.PostgresqlControl;
import org.wwscc.util.ServiceControl;

public class TrayMonitor implements ActionListener
{
	private static final Logger log = Logger.getLogger(TrayMonitor.class.getName());
	
	public static final String DATABASE_NOT_RUNNING  = "Start Database Server";
	public static final String DATABASE_RUNNING      = "Stop Database Server";
	public static final String WEBSERVER_NOT_RUNNING = "Start Webserver";
	public static final String WEBSERVER_RUNNING     = "Stop Webserver";
	
    ServiceControl databaseCtrl, webserverCtrl;
    boolean databaseRunning, webserverRunning;
    MenuItem mDatabase, mWebServer, mQuit;
    BackgroundChecker checker;
    Image coneok, conewarn;
    TrayIcon trayIcon;
    PopupMenu trayPopup;
    
	public TrayMonitor()
	{
	    if (!SystemTray.isSupported()) 
	    {
	    	log.severe("TrayIcon is not supported, unable to run Scorekeeper monitor application.");
	    	System.exit(-1);
	    }
        
        trayPopup  = new PopupMenu();        
        Menu launch = new Menu("Launch");
        trayPopup.add(launch);
        
        newMenuItem("DataEntry",    "dataentry",    launch);
        newMenuItem("Registration", "registration", launch);
        newMenuItem("Syncronizer",  "synchronizer", launch);
        
        mDatabase  = newMenuItem(DATABASE_NOT_RUNNING,  "database",  trayPopup);
        mWebServer = newMenuItem(WEBSERVER_NOT_RUNNING, "webserver", trayPopup);
        mQuit      = newMenuItem("Quit",                "quit",      trayPopup);

        coneok   = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/org/wwscc/images/conesmall.png"));
        conewarn = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/org/wwscc/images/conewarn.png"));
        
        trayIcon = new TrayIcon(conewarn, "Scorekeeper Monitor", trayPopup);
        trayIcon.setImageAutoSize(true);
        
        // Do a check when opening the context menu
        trayIcon.addMouseListener(new MouseAdapter() {
    	    private void docheck(MouseEvent e) { if (e.isPopupTrigger()) { synchronized (checker) { checker.notify(); }}}
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

        databaseCtrl = new PostgresqlControl();
        webserverCtrl = new CherryPyControl();
        
        databaseCtrl.start();
        webserverCtrl.start();
        checker = new BackgroundChecker();
        checker.start();
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
			case "database":
	    		if (databaseRunning) databaseCtrl.stop();
	    		else                 databaseCtrl.start();
	    		break;
	    	
			case "webserver":
				if (webserverRunning) webserverCtrl.stop();
				else                  webserverCtrl.start();
				break;
				
			case "quit":
				if (JOptionPane.showConfirmDialog(null, "This will stop the datbase server and web server.  Is that ok?", 
					"Quit Scorekeeper", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
				{
					databaseCtrl.stop();
					webserverCtrl.stop();
					System.exit(0);
				}
				
			default:
				System.out.println("other command " + cmd);
		}
	}
	
	/**
	 * Thread to keep pinging our services to check their status.  It pauses for 3 seconds but can
	 * be woken by anyone calling notify on the class object.
	 */
    class BackgroundChecker extends Thread
    {
    	boolean done = false;
    	@Override
    	public void run()
    	{
    		while (!done)
    		{
    			try {
    				databaseRunning = databaseCtrl.check();
    				webserverRunning = webserverCtrl.check();
					mDatabase.setLabel(databaseRunning ? DATABASE_RUNNING : DATABASE_NOT_RUNNING);
					mWebServer.setLabel(webserverRunning ? WEBSERVER_RUNNING : WEBSERVER_NOT_RUNNING);
					trayIcon.setImage((databaseRunning && webserverRunning) ? coneok : conewarn);
    				synchronized (this) { this.wait(3000); }

				} catch (InterruptedException e) {}
    		}
    	}
    }
	
    /**
     * Main entry point.
     * @param args ignored
     */
	public static void main(String args[])
	{
		System.setProperty("swing.defaultlaf", UIManager.getSystemLookAndFeelClassName());
		new TrayMonitor();
	}
}
