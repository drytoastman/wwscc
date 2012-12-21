package org.wwscc.dataentry.announcer;

import org.wwscc.storage.Entrant;
import org.wwscc.storage.EventResult;

public class FakeResult extends EventResult
{
	Entrant entrant;
	String type;
	Double theorectical;

	public FakeResult(Entrant e, String t, Double theory)
	{
		entrant = e;
		type = t;
		theorectical = theory;
	}
	
	public String getFullName() { return entrant.getFirstName() + " " + entrant.getLastName(); }
	public String getIndexCode() { return "";}
	public String getIndexStr() { return "";}
	public double getSum() {return theorectical; }
	public String getType() { return type; }
}
