/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * Separate class so testing can take place on Java 6 while allowing some Java 7 constructs in other source.
 */
public class LibLoader
{
	private static final Logger log = Logger.getLogger(LibLoader.class.getCanonicalName());

	private static boolean copyResource(String path, String dest)
	{		
		log.log(Level.INFO, "Attempting to copy resource/file {0}", path);
		try 
		{
			InputStream is = LibLoader.class.getResourceAsStream(path);
			if (is == null)
				is = new FileInputStream(path);
			FileOutputStream os = new FileOutputStream(dest);
			byte[] buf = new byte[4096];
			while (is.available() > 0)
			{
				int read = is.read(buf);
				os.write(buf, 0, read);
			}
			is.close();
			os.close();
			return true;
		}
		catch (Exception e)
		{
			log.log(Level.INFO, "copyResouce: {0}", e.getMessage());
		}
		
		return false;
	}
	
	private static void installLibrary(String name, String function)
	{
		String dirname = "", libname = "";		
		try
		{
			libname = System.mapLibraryName(name);
			dirname = System.getProperty("os.name").split("\\s")[0] + System.getProperty("sun.arch.data.model", "?");
			File ondisk = new File(libname);		
			if (!ondisk.exists())
			{
				log.log(Level.INFO, "{0} does not exist in cwd", libname);
				if (!copyResource("/" + dirname + "/" + libname, libname))
					copyResource("native/"+dirname+"/"+libname, libname);
			}

			System.loadLibrary(name);
		}
		catch (Throwable e)
		{
			JOptionPane.showMessageDialog(null, 
					"<HTML><B>" + function + " will be broken</b><br><br>" +
					"Failed to unpack or load '/" + dirname + "/" + libname + "': " + e.getMessage(),
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public static void installSerialLibrary()
	{
		installLibrary("rxtxSerial", "serial port access");
	}
}
