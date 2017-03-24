/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2014 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;


public class StupidSimpleDataService implements Runnable
{
	private static Logger log = Logger.getLogger(StupidSimpleDataService.class.getCanonicalName());

	private Hashtable<String,String> data; // locking map as we have an outside thread setting data
	private int port;

	public StupidSimpleDataService(int port)
	{
		this.data = new Hashtable<String, String>();
		this.port = port;
	}
	
	public void setData(String k, String v)
	{
		data.put(k, v);
		synchronized (data) {
			data.notifyAll();
		}
	}

	@SuppressWarnings("resource")
	@Override
    public void run() 
    {        
        ServerSocket ssocket = null; 
        try  {
        	log.log(Level.INFO, "Starting announcer timer server on port " + port);
            ssocket = new ServerSocket(port); 
        }  catch (IOException ioe)  {
        	log.log(Level.SEVERE, "Failed to start announcer time server: " + ioe, ioe);
        }

        while (true) 
        {
        	try {
	            new Thread(new ConnectionHandler(ssocket.accept())).start();
        	} catch (IOException ioe) {
        		log.info("Incoming ssds connection failed: " + ioe);
        	}
        }
    }
	
	
	class ConnectionHandler implements Runnable
	{
		private Socket connection;
		
		public ConnectionHandler(Socket s)
		{
			connection = s;
		}

		public void run()
		{
			try
			{
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                OutputStream out = new BufferedOutputStream(connection.getOutputStream());
              
                // read first line of request (ignore the rest)
                String request = in.readLine();
                if (request==null)
                    return;
                while (true) 
                {
                    String misc = in.readLine();
                    if (misc == null || misc.length() == 0)
                        break;
                }

                // parse the line
                if (!request.startsWith("GET") || request.length()<14 || !(request.endsWith("HTTP/1.0") || request.endsWith("HTTP/1.1"))) 
                {
                	out.write("HTTP/1.0 400 Bad Request\r\n".getBytes());
                	out.flush();
                	connection.close();
                	return;
                }
                	
                String req = request.substring(4, request.length()-9).trim();
                String parts[] = req.split("/");
                if ((parts.length != 3) || (data.get(parts[1]) == null))
                {
                	out.write("HTTP/1.0 404 Don't know what you want\r\n".getBytes());
                	out.flush();
                	connection.close();
                	return;
                }
                
                out.write("HTTP/1.0 200 OK\r\n".getBytes());
                out.write("Content-Type: text/json\r\n".getBytes());
            	out.write("Server: Scorekeeper DataEntry\r\n\r\n".getBytes());
            	
                // wait until the data changes to something that wasn't provided in the get argument
                while (data.get(parts[1]).equals(parts[2]))
                {
            		synchronized (data) {
            			data.wait();
            		}
                }
                
                log.info("Served " + data.get(parts[1]));
                out.write(String.format("{\"%s\":\"%s\"}\r\n", parts[1], data.get(parts[1])).getBytes());
                out.flush();
                connection.close();
            } 
			catch (Exception ioe) 
			{ 
				log.info("Running ssds connection failed: " + ioe);
			}
        }
    }
	
	public static void main(String args[])
	{
		StupidSimpleDataService s = new StupidSimpleDataService(9090);
		new Thread(s).start();
		while (true)
		{
			try {
				s.setData("timer", "1.234");
				Thread.sleep(4000);
				s.setData("timer", "5.678");
				Thread.sleep(4000);
			} catch (InterruptedException ie) {
			  // nothing
			}
		}
	}
}
