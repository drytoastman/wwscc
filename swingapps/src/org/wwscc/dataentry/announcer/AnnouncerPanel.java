/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dataentry.announcer;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.table.TableColumnModel;

import net.miginfocom.swing.MigLayout;

import org.wwscc.storage.AnnouncerData;
import org.wwscc.storage.Database;
import org.wwscc.storage.Entrant;
import org.wwscc.storage.EventResult;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;

/**
 * Encompasses all the components in the announcer tab and handles the run change
 * events, keeping track of last to finish as well as next to finish result panels.
 */
public class AnnouncerPanel extends JPanel implements MessageListener, ActionListener
{
	private static final Logger log = Logger.getLogger(AnnouncerPanel.class.getCanonicalName());

	static final Font regularFont = ((Font)UIManager.get("Label.font")).deriveFont(12f);
	static final Font boldFont = regularFont.deriveFont(Font.BOLD);
	static final Color grayBackground = new Color(220,220,220);
	static final Color tablebackground = new Color(200, 200, 244);
	static final Color buttonBackground = new Color(210, 210, 225);

	static final Color redBackground = new Color(250,200,200);
	static final Color redStroke = new Color(140,100,100);
	
	static final Color blueBackground = new Color(200,200,250);
	static final Color blueStroke = new Color(100,100,140);


	JToggleButton lastToFinish;
	JToggleButton nextToFinish;
	
	Entrant next;
	Entrant last;
	
	JLabel nameLabel;
	JLabel detailsLabel;
	JLabel classLabel;
	
	JTable nameTable;
	JTable details;
	JTable classTable;

	EntrantResultModel nameModel;
	LastRunStatsModel detailsModel;
	ClassListModel classModel;

	ClassListRenderer classRenderer;
	
	public AnnouncerPanel()
	{
		super(new MigLayout("ins 10 2", "[grow 50, fill][grow 50, fill]", "[][grow,fill]"));
		
		next = null;
		last = null;
		
		createButtons();
		createNameTable();
		createDetailsTable();
		createClassTable();
		
		add(lastToFinish, "split 2");
		add(nextToFinish, "wrap");
		add(nameLabel, "wrap");
		add(nameTable.getTableHeader(), "wrap");
		add(nameTable, "wrap");
		add(detailsLabel, "gaptop 10px, wrap");
		add(details, "wrap");
		add(classLabel, "gaptop 10px, wrap");
		JScrollPane scroller = new JScrollPane(classTable);
		scroller.setColumnHeader(null);
		scroller.setColumnHeaderView(null);
		add(scroller, "grow");		
		setOpaque(true); 
		setBackground(Color.WHITE);
	
		Messenger.register(MT.RUN_CHANGED, this);
		Messenger.register(MT.NEXT_TO_FINISH, this);
	}
	

	private void createButtons()
	{
		lastToFinish = new JToggleButton("Last To Finish");
		nextToFinish = new JToggleButton("Next To Finish");
		MetalLookAndFeel m = new MetalLookAndFeel();
		lastToFinish.setUI((ButtonUI)m.getDefaults().getUI(lastToFinish));
		nextToFinish.setUI((ButtonUI)m.getDefaults().getUI(lastToFinish));
		lastToFinish.setBackground(buttonBackground);
		nextToFinish.setBackground(buttonBackground);
		lastToFinish.setFont(regularFont);
		nextToFinish.setFont(regularFont);
		
		ButtonGroup b = new ButtonGroup();
		b.add(lastToFinish);
		b.add(nextToFinish);
		lastToFinish.addActionListener(this);
		nextToFinish.addActionListener(this);
		lastToFinish.setSelected(true);
	}
	
	private void createNameTable()
	{
		nameLabel = new JLabel("Driver", SwingConstants.CENTER);
		nameLabel.setFont(new Font("dialog", Font.BOLD, 16));
		nameLabel.setBorder(new LineBorder(Color.GRAY));
		nameLabel.setBackground(tablebackground);
		nameLabel.setOpaque(true);

		nameModel = new EntrantResultModel();
		
		nameTable = new JTable(nameModel);
		nameTable.setDefaultRenderer(Object.class, new EntrantResultRenderer());
		nameTable.setRowHeight(20);
		nameTable.setRowSelectionAllowed(false);
		nameTable.setColumnSelectionAllowed(false);
		
		TableColumnModel cm1 = nameTable.getColumnModel();
		cm1.getColumn(0).setPreferredWidth(250);
		cm1.getColumn(1).setPreferredWidth(50);
		cm1.getColumn(2).setPreferredWidth(50);
		cm1.getColumn(3).setPreferredWidth(250);
	}
	
	private void createDetailsTable()
	{
		detailsLabel = new JLabel("Last Run", JLabel.CENTER);
		detailsLabel.setFont(new Font("dialog", Font.BOLD, 16));
		detailsLabel.setBorder(new LineBorder(Color.GRAY));
		detailsLabel.setBackground(tablebackground);
		detailsLabel.setOpaque(true);

		detailsModel = new LastRunStatsModel();
		
		details = new JTable(detailsModel);		
		details.setFont(regularFont);
		details.setTableHeader(null);
		details.setRowHeight(22);
		details.setRowSelectionAllowed(false);
		details.setColumnSelectionAllowed(false);
		TableColumnModel cm2 = details.getColumnModel();
		cm2.getColumn(0).setPreferredWidth(250);
		cm2.getColumn(1).setPreferredWidth(250);
	}
	
	private void createClassTable()
	{
		classLabel = new JLabel("", SwingConstants.CENTER);
		classLabel.setFont(new Font("dialog", Font.BOLD, 16));
		classLabel.setBorder(new LineBorder(Color.GRAY));
		classLabel.setBackground(tablebackground);
		classLabel.setOpaque(true);
		classLabel.setText("Class");

		classModel = new ClassListModel();
		classTable = new JTable(classModel);

		classRenderer = new ClassListRenderer();
		classTable.setDefaultRenderer(Object.class, classRenderer);
		classTable.setRowHeight(20);
		classTable.setRowSelectionAllowed(false);
		classTable.setColumnSelectionAllowed(false);

		TableColumnModel cm = classTable.getColumnModel();
		cm.getColumn(0).setPreferredWidth(80);
		cm.getColumn(1).setPreferredWidth(500);
		cm.getColumn(2).setPreferredWidth(80);
		cm.getColumn(3).setPreferredWidth(250);
	}
	

	@Override
	public void event(MT type, Object o)
	{
		switch (type)
		{
			case RUN_CHANGED:
				last = (Entrant)o;
				updateDisplayData(last, true);
				lastToFinish.setSelected(true);
				break;
				
			case NEXT_TO_FINISH:
				next = (Entrant)o;
				break;
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) 
	{
		String cmd = e.getActionCommand();
		if (cmd.equals("Last To Finish"))
		{
			if (last != null)
				updateDisplayData(last, true);
		}
		else if (cmd.equals("Next To Finish"))
		{
			if (next != null)
				updateDisplayData(next, false);
		}
	}


	public void updateDisplayData(Entrant entrant, boolean showLast)
	{
		String classcode = entrant.getClassCode();
		List<EventResult> erlist = Database.d.getResultsForClass(classcode);
		AnnouncerData announcer = Database.d.getAnnouncerDataForCar(entrant.getCarId());
		EventResult myresult = null;
		
		for (EventResult er : erlist) {
			if (er.getCarId() == entrant.getCarId()) {
				myresult = er;
				break;
		}}
		
		if ((myresult == null) || (announcer == null))
		{
			log.warning("Announcer panel missing result for entrant, skipping processing");
			return;
		}

		if (announcer.getOldSum() != myresult.getSum())
            erlist.add(new FakeResult(entrant, "old", announcer.getOldSum()));
		if (announcer.getPotentialSum() != myresult.getSum())
            erlist.add(new FakeResult(entrant, "raw", announcer.getPotentialSum()));		            
		Collections.sort(erlist);
		
		nameLabel.setText(entrant.getName());
		nameModel.setData(entrant);

		if (showLast)
			detailsLabel.setText("Last Run");			
		else
			detailsLabel.setText("Difference");

		detailsModel.setData(erlist.get(0), myresult, announcer.getRawDiff(), announcer.getNetDiff(), showLast);
		classLabel.setText(classcode);
		classModel.setData(erlist);
		classRenderer.setHighlightValue(entrant);
	}
	
}
