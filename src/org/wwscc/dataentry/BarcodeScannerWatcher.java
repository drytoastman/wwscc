/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */
package org.wwscc.dataentry;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.ListIterator;
import javax.swing.Timer;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;

/**
 * EventDispatcher that watches incoming key characters to look for the type
 * of characters and timing that indicates input from a barcode scanner.  Turn
 * it into an internal event and keep the keystrokes from rest of the application.
 */
public class BarcodeScannerWatcher implements KeyEventDispatcher
{
	private final LinkedList<KeyEvent> queue;
	private final Timer queuePush;
	Character stx, etx;
	
	public BarcodeScannerWatcher()
	{
		stx = '\002';
		etx = '\003';
		queue = new LinkedList<KeyEvent>();
		queuePush = new Timer(100, null);
		queuePush.setCoalesce(true);
		queuePush.setRepeats(false);
		queuePush.addActionListener(new ActionListener() {
			// timeout, dump it all
			public void actionPerformed(ActionEvent ae) { dumpQueue(); }
		});
	}
	
	/**
	 * Configure the attributes for the scanner
	 * @param stx
	 * @param etx
	 * @param spacing 
	 */
	public void configure(char stx, char etx, int spacing)
	{
		this.stx = stx;
		this.etx = etx;
		queuePush.setInitialDelay(spacing);
		queuePush.setDelay(spacing);
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
		StringBuilder toconvert = new StringBuilder();
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
					if (c != stx)
						return false;
					firstChar = false;
					continue;
				}

				if (c == etx) {
					try {
						queue.clear();
						Messenger.sendEvent(MT.SCANNER_INPUT, Integer.parseInt(toconvert.toString()));
					} catch (NumberFormatException nfe) {}
					return false;
				}
							
				if (!Character.isDigit(c))  // need to be '0'-'9'
					return false;				
				toconvert.append(c);				
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
}
