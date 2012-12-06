/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.challenge;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import net.miginfocom.swing.MigLayout;
import org.wwscc.storage.Database;
import org.wwscc.util.BetterViewportLayout;
import org.wwscc.util.Logging;

/**
 * @author bwilson
 */
public class ChallengeGUI extends JFrame
{
	private static Logger log = Logger.getLogger(BracketPane.class.getCanonicalName());

	ChallengeModel model;
	JScrollPane bracketScroll;
	BracketPane bracket;
	JComboBox<String> bonusSelect;
	EntrantTree tree;
	
	/**
	 * Create the main GUI window.
	 */
	public ChallengeGUI()
	{
		super("Challenge");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		setJMenuBar(new Menus());
				
		model = new ChallengeModel();
		bracket = new BracketPane(model);
		bracketScroll = new JScrollPane(bracket);
		bracketScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		bracketScroll.getViewport().setBackground(Color.WHITE);
		bracketScroll.getViewport().setLayout(new BetterViewportLayout());
		
		tree = new EntrantTree();
		tree.setDragEnabled(true);
		
		bonusSelect = new JComboBox<String>(new String[] { "Bonus", "Regular" });
		bonusSelect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tree.useBonusDialins(bonusSelect.getSelectedItem().equals("Bonus"));
			}
		});
		bonusSelect.setSelectedItem("Bonus");  // just to make sure everyone is on the same page
		
		SelectionBar selectBar = new SelectionBar();

		JScrollPane tpane = new JScrollPane(tree);
		tpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		tpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		JPanel main = new JPanel(new MigLayout("fill", "[grow 0][fill]", "[grow 0][]"));
		
		main.add(new JLabel("Drag drives with"), "split 3");
		main.add(bonusSelect, "");
		main.add(new JLabel("dialins"), "");
		main.add(bracketScroll, "spany 2, grow, wrap");
		main.add(tpane, "growy, w 200!");

		BorderLayout layout = new BorderLayout();
		layout.setHgap(5);
		layout.setVgap(5);
		JPanel content = new JPanel(layout);
		content.add(selectBar, BorderLayout.NORTH);
		content.add(main, BorderLayout.CENTER);

		Database.openDefault();
		
		setContentPane(content);
		setSize(1024,768);
		setVisible(true);
	}
	
	/**
	 * Entry point for Challenge GUI.
	 * @param args unused
	 */
	public static void main(String args[])
	{
		try
		{
			Logging.logSetup("challenge");
			SwingUtilities.invokeLater(new Runnable() { public void run() {
				try {
					new ChallengeGUI();
				} catch (Exception sqle) {
					log.log(Level.SEVERE, "Failed to start Challenge GUI: " + sqle, sqle);
				}
			}});
		}
		catch (Throwable e)
		{
			log.log(Level.SEVERE, "Failed to start Challenge GUI: " + e, e);
		}
	}
}
