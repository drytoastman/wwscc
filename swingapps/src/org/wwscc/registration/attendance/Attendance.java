/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2013 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.registration.attendance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import org.wwscc.util.CSVParser;
import org.wwscc.util.MonitorProgressStream;
import org.wwscc.util.Prefs;

public class Attendance
{
	//private static final Logger log = Logger.getLogger(Attendance.class.getCanonicalName());
	public static final File defaultfile = new File(Prefs.getInstallRoot(), "attendance.csv");
	
	/**
	 * Retrieve the attendance report from the main host
	 * @param host the hostname to retrieve from
	 * @throws IOException 
	 * @throws URISyntaxException 
	 * @throws UnsupportedEncodingException 
	 */
	public static void getAttendance(String host) throws IOException, URISyntaxException
	{		
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpProtocolParams.setUserAgent(httpclient.getParams(), "Scorekeeper/2.0");
		
		MonitorProgressStream monitor = new MonitorProgressStream("Download Attendance");
		monitor.setProgress(1);
		monitor.setNote("Initialize");
		
        HttpPost request = new HttpPost(new URI("http", host, "/history/attendance", null));
        File temp = File.createTempFile("attendance", "tmp");
		monitor.setProgress(2);
		monitor.setNote("Connecting/Calcuation...");

        HttpEntity download = httpclient.execute(request).getEntity();
		monitor.setProgress(3);
		monitor.setNote("Downloading...");

		FileOutputStream todisk = new FileOutputStream(temp);
        monitor.setStream(todisk, download.getContentLength());
        download.writeTo(monitor);
        FileUtils.copyFile(temp, defaultfile);
	}
	
	/**
	 * Read in the history data from a csv file
	 * @param file the csv file to read from
	 * @return a list of attendance entry values from the file
	 * @throws IOException
	 */
	public static List<AttendanceEntry> scanFile(File file) throws IOException
	{
        BufferedReader buffer = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        CSVParser parser = new CSVParser();
        List<AttendanceEntry> ret = new ArrayList<AttendanceEntry>();
        
        try {
	    	String[] titles = parser.parseLine(buffer.readLine());
	        while (true) {
	        	String[] parts = parser.parseLine(buffer.readLine());
	        	if (parts == null) break;	        	
	        	ret.add(new AttendanceEntry(titles, parts));
	        }
        } finally {
        	buffer.close();
        }
        
        return ret;
	}	
}
