/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wwscc.bwtimer;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

import org.wwscc.dialogs.SimpleFinderDialog;
import org.wwscc.storage.Run;
import org.wwscc.timercomm.TimerService;
import org.wwscc.util.Logging;
import org.wwscc.util.TimeTextField;

/**
 * Simple source to send debug times to listeners.
 */
public class DebugTimer extends JPanel
{
	private static Logger log = Logger.getLogger(DebugTimer.class.getCanonicalName());
	
	JButton defaultButton;
	TimeTextField tf;
	TimerService server;
	
	public DebugTimer() throws IOException
	{
		super(new MigLayout());

		tf = new TimeTextField("123.456", 6);
		
		defaultButton = new JButton("Send");
		defaultButton.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				server.sendRun(new Run(DebugTimer.this.tf.getTime()));
			}
		});
		
		add(tf, "w 200, wrap");
		add(defaultButton, "w 200, wrap");
		
		server = new TimerService(SimpleFinderDialog.BWTIMER_TYPE);
		server.start();
	}
		
		
	public static void main(String args[])
	{
		try
		{
			Logging.logSetup("bwtimer");
			DebugTimer t = new DebugTimer();
			
			JFrame f = new JFrame("DebugTimer");
			f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			f.getContentPane().add(t);
			f.pack();
			f.setVisible(true);

			t.getRootPane().setDefaultButton(t.defaultButton);
		}
		catch (Throwable e)
		{
			log.log(Level.SEVERE, "Timer stopped: " + e, e);
			e.printStackTrace();
		}
	}
}
