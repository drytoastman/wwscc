package org.wwscc.storage;

/**
 * Add some meta information that can be loaded base on current state but it associated 
 * directly with a Car entry.  This make sure that anyone using the information is getting
 * something that was required to be set during instantiation.
 */
public class MetaCar extends Car 
{	
	protected boolean isRegistered;
	protected boolean isInRunOrder;
	protected boolean hasActivity;
	protected boolean isPaid;
	
	public MetaCar(Car c)
	{
		super(c);
	}
	
	public boolean isPaid() { return isPaid; }
	public boolean isRegistered() { return isRegistered; }
	public boolean isInRunOrder() { return isInRunOrder; }
	public boolean hasActivity() { return hasActivity; }
}
