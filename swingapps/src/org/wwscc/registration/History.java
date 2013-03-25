package org.wwscc.registration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import org.wwscc.registration.attendance.Processor;
import org.wwscc.util.CSVParser;
import org.wwscc.util.Prefs;

public class History
{
	//private static final Logger log = Logger.getLogger(History.class.getCanonicalName());
	private static final File defaultfile = new File(Prefs.getInstallRoot(), "attendance.txt");
	
	/**
	 * Retrieve the attendance report from the main host
	 * @param host the hostname to retrieve from
	 * @throws IOException 
	 * @throws URISyntaxException 
	 * @throws UnsupportedEncodingException 
	 */
	public static void getHistory(String host) throws IOException, URISyntaxException
	{		
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpProtocolParams.setUserAgent(httpclient.getParams(), "Scorekeeper/2.0");
		
        HttpPost request = new HttpPost(new URI("http", host, "/history/attendance", null));
        FileOutputStream out = new FileOutputStream(defaultfile);
        httpclient.execute(request).getEntity().writeTo(out);
	}
	
	/**
	 * Read in the history data from a csv file
	 * @param file the csv file to read from
	 * @return a map of lower case name to map of key/values
	 * @throws IOException
	 */
	public static void readFile(File file, List<Processor> processors) throws IOException
	{
        BufferedReader buffer = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        CSVParser parser = new CSVParser();
        
        try {
	    	String[] titles = parser.parseLine(buffer.readLine());
	        while (true) {
	        	// last first years series isttotal istavg pcchamp pcevents istqualify pcqualify
	        	String[] parts = parser.parseLine(buffer.readLine());
	        	if (parts == null) break;
	        	Map<String, String> object = new HashMap<String,String>();
	        	for (int ii = 0; ii < titles.length; ii++) {
	        		object.put(titles[ii], parts[ii]);
	        	}
	        	//ret.put(object.remove("first") + " " + object.remove("last"), object);
	        }
        } finally {
        	buffer.close();
        }

        return null;
	}	
}
