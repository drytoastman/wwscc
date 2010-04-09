/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */


package org.wwscc.protimer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import org.wwscc.timercomm.SerialDataInterface;
import org.wwscc.util.Logging;
import org.wwscc.util.MessageListener;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;


public class ProSoloInterface extends JFrame implements ActionListener, MessageListener
{
	private static Logger log = Logger.getLogger(ProSoloInterface.class.getCanonicalName());

	protected DebugPane debug;
	protected ResultsModel model;
	protected ResultsPane results;
	protected AuditLog audit;
    protected TimingInterface timing;

	protected DialinPane dialins;
	protected JLabel alignModeLabel;

    public ProSoloInterface() throws Exception
	{
		super("NWR ProSolo Interface");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		Class.forName("org.wwscc.dataentry.Sounds");
		
		debug = new DebugPane();
		model = new ResultsModel();
		results = new ResultsPane(model);
		audit = new AuditLog();
		dialins = new DialinPane();

		alignModeLabel = new JLabel("Align Mode");
		alignModeLabel.setForeground(Color.RED);
		alignModeLabel.setFont(new Font("serif", Font.BOLD, 20));
		alignModeLabel.setHorizontalAlignment(JLabel.CENTER);

		Messenger.register(MT.RUN_MODE, this);
		Messenger.register(MT.ALIGN_MODE, this);

		createMenus();
		add(dialins, BorderLayout.NORTH);


		JTabbedPane tp = new JTabbedPane();
		tp.addTab("Results", null, results, "results interface");
		tp.addTab("Debug", null, debug, "shows serial conversation");
		add(tp, BorderLayout.CENTER);

		add(createButtonPanel(), BorderLayout.SOUTH);

		setSize(1024, 768);
		setVisible(true);

		dialins.doFocus(this);
        timing = new TimingInterface();

		actionPerformed(new ActionEvent(this, 1, "Open Comm Port"));
    }


	public void createMenus()
	{
        JMenu file = new JMenu("File");
        file.add(createMenu("Quit", 0, 0));

		JMenu mode = new JMenu("Timer");
		mode.add(createMenu("Open Comm Port", 0, 0));
		mode.add(createMenu("Show/Hide Dialin", 0, 0));
		mode.add(createMenu("Delete Left Finish", KeyEvent.VK_Z, ActionEvent.ALT_MASK));
		mode.add(createMenu("Delete Right Finish", KeyEvent.VK_SLASH, ActionEvent.ALT_MASK));
        mode.add(createMenu("Set Run Mode", 0, 0));
        mode.add(createMenu("Set Align Mode", 0, 0));
        mode.add(createMenu("Reset", 0, 0));

		JMenu other = new JMenu("Other");
        other.add(createMenu("Show In Progress", 0, 0));
		other.add(createMenu("Show State", 0, 0));
        other.add(createMenu("Hard", 0, 0));

        JMenuBar bar = new JMenuBar();
        bar.add(file);
		bar.add(mode);
		bar.add(other);
        setJMenuBar(bar);
	}

	
	public JPanel createButtonPanel()
	{
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.33;
		panel.add(new TimingButtons(true), c);
		c.weightx = 1.00;
		panel.add(new OpenStatus(), c);
		c.weightx = 0.33;
		panel.add(new TimingButtons(false), c);
		c.weightx = 0;
		panel.add(Box.createHorizontalStrut(25));
		return panel;
	}


    protected JMenuItem createMenu(String title, int key, int modifier)
    {
        JMenuItem item = new JMenuItem(title);
        if (key != 0) item.setAccelerator(KeyStroke.getKeyStroke(key, modifier));
        item.addActionListener(this);
        return item;
    }


	@Override
	public void actionPerformed(ActionEvent e)
	{
		String com = e.getActionCommand();

		if (com.equals("Set Run Mode")) { Messenger.sendEvent(MT.INPUT_SET_RUNMODE, null); }
		else if (com.equals("Set Align Mode")) { Messenger.sendEvent(MT.INPUT_SET_ALIGNMODE, null); }
		else if (com.equals("Show In Progress")) { Messenger.sendEvent(MT.INPUT_SHOW_INPROGRESS, null); }
		else if (com.equals("Show State")) { Messenger.sendEvent(MT.INPUT_SHOW_STATE, null); }

		else if (com.equals("Delete Left Finish")) { Messenger.sendEvent(MT.INPUT_DELETE_FINISH_LEFT, null); }
		else if (com.equals("Delete Right Finish")) { Messenger.sendEvent(MT.INPUT_DELETE_FINISH_RIGHT, null); }

		else if (com.equals("Reset")) { Messenger.sendEvent(MT.INPUT_RESET_SOFT, null); }
		else if (com.equals("Hard")) { Messenger.sendEvent(MT.INPUT_RESET_HARD, null); }

		else if (com.startsWith("Show/Hide"))
		{
			dialins.setVisible(!dialins.isVisible());
		}
		else if (com.startsWith("Open Comm"))
		{
			String port;
			if ((port = SerialDataInterface.selectPort("BasicSerial")) != null)
				timing.openPort(port);
		}
		else if (com.equals("Quit"))
		{
			System.exit(0);
		}

	}


	@Override
	public void event(MT type, Object o)
	{
		switch (type)
		{
			case ALIGN_MODE:
				remove(dialins);
				add(alignModeLabel, BorderLayout.NORTH); 
				break;
			case RUN_MODE:
				remove(alignModeLabel);
				add(dialins, BorderLayout.NORTH); 
				break;
		}

		validate();
		repaint();
	}


    public static void main(String[] args)
	{
		try
		{
			Logging.logSetup("prointerface");
			new ProSoloInterface();
		}
		catch (Throwable e)
		{
			log.log(Level.SEVERE, "ProTimer thread died: " + e, e);
		}
    }
}

