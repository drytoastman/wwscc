/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wwscc.services;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import org.wwscc.services.ServiceFinder.ServiceFinderListener;

/**
 *
 * @author bwilson
 */
public class JServiceList extends JList<FoundService> implements ServiceFinderListener {
	
	DefaultListModel<FoundService> serviceModel;
	
	public JServiceList()
	{
		super();
		serviceModel = new DefaultListModel<FoundService>();
		setModel(serviceModel);
	}

	@Override
	public void newService(FoundService service) 
	{
		serviceModel.addElement(service);
	}
}

