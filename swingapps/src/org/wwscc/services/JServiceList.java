/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */
package org.wwscc.services;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;
import org.wwscc.services.ServiceFinder.ServiceFinderListener;

/**
 */
@SuppressWarnings("serial")
public class JServiceList extends JList<FoundService> implements ServiceFinderListener 
{	
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
}

class FoundServiceRenderer extends DefaultListCellRenderer
{
	Map<String, Icon> iconMap = new HashMap<String, Icon>();
	
	public FoundServiceRenderer()
	{
		// some defaults
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
			 if (iconMap.containsKey(f.getService()))
			 {
				setIcon(iconMap.get(f.getService()));
				setText(String.format("%s (%s:%s)", f.getId(), f.getHost().getHostAddress(), f.getPort()));
			 }
			else
			{
				setIcon(null);
				setText(String.format("%s %s (%s:%s)", f.getService().toUpperCase(), f.getId(), f.getHost().getHostAddress(), f.getPort()));
			}
		 }
		 return this;
	 }
	
}