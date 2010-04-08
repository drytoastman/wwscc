/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.util;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Utility class for using the file chooser.  Also allows all the class users
 * to use the same file chooser object.  On Windows, this can mean greatly
 * improved time opening the window as a long pause of local registry actions occur
 * when instantiated.
 */
public class FileChooser 
{
	private static JFileChooser chooser;
	
	private static void setup(String title, String name, String ext, File initial)
	{
		if (chooser == null)
			chooser = new JFileChooser();
		
		if (title != null)
			chooser.setDialogTitle(title);
		else
			chooser.setDialogTitle(null);
		
		if (name != null)
			chooser.setFileFilter(new FileNameExtensionFilter(name, ext));
		else
			chooser.setFileFilter(null);
		
		if (initial != null)
			chooser.setSelectedFile(initial);
		else
			chooser.setSelectedFile(null);
	}
	
	public synchronized static File open(String title, String name, String ext)
	{
		setup(title, name, ext, null);
		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
			return chooser.getSelectedFile();
		return null;
	}
	
	public synchronized static File open(String title, String name, String ext, File initial)
	{
		setup(title, name, ext, initial);

		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
			return chooser.getSelectedFile();
		return null;
	}
	
	public synchronized static File save(String title, String name, String ext)
	{
		setup(title, name, ext, null);
		if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
			return chooser.getSelectedFile();
		return null;
	}
}
