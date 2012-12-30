
package org.wwscc.android.Results;

public class ResultsViewConfig 
{
	String classcode, type;
	public ResultsViewConfig()
	{
		classcode = "";
		type = "";
	}
	
	public ResultsViewConfig(String clazz, String typ) 
	{ 
		classcode = clazz; 
		type = typ; 
	}
	
	@Override
	public String toString()
	{
		return classcode+"/"+type;
	}
	
	public ResultsViewConfig decode(String s)
	{
		String p[] = s.split("/");
		if (p.length < 2)
			return this;
		classcode = p[0];
		type = p[1];
		return this;
	}
	
	@Override
	public int hashCode() 
	{ 
		return type.hashCode() ^ classcode.hashCode(); 
	}
	
	@Override
	public boolean equals(Object o) 
	{
		if (o instanceof ResultsViewConfig)
		{
			ResultsViewConfig t = (ResultsViewConfig)o;
			return (t.classcode.equals(classcode) && (t.type.equals(type)));
		}
		return false;
	}
}