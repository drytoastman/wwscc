/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wwscc.challenge;

/**
 *
 * @author bwilson
 */
public class ActivationRequest 
{
	final Id.Run runToChange;
	/** true to activate, false to deactivate */
	final boolean makeActive;
	final boolean sendDials;
	
	public ActivationRequest(Id.Run run, boolean activate)
	{
		this(run, activate, false);
	}
	
	public ActivationRequest(Id.Run run, boolean activate, boolean dials)
	{
		runToChange = run;
		makeActive = activate;
		sendDials = dials;
	}
}
