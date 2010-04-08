/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

public class DatabaseTransfer
{
	HttpURLConnection hpcon;
	String dbname;
	String host;
	String password;
	File db;
	
	byte[] buffer;
	ProgressMonitor monitor;
	int downloaded;

	public DatabaseTransfer()
	{
		buffer = new byte[4096];
		downloaded = 0;
		host = Prefs.getHomeServer();
		password = "";
	}

	/**
	 * Get the list of databases that are available for checking out from online.
	 * @return list of strings representing their names.
	 */
	protected String[] getAvailableForCheckout()
	{
		List<String> list = new ArrayList<String>();
		try
		{
			HttpURLConnection c = (HttpURLConnection)new URL("http://"+host+"/available").openConnection();            
			c.setUseCaches(false);
			c.setRequestProperty("User-Agent", "Scorekeeper DataEntry 1.0");
			c.setRequestProperty("X-Scorekeeper", password);

			BufferedReader reader = new BufferedReader(new InputStreamReader(c.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null)
				list.add(line);
			c.disconnect();
		}
		catch (IOException ioe)
		{
		}
		
		return list.toArray(new String[0]);
	}

	/**
	 * List the databases in the current directory that can be uploaded.
	 * @return a list of strings for the file names;
	 * @throws java.io.IOException
	 */
	protected String[] getAvailableForCheckin() throws IOException
	{
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.endsWith(".db"));
			}
		};
		
		File[] files = new File(Prefs.getInstallRoot() + "/htdocs/series/").listFiles(filter);
		String [] list = new String[files.length];
		for (int ii = 0; ii < files.length; ii++)
			list[ii] = files[ii].getName();
		return list;
	}

	/**
	 * Perform some basic setup on object values before performing a network operation.
	 * @param request the URL request being made
	 * @return true if everything is ready to go
	 * @throws java.io.IOException
	 */
	protected boolean commonSetup(String request) throws IOException
	{
		if (host.equals("") || dbname.equals("") || password.equals(""))
			throw new IOException("Missing required data ("+host+","+dbname+","+password+")");

		db = new File(Prefs.getInstallRoot() + "/htdocs/series/"+dbname);
		if (!db.getParentFile().isDirectory())
			if (!db.getParentFile().mkdirs())
				throw new IOException("Unable to create parent dir for: " + db);

		monitor = new ProgressMonitor(null, "Transfering " + dbname, "Connecting...", 0, Integer.MAX_VALUE);
		monitor.setMillisToDecideToPopup(0);
		monitor.setMillisToPopup(0);
		monitor.setProgress(0);

		hpcon = (HttpURLConnection) new URL("http://"+host+request+dbname).openConnection();            
		hpcon.setUseCaches(false);
		hpcon.setDoInput(true);
		hpcon.setRequestProperty("User-Agent", "Scorekeeper DataEntry 1.0");
		hpcon.setRequestProperty("X-Scorekeeper", password);

		return true;
	}


	protected void transferData(InputStream in, OutputStream out) throws IOException
	{
		int len;
		while((len = in.read(buffer)) >= 0) 
		{
			if (len == 0)
				continue;

			out.write(buffer, 0, len);

			if (monitor.isCanceled())
				break;

			downloaded += len;
			monitor.setProgress(downloaded);
		}
	}


	public File downloadDatabase(boolean lockServerSide)
	{
		try
		{
			dbname = (String)JOptionPane.showInputDialog(null, 
					"Select the file to check out", "Download File", JOptionPane.PLAIN_MESSAGE, 
					null, getAvailableForCheckout(), null);
			
			if (dbname == null)
				return null;
			
			if (lockServerSide)
			{
				if (!commonSetup("/download/"))
					return null;
			}
			else
			{
				if (!commonSetup("/copy/"))
					return null;
			}
				
			monitor.setMaximum(hpcon.getContentLength() + 1);
			InputStream input = hpcon.getInputStream();
			
			monitor.setNote("Downloading Data");
			
			File temp = File.createTempFile("download", ".tmp", db.getParentFile());
			FileOutputStream output = new FileOutputStream(temp);
			transferData(input, output);
			output.close();
			input.close();
			
			if (!temp.renameTo(db))
				throw new IOException("Can't rename temp download to: " + db);
		}
		catch (IOException ioe)
		{
			JOptionPane.showMessageDialog(null, "Download Error: " + ioe);
			return null;
		}

		if (monitor != null)
			monitor.close();

		return db;
	}


	public File uploadDatabase()
	{
		try
		{
			dbname = (String)JOptionPane.showInputDialog(null, 
					"Select the file to check in", "Upload File", JOptionPane.PLAIN_MESSAGE, 
					null, getAvailableForCheckin(), null);
				
			if (dbname == null)
				return null;
			
			if (!commonSetup("/upload/"))
				return null;
	
			String boundary = "--------------------" + Long.toString(System.currentTimeMillis(), 16);
			byte[] header = mimeFileHeader(boundary, "db", dbname, "application/octet-stream").getBytes();
			byte[] footer = mimeFooter(boundary).getBytes();
			long totallen = header.length + footer.length + db.length();

			monitor.setMaximum((int)totallen + 1);

			// POST only behaviour
			hpcon.setRequestMethod("POST");
			hpcon.setRequestProperty("Content-Type", "multipart/form-data; boundary="+boundary);
			hpcon.setFixedLengthStreamingMode((int)totallen);
			hpcon.setDoOutput(true);

			OutputStream output = hpcon.getOutputStream();
			FileInputStream input = new FileInputStream(db);

			output.write(header);
			monitor.setNote("Uploading Data");
			transferData(input, output);
			output.write(footer);
			output.flush();

			hpcon.getInputStream().read(); // For the connection to check its status

			output.close();
			input.close();	
			monitor.close();
		}
		catch (IOException ioe)
		{
			JOptionPane.showMessageDialog(null, "Upload Error: " + ioe);
			return null;
		}

		if (monitor != null)
			monitor.close();

		return db;
	}


	public String mimeFileHeader(String boundary, String name, String fileName, String mimeType)
	{
		String header = new String();
		header += "--"+boundary+"\r\n";
		header += "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"\r\n";
		header += "Content-Type: " + mimeType + "\r\n";
		header += "\r\n";
		return header;
	}


	public String mimeFooter(String boundary)
	{
		String footer = new String();
		footer += "\r\n";
		footer += "--"+boundary+"--\r\n";
		return footer;
	}

	public static void main(String args[]) throws IOException
	{
		DatabaseTransfer w = new DatabaseTransfer();
		//w.downloadDatabase();
		w.uploadDatabase();
	}
}

