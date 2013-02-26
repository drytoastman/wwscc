package org.wwscc.storage;

/**
 * Add some meta information that can be loaded base on current state but it associated 
 * directly with a Car entry.  This make sure that anyone using the information is getting
 * something that was required to be set during instantiation.
 */
public class MetaCar extends Car 
{
	private static final long serialVersionUID = 7446741644331347649L;
	
	private boolean isRegistered;
	private boolean isInRunOrder;
	private boolean hasActivity;
	
	public MetaCar(Car c, boolean reg, boolean order, boolean activity)
	{
		super(c);
		isRegistered = reg;
		isInRunOrder = order;
		hasActivity = activity;
	}
	
	public boolean isRegistered() { return isRegistered; }
	public boolean isInRunOrder() { return isInRunOrder; }
	public boolean hasActivity() { return hasActivity; }
}
