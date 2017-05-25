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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.wwscc.util.CSVParser;
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
		throw new UnsupportedOperationException("Need to reimplement getAttendance if anyone uses it anymore");
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
