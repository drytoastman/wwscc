/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.wwscc.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.Timer;

/**
 *
 * @author bwilson
 */
public class MyIpLabel extends JLabel implements ActionListener
{
	public MyIpLabel()
	{
		super("");
		setHorizontalAlignment(CENTER);
		actionPerformed(null);
		new Timer(3000, this).start();
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		try {
			setText("My IP: " + InetAddress.getLocalHost().getHostAddress());
		} catch (UnknownHostException ex) {
			Logger.getLogger(MyIpLabel.class.getName()).info("Failed to get local IP: " + ex);
		}
	}
}
