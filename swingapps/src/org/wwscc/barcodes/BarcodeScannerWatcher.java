/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */
package org.wwscc.barcodes;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Timer;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;
import org.wwscc.util.Prefs;

/**
 * EventDispatcher that watches incoming key characters to look for the type
 * of characters and timing that indicates input from a barcode scanner.  Turn
 * it into an internal event and keep the keystrokes from rest of the application.
 */
public class BarcodeScannerWatcher implements KeyEventDispatcher, MessageListener
{
	private static final Logger log = Logger.getLogger(BarcodeScannerWatcher.class.getCanonicalName());
	private final LinkedList<KeyEvent> queue;
	private final Timer queuePush;
	ScannerConfig config;

	public BarcodeScannerWatcher()
	{
		config = new ScannerConfig();
		config.decode(Prefs.getScannerConfig());
		Messenger.register(MT.SCANNER_OPTIONS, this);

		queue = new LinkedList<KeyEvent>();
		queuePush = new Timer(config.delay, null);
		queuePush.setCoalesce(true);
		queuePush.setRepeats(false);
		queuePush.addActionListener(new ActionListener() {
			// timeout, dump it all
			public void actionPerformed(ActionEvent ae) { dumpQueue(); }
		});
	}
	
	/**
	 * Implemented interface to respond to key events
	 * @param ke the incoming event
	 * @return always returns true, we will redispatch things ourselves if the queue doesn't convert
	 */
	@Override
	public boolean dispatchKeyEvent(KeyEvent ke) 
	{
		queue.addLast(ke);
		if (!scanQueue()) {
			dumpQueue();
			queuePush.stop();
		} else {
			queuePush.restart();
		}
		return true;
	}
	
	/**
	 * Run through the queue to see if we got a possible match of:
	 * [0] stx, [1 - (n-1)] Integers within time frame, [n] etx
	 * @return false if this doesn't or can't eventually match, true otherwise
	 */
	protected boolean scanQueue()
	{
		StringBuilder barcode = new StringBuilder();
		ListIterator<KeyEvent> iter = queue.listIterator();
		boolean firstChar = true;
		
		while (iter.hasNext())
		{
			KeyEvent ke = iter.next();
			if (ke.getID() == KeyEvent.KEY_TYPED)
			{
				Character c = ke.getKeyChar();
				if (firstChar)
				{
					if (c != config.stx)
						return false;
					firstChar = false;
					continue;
				}

				if (c == config.etx) {
					queue.clear();
					log.log(Level.FINE, "Scanned barcode {0}", barcode);
					Messenger.sendEvent(MT.BARCODE_SCANNED, barcode.toString());
					return false;  // time to clear
				}
							
				barcode.append(c);				
			}
			else // Don't let queue hang up on arrow key presses
			{
				return false;
			}
		}
		return true;
	}
	
	
	/**
	 * Dump all of the queued key events back into the regular system.
	 */
	synchronized protected void dumpQueue()
	{
		KeyboardFocusManager mgr = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		while (queue.size() > 0)
		{
			KeyEvent ke = queue.pop();
			mgr.redispatchEvent(ke.getComponent(), ke);
		}
	}

	@Override
	public void event(MT type, Object data) 
	{
		switch (type)
		{
			case SCANNER_OPTIONS:
				ScannerOptionsDialog dialog = new ScannerOptionsDialog(config);
				dialog.doDialog("Scanner Config", null);
				config = dialog.getResult();
				queuePush.setInitialDelay(config.delay);
				queuePush.setDelay(config.delay);
				Prefs.setScannerConfig(config.encode());
				break;
		}
	}
}
