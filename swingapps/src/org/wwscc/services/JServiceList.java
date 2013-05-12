/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */
package org.wwscc.services;

import java.awt.Component;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.SwingWorker;

import org.wwscc.services.ServiceFinder.ServiceFinderListener;

/**
 */
@SuppressWarnings("serial")
public class JServiceList extends JList<FoundService> implements ServiceFinderListener 
{	
	private static final Logger log = Logger.getLogger(JServiceList.class.getCanonicalName());
	
	private static Map<String, String> hostnames = new Hashtable<String, String>();  // map IP to name, need to do async to keep GUI lively
	private static final Pattern lookslikeip = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+");
	
	DefaultListModel<FoundService> serviceModel;
	FoundServiceRenderer renderer;
	
	/**
	 * Create a JList that can listen to a ServiceFinder and update its list accordingly
	 */
	public JServiceList()
	{
		super();
		serviceModel = new DefaultListModel<FoundService>();
		setModel(serviceModel);
		renderer = new FoundServiceRenderer();
		setCellRenderer(renderer);
	}

	/**
	 * @param service add this new service to this JList
	 */
	@Override
	public void newService(FoundService service) 
	{
		serviceModel.addElement(service);
		repaint();
	}
	
	@Override
	public void serviceTimedOut(FoundService service) 
	{
		serviceModel.removeElement(service);
		repaint();
	}
	
	public void mapIcon(String service, Icon icon)
	{
		renderer.mapIcon(service, icon);
	}


	/**
	 * Renderer for displaying Icon and service information based on FoundService objects
	 */
	class FoundServiceRenderer extends DefaultListCellRenderer
	{
		Map<String, Icon> iconMap;
		
		public FoundServiceRenderer()
		{
			// some defaults
			iconMap = new HashMap<String, Icon>();
			iconMap.put("BWTimer", new ImageIcon(getClass().getResource("/org/wwscc/images/timer.gif")));
			iconMap.put("ProTimer", new ImageIcon(getClass().getResource("/org/wwscc/images/draglight.gif")));
			iconMap.put("RemoteDatabase", new ImageIcon(getClass().getResource("/org/wwscc/images/server.gif")));
		}
		
		public void mapIcon(String service, Icon icon)
		{
			iconMap.put(service, icon);
		}
		
		@Override
		 public Component getListCellRendererComponent(
	        JList<?> list,
	        Object value,
	        int index,
	        boolean isSelected,
	        boolean cellHasFocus)
		 {
			 super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			 if (value instanceof FoundService)
			 {
				 FoundService f = (FoundService)value;
				 String name = f.getHost().getHostAddress();
				 
				 if (hostnames.containsKey(name))
					 name = hostnames.get(name);
				 else
					 new Lookup(f.getHost()).execute();
				 
				 if (iconMap.containsKey(f.getService()))
				 {
					setIcon(iconMap.get(f.getService()));
					setText(String.format("%s (%s:%s)", f.getId(), name, f.getPort()));
				 }
				else
				{
					setIcon(null);
					setText(String.format("%s %s (%s:%s)", f.getService().toUpperCase(), f.getId(), name, f.getPort()));
				}
			 }
			 return this;
		 }
	}
	
	/**
	 * Use SwingWorker thread to do hostname lookup so we GUI remains responsive
	 */
	class Lookup extends SwingWorker<String, Object>
	{
		
		InetAddress tofind;
		public Lookup(InetAddress src) { tofind = src; }
		@Override
		protected String doInBackground() throws Exception { return tofind.getHostName(); }
		@Override
		protected void done()  {
			try {
				if (lookslikeip.matcher(get()).matches())
					return;
				hostnames.put(tofind.getHostAddress(), get());
				repaint();
			} catch (Exception e) {
				log.info("Failed to process hostname lookup: " + e.getMessage());
			}
		}
	}
}
