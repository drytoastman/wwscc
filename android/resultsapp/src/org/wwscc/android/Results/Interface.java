package org.wwscc.android.Results;

import org.json.JSONArray;

public class Interface 
{	
	public static interface DataDest
	{
		public void updateData(JSONArray newData);
	}

	public static interface DataSource
	{
		public void startListening(DataDest dest, int dataType, String classcode);
		public void stopListening(DataDest dest);
	}
}
