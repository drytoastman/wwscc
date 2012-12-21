package org.wwscc.dataentry.announcer;

import org.wwscc.storage.EventResult;

public class FakeResult extends EventResult
{
	String type;
	String fullname;
	
	public FakeResult(EventResult r, String t, Double theory)
	{
		type = t;
		sum = theory;
		courses = r.getCourseCount();
		fullname = r.getFullName();
	}
	
	public String getFullName() { return fullname; }
	public String getIndexCode() { return "";}
	public String getIndexStr() { return "";}
	public String getType() { return type; }
}
