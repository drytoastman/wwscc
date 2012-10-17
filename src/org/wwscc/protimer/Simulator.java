/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wwscc.protimer;

import org.wwscc.util.MT;
import org.wwscc.util.Messenger;

/**
 * Simulate events from hardware without need for hardware.
 */
public class Simulator 
{		
	public Simulator()
	{
	}
	
	public void reaction(boolean left, double time, int color)
	{
		Messenger.sendEvent(left?MT.REACTION_LEFT:MT.REACTION_RIGHT, new ColorTime(time, color));
	}

	public void sixty(boolean left, double time, int color)
	{
		Messenger.sendEvent(left?MT.SIXTY_LEFT:MT.SIXTY_RIGHT, new ColorTime(time, color));
	}

	public void finish(boolean left, double time, double dial, int color)
	{
		Object [] result = { new ColorTime(time, color), dial };
		Messenger.sendEvent(left?MT.FINISH_LEFT:MT.FINISH_RIGHT, result);
	}

	public void win(boolean left)
	{
		Messenger.sendEvent(left?MT.WIN_LEFT:MT.WIN_RIGHT, null);
	}

	public void lead(boolean left, double time)
	{
		Messenger.sendEvent(left?MT.LEAD_LEFT:MT.LEAD_RIGHT, time);
	}

	public void challengewin(boolean left, double time)
	{
		Messenger.sendEvent(left?MT.CHALWIN_LEFT:MT.CHALWIN_RIGHT, time);
	}
	
	public void overdial(boolean left, double time, double dial)
	{
		Messenger.sendEvent(left?MT.CHALDIAL_LEFT:MT.CHALDIAL_RIGHT, new Double[] { time, dial } );
	}

	public void breakout(boolean left, double time, double dial)
	{
		Messenger.sendEvent(left?MT.CHALDIAL_LEFT:MT.CHALDIAL_RIGHT, new Double[] {-time, dial } );
	}
	
	public void dials(double left, double right)
	{
		Messenger.sendEvent(MT.DIALIN_LEFT, left);
		Messenger.sendEvent(MT.DIALIN_RIGHT, left);
	}
		
	public void tree()
	{
		Messenger.sendEvent(MT.TREE, null);
	}
}
