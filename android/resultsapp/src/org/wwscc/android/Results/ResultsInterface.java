package org.wwscc.android.Results;

import org.json.JSONArray;

public class ResultsInterface 
{	
	public static interface DataDest
	{
		public void updateData(JSONArray newData);
	}

	public static interface DataSource
	{
		public void startListening(DataDest dest, ResultsViewConfig type);
		public void stopListening(DataDest dest);
		public void stopListeningAll();
	}
}
