/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */
package org.wwscc.dataentry;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import org.wwscc.storage.Database;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;

/**
 * A Panel to organize pre-registered user options.  This includes the QuickAdd and
 * ClassTree functionality.
 */
class PreregPanel extends JPanel implements MessageListener, ActionListener, KeyListener
{
	private static final Logger log = Logger.getLogger(PreregPanel.class.getCanonicalName());
	JScrollPane treePane;
	JPanel quickAddPanel;
	JTextField quickTextField;
	JButton quickAddButton;
	
	public PreregPanel(ClassTree centerTree)
	{
		super(new BorderLayout());
		this.setToolTipText("Add drivers by car ID from drivers cards.  CTRL+Q to jump to this location.");
		treePane = new JScrollPane(centerTree);
		
		// setup quick add section
		quickAddPanel = new JPanel(new BorderLayout());
		quickTextField = new JTextField();
		quickTextField.addKeyListener(this);
		quickAddButton = new JButton("Add");
		quickAddButton.addActionListener(this);
		quickAddButton.addKeyListener(this);
		
		quickAddPanel.add(new JLabel("Reg. Card #:"), BorderLayout.WEST);
		quickAddPanel.add(quickTextField, BorderLayout.CENTER);
		quickAddPanel.add(quickAddButton, BorderLayout.EAST);
		
		this.add(quickAddPanel, BorderLayout.NORTH);
		this.add(treePane, BorderLayout.CENTER);
		Messenger.register(MT.QUICK_ADD, this);
		Messenger.register(MT.SCANNER_INPUT, this);
	}
	
	/**
	 * This takes care of the processing required to validate the quickTextField
	 * input and send out a CAR_ADD event.
	 */
	private void processQuickTextField()
	{
		String carText = quickTextField.getText().trim();
		if(carText.length() > 0)
		{
			try
			{
				int carID = Integer.parseInt(carText);
				if(!Database.d.isRegistered(carID))
				{
					JOptionPane.showMessageDialog(
						getRootPane(),
						"The inputed registration card # is not registered for this event.",
						"User Input Error",
						JOptionPane.ERROR_MESSAGE
					);
				}
				else
				{
					Messenger.sendEvent(MT.CAR_ADD, carID);
				}
			}
			catch(NumberFormatException fe)
			{
				JOptionPane.showMessageDialog(
					getRootPane(),
					"The inputed registration card # was not valid ("+carText+").",
					"User Input Error",
					JOptionPane.ERROR_MESSAGE
				);
			}
			quickTextField.setText("");
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == quickAddButton)
		{
			processQuickTextField();
		}
	}
	
	@Override
    public void keyTyped(KeyEvent e) {}
	
	@Override
    public void keyReleased(KeyEvent e) {}
	
	@Override
    public void keyPressed(KeyEvent e)
    {
        if(e.getKeyCode() == KeyEvent.VK_ENTER)
    	{
        	processQuickTextField();
        }
    }

	@Override
	public void event(MT type, Object data)
	{
		switch (type)
		{
			case QUICK_ADD:
				if (!(getParent() instanceof JTabbedPane))
				{
					log.severe("Quick Add no longer in a tabbed pane");
					return;
				}
				((JTabbedPane)getParent()).setSelectedComponent(this);
				quickTextField.requestFocus();
				break;
				
			case SCANNER_INPUT:
				quickTextField.setText(((Integer)data).toString());
				processQuickTextField();
				break;
		}
	}
}