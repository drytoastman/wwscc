/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.wwscc.util.CancelException;
import org.wwscc.util.Prefs;

/**
 */
public class RemoteHTTPConnection
{
	private static final Logger log = Logger.getLogger(RemoteHTTPConnection.class.getCanonicalName());
	String host;
	String dbname;
	MyClient httpclient;

	public RemoteHTTPConnection() throws IOException
	{
		this(Prefs.getHomeServer());
	}

	public RemoteHTTPConnection(String remote) throws IOException
	{
		host = remote;
		dbname = "";
		httpclient = new MyClient();
		HttpProtocolParams.setUserAgent(httpclient.getParams(), "Scorekeeper/2.0");
		CredentialsProvider creds = httpclient.getCredentialsProvider();

		for (Map.Entry<String,String> entry : Prefs.getPasswords().entrySet())
		{
			creds.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, entry.getKey()+"/series", "Digest"), 
									new UsernamePasswordCredentials("admin", entry.getValue()));
		}
	}

	class MyClient extends DefaultHttpClient
	{
		public final HttpResponse wrappedExecute(HttpUriRequest request) throws IOException, ClientProtocolException 
		{
			while (true)
			{
				HttpResponse response = super.execute(request);
				if (response.getStatusLine().getStatusCode() == 401)  // try and get new credentials from user cause something failed
				{
					String s = JOptionPane.showInputDialog("Password not accepted, please enter the proper password: ");
					if (s == null) 
						throw new CancelException("No input for password dialog");
					
					Prefs.setPasswordFor(dbname, s);
					httpclient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, dbname+"/series", "Digest"), 
																		new UsernamePasswordCredentials("admin", s));
					continue;
				}
				else if (response.getStatusLine().getStatusCode() != 200)
				{
					String error = EntityUtils.toString(response.getEntity());
					if (error.contains("<body>"))
						error = error.substring(error.indexOf("<body>")+6, error.length()-14);
					throw new IOException(error);
				}
				
				return response;
			}
			
		}
	}
	

	public byte[] performSQL(String name, byte[] data) throws IOException, URISyntaxException
	{
		dbname = name;
        ByteArrayEntity entity = new ByteArrayEntity(data);
        HttpPost sqlmap = new HttpPost(URIs.sqlmap(host, dbname));
        sqlmap.setEntity(new CountingEntity("Remote SQL", entity));		        
        return EntityUtils.toByteArray(new CountingEntity("SQL Results", httpclient.execute(sqlmap).getEntity()));
	}

	
	/**
	 * Get the list of available databases on the server.  The return value map contains keys based
	 * on the status of the database, currently "locked" or "unlocked".
	 * @return a map of status to lists of names
	 * @throws IOException
	 * @throws URISyntaxException 
	 */
	public Map<String, List<String>> getAvailableForCheckout() throws IOException, URISyntaxException
	{
		Map<String, List<String>> ret = new HashMap<String, List<String>>();
		ret.put("unlocked", new ArrayList<String>());
		ret.put("locked", new ArrayList<String>());
		
		HttpGet available = new HttpGet(URIs.available(host));
		HttpEntity resEntity = new CountingEntity("Download", httpclient.execute(available).getEntity());
		String data = EntityUtils.toString(resEntity);
		for (String db : data.split("\n"))
		{
			String[] parts = db.split("\\s+");
			if (parts.length < 3)
			{
				log.log(Level.INFO, "Invalid data from available: {0}", db);
				continue;
			}
			
			String name = parts[0];
			boolean locked = parts[1].trim().equals("1");
			boolean archived = parts[2].trim().equals("1");
			if (archived) continue;
			ret.get(locked ? "locked" : "unlocked").add(name);
		}

		for (List<String> list : ret.values())
			Collections.sort(list);
		return ret;
	}

	
	public void downloadDatabase(File dst, boolean lockServerSide) throws IOException, URISyntaxException
	{
		String name = dst.getName();
		this.downloadDatabase(dst, name.substring(0, name.indexOf('.')), lockServerSide);
	}

	public void downloadDatabase(File dst, String remotename, boolean lockServerSide) throws IOException, URISyntaxException
	{
		dbname = remotename;		
		HttpGet download = new HttpGet((lockServerSide) ? URIs.download(host, dbname) : URIs.copy(host, dbname));
		HttpEntity resEntity = new CountingEntity("Download", httpclient.execute(download).getEntity());
		FileOutputStream output = new FileOutputStream(dst);
		resEntity.writeTo(output);
	}

	public void uploadDatabase(File f) throws IOException, URISyntaxException
	{
		String name = f.getName();
		dbname = name.substring(0, name.indexOf('.'));
		
        MultipartEntity entity = new MultipartEntity();
        entity.addPart("db", new FileBody(f, dbname, ContentType.APPLICATION_OCTET_STREAM.getMimeType()));
        HttpPost upload = new HttpPost(URIs.download(host, dbname));
        upload.setEntity(new CountingEntity("Upload", entity));			
        EntityUtils.consume(httpclient.execute(upload).getEntity());
	}

	static class URIs {
		public static URI upload(String host, String db) throws URISyntaxException { return base(host, db, "upload"); }
		public static URI download(String host, String db) throws URISyntaxException { return base(host, db, "download"); }
		public static URI copy(String host, String db) throws URISyntaxException { return base(host, db, "copy"); }
		public static URI sqlmap(String host, String db) throws URISyntaxException { return base(host, db, "sqlmap"); }
		public static URI available(String host) throws URISyntaxException { return new URI("http", host, "/dbserve/available", null); }
		private static URI base(String host, String db, String action) throws URISyntaxException { return new URI("http", host, String.format("/dbserve/%s/%s", db, action), null); } 
	}

	class CountingEntity extends HttpEntityWrapper
	{
		String title;
		public CountingEntity(String monitorTitle, HttpEntity wrapped) { 
			super(wrapped);
			title = monitorTitle;
		}
		@Override
		public void writeTo(final OutputStream out) throws IOException {
			if (out instanceof MonitorProgressStream)
				wrappedEntity.writeTo(out);
			else
				wrappedEntity.writeTo(new MonitorProgressStream(title, out, wrappedEntity.getContentLength()));
		}
	}
		
	class MonitorProgressStream extends FilterOutputStream 
	{
		int transferred = 0;
		ProgressMonitor monitor;
		MonitorProgressStream(String title, OutputStream out, long max) { 
			super(out); 
			monitor = new ProgressMonitor(null, title, "Connecting...", 0, (int)max);
    		monitor.setMillisToDecideToPopup(50);
    		monitor.setMillisToPopup(50);
    		monitor.setProgress(0);
		}
		@Override
		public void write(final byte[] b, final int off, final int len) throws IOException {
			out.write(b, off, len);
			transferred += len;
			monitor.setProgress(transferred);
		}
		@Override
		public void write(final int b) throws IOException {
			out.write(b);
			transferred++;
			monitor.setProgress(transferred);
		}
		@Override
		public void close() throws IOException {
			super.close();
			monitor.close();
		}
	}
	
	public static void main(String[] args) throws Exception
	{
		RemoteHTTPConnection c = new RemoteHTTPConnection("localhost");
		c.uploadDatabase(new File("d:/cygwin/home/bwilson/testing.db"));
	}
}
