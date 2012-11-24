/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.wwscc.components;

import javax.swing.JLabel;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;

/**
 *
 * @author bwilson
 */
public class MyServerLabel extends JLabel implements MessageListener
{
	public MyServerLabel()
	{
		super("Server Uninitialized");
		setHorizontalAlignment(CENTER);
		Messenger.register(MT.TIMER_SERVICE_LISTENING, this);
		Messenger.register(MT.TIMER_SERVICE_NOTLISTENING, this);
	}

	@Override
	public void event(MT type, Object o)
	{
		switch (type)
		{
			case TIMER_SERVICE_LISTENING:
				Object a[] = (Object[])o;
				setText("Server On: " + a[1] + ":" + a[2]);
				break;
			case TIMER_SERVICE_NOTLISTENING:
				setText("Server Off");
				break;
		}
	}
}
