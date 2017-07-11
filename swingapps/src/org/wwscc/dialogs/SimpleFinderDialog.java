/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dialogs;

import java.awt.Component;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.jmdns.JmmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

/**
 * Dialog that queries for a service on the network.
 */
@SuppressWarnings("serial")
public class SimpleFinderDialog extends BaseDialog<InetSocketAddress> implements ListSelectionListener
{
	private static final Logger log = Logger.getLogger(SimpleFinderDialog.class.getCanonicalName());
	public static final String BWTIMER_TYPE  = "_bwtimer._tcp.local.";
	public static final String PROTIMER_TYPE = "_protimer._tcp.local.";
	public static final String DATABASE_TYPE = "_scorekeeperdb._tcp.local.";
			
	private JServiceList list;
	private JmmDNS jmdns;
	
	/**
	 * shortcut when only looking for a single name
	 * @param serviceName
	 */
	public SimpleFinderDialog(String serviceName)
	{
		this(Arrays.asList(new String[] { serviceName }));
	}
	
	/**
	 * Create the dialog
	 * 
	 * @param serviceNames the service names to look for
	 */
    public SimpleFinderDialog(List<String> serviceNames)
	{
		super(new MigLayout(""), false);

		// some defaults
		Map<String, Icon> iconMap = new HashMap<String, Icon>();
		iconMap.put(BWTIMER_TYPE, new ImageIcon(getClass().getResource("/org/wwscc/images/timer.gif")));
		iconMap.put(PROTIMER_TYPE, new ImageIcon(getClass().getResource("/org/wwscc/images/draglight.gif")));
		iconMap.put(DATABASE_TYPE, new ImageIcon(getClass().getResource("/org/wwscc/images/server.gif")));
		
		list = new JServiceList(iconMap);
		list.addListSelectionListener(this);
		
		JScrollPane p = new JScrollPane(list);
        mainPanel.add(new JLabel("Full Discovery Can Take Up To 6 Seconds"), "spanx 2, center, wrap");
        mainPanel.add(p, "w 300, h 400, growx, spanx 2, wrap");
    
		mainPanel.add(label("Host", false), "");
		mainPanel.add(entry("host", ""), "growx, wrap");
		mainPanel.add(label("Port", false), "");
		mainPanel.add(ientry("port", 0), "growx, wrap");
		result = null;

        new Thread(new Runnable() { public void run() {
            jmdns = JmmDNS.Factory.getInstance();
            for (String service : serviceNames) {
                jmdns.addServiceListener(service, list);
            }
        }}).start();
    }

	@Override
	public void close()
	{
		new Thread(new Runnable() {
			@Override
			public void run() { try { jmdns.close(); } catch (IOException e) { log.log(Level.INFO, "JMDns did not close successfully: " + e, e);}}
		}).start();
		super.close();
	}
	
	/**
	 * Called after OK to verify data before closing.
	 */ 
	@Override
	public boolean verifyData()
	{
		try {
			result = new InetSocketAddress(getEntryText("host"), getEntryInt("port"));
		} catch (Exception e) {
			result = null;
		}
		return (result != null);
	}

	@Override
	public void valueChanged(ListSelectionEvent e) 
	{
		ServiceInfo f = list.getSelectedValue();
		if (f != null)
		{
			setEntryText("host", f.getHostAddresses()[0]);
			setEntryText("port", String.valueOf(f.getPort()));
		}
		else
		{
			setEntryText("host", "");
			setEntryText("port", "");
		}
	}

}


@SuppressWarnings("serial")
class JServiceList extends JList<ServiceInfo> implements ServiceListener 
{	
	private static final Logger log = Logger.getLogger(JServiceList.class.getCanonicalName());
	
	private static Map<InetAddress, String> hostnames = new Hashtable<InetAddress, String>();  // map IP to name, need to do async to keep GUI lively
	private static final Pattern lookslikeip = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+");
	
	DefaultListModel<ServiceInfo> serviceModel;
	FoundServiceRenderer renderer;
	
	/**
	 * Create a JList that can listen to a ServiceFinder and update its list accordingly
	 * @param iconMap map of service types to an icon to use
	 */
	public JServiceList(Map<String, Icon> iconMap)
	{
		super();
		serviceModel = new DefaultListModel<ServiceInfo>();
		setModel(serviceModel);
		renderer = new FoundServiceRenderer(iconMap);
		setCellRenderer(renderer);
	}


	@Override
	public void serviceAdded(ServiceEvent arg0) {}

	@Override
	public void serviceRemoved(ServiceEvent event)
	{
		log.info("serviceRemoved: " + event);
		serviceModel.removeElement(event.getInfo());
		repaint();
	}

	@Override
	public void serviceResolved(ServiceEvent event)
	{
		log.info("serviceResolved: " + event);
		if (!serviceModel.contains(event.getInfo()) && event.getInfo().getInet4Addresses().length > 0) {
			serviceModel.addElement(event.getInfo());
		}
		repaint();
	}

	/**
	 * Renderer for displaying Icon and service information based on FoundService objects
	 */
	class FoundServiceRenderer extends DefaultListCellRenderer
	{
		Map<String, Icon> iconMap;
		
		public FoundServiceRenderer(Map<String, Icon> map)
		{
			iconMap = map;
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
			 if (value instanceof ServiceInfo)
			 {
				 ServiceInfo f = (ServiceInfo)value;
				 InetAddress ip = InetAddress.getLoopbackAddress();
				 String hostname = "";
				 try {
					 ip = f.getInet4Addresses()[0];
				 } catch (Exception e) {
				 }
				 
				 if (hostnames.containsKey(ip))
					 hostname = hostnames.get(ip);
				 else
					 new Lookup(ip).execute();
				 
				 if (iconMap.containsKey(f.getType()))
				 {
					setIcon(iconMap.get(f.getType()));
					setText(String.format("%s (%s:%s)", hostname, ip, f.getPort()));
				 }
				else
				{
					setIcon(null);
					setText(String.format("%s (%s:%s) (type=%s)", hostname, ip, f.getPort(), f.getType()));
				}
			 }
			 return this;
		 }
	}
	
	/**
	 * Use SwingWorker thread to do hostname lookup so GUI remains responsive
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
				if (lookslikeip.matcher(get()).matches())  // don't resolve to IP?
					return;
				hostnames.put(tofind, get());
				repaint();
			} catch (Exception e) {
				log.info("Failed to process hostname lookup: " + e.getMessage());
			}
		}
	}
}


