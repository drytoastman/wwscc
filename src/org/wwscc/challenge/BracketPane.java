/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.challenge;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.Scrollable;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import org.wwscc.challenge.viewer.RoundViewer;
import org.wwscc.storage.Challenge;
import org.wwscc.storage.Entrant;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;

/**
 * The BracketPane displays a tournament bracket, the driver labels and some action buttons.
 * @author bwilson
 */
public final class BracketPane extends JLayeredPane implements MessageListener, Scrollable, Printable, Pageable
{
	private static final Logger log = Logger.getLogger(BracketPane.class.getCanonicalName());
	private static final int roundWidth = 140;
	private static final int initialSpacing = 36;

	// these are the placements into the bracket as per SCCA rulebook
    public static final int[] RANK4 =  new int[] { 3, 2, 4, 1 };
    public static final int[] RANK8 =  new int[] { 6, 3, 7, 2, 5, 4, 8, 1 };
    public static final int[] RANK16 = new int[] { 11, 6, 14, 3, 10, 7, 15, 2, 12, 5, 13, 4, 9, 8, 16, 1 };
	public static final int[] RANK32 = new int[] { 22, 11, 27, 6, 19, 14, 30, 3, 23, 10, 26, 7, 18, 15, 31, 2, 21, 12, 28, 5, 20, 13, 29, 4, 24, 9, 25, 8, 17, 16, 32, 1 };

	// these are the map from finishing place to first bracket indexes
	public static final int[] POS4 = new int[4];  // i.e. becomes [ 3, 1, 0, 2], so person in top position, index 0 gets put in bracket position 3
	public static final int[] POS8 = new int[8];
	public static final int[] POS16 = new int[16];
	public static final int[] POS32 = new int[32];
	
	static
	{ // Take the SCCA mapping and turn it into values we can use
		for (int ii = 0; ii < RANK32.length; ii++)
			POS32[RANK32[ii]-1] = ii;
		for (int ii = 0; ii < RANK16.length; ii++)
			POS16[RANK16[ii]-1] = ii;
		for (int ii = 0; ii < RANK8.length; ii++)
			POS8[RANK8[ii]-1] = ii;
		for (int ii = 0; ii < RANK4.length; ii++)
			POS4[RANK4[ii]-1] = ii;
	}

	private TransferHandler handler = new DriverDrop();
	private Challenge challenge;
	private int baseRounds;
	private Vector<RoundGroup> rounds;
	private RoundGroup thirdPRound;
	private EntrantLabel winner3;
	private EntrantLabel winner1;
	private ChallengeModel model;

	/* 
	 * Groups two Entrant labels and the open button together
	 */
	class RoundGroup
	{
		int round;
		EntrantLabel upper;
		EntrantLabel lower;
		JButton open;
		
		public RoundGroup(int rnd)
		{
			round = rnd;
			upper = new EntrantLabel(new Id.Entry(challenge.getId(), round, Id.Entry.Level.UPPER));
			lower = new EntrantLabel(new Id.Entry(challenge.getId(), round, Id.Entry.Level.LOWER));
			open = new JButton("Open " + round);
			
			open.addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseDragged(MouseEvent e)
				{
					JComponent c = (JComponent)e.getSource();
					TransferHandler th = c.getTransferHandler();
					if (th != null)
						th.exportAsDrag(c, e, TransferHandler.MOVE);
				}
			});

			open.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e)
				{
					RoundViewer v = new RoundViewer(model, new Id.Round(challenge.getId(), round));
					v.addInternalFrameListener(new InternalFrameAdapter() {
						@Override
						public void internalFrameClosed(InternalFrameEvent e) {
							requestFocus();
					}});
					add(v, new Integer(10));
					v.moveToFront();
				}
			});
		}
		
		public void updateEntrants()
		{
			upper.updateEntrant();
			lower.updateEntrant();
		}
	}
	
	/**
	 * Construct a basic bracket pane.
	 */
	public BracketPane(ChallengeModel m)
	{
		super();
		rounds = new Vector<RoundGroup>();
		setOpaque(true);
		setBorder(new EmptyBorder(20,20,20,20));
		setModel(m);
		Messenger.register(MT.EVENT_CHANGED, this);
		Messenger.register(MT.MODEL_CHANGED, this);
		Messenger.register(MT.CHALLENGE_CHANGED, this);
		Messenger.register(MT.ENTRANT_CHANGED, this);
		Messenger.register(MT.PRINT_BRACKET, this);
		Messenger.register(MT.PRELOAD_MENU, this);
	}

	@Override
	public void event(MT type, Object data)
	{
		switch (type)
		{
			case EVENT_CHANGED:
				rounds.clear();
				baseRounds = 0;
				removeAll();
				repaint();
				break;
				
			case MODEL_CHANGED:
				setModel((ChallengeModel)data);
				break;
				
			case CHALLENGE_CHANGED:
				Challenge c = (Challenge)data;
				if (c == null)
					break;
				setChallenge(c);
				break;

			case PRELOAD_MENU:
				BracketingList b = new BracketingList(challenge.getName(), baseRounds*2);
				b.doDialog("Auto Load", null);
				List<BracketEntry> toload = b.getResult();
				if (toload == null)
					break;

				int[] pos = null;
				switch (baseRounds)
				{
					case 16: pos = POS32; break;
					case 8: pos = POS16; break;
					case 4: pos = POS8; break;
					case 2: pos = POS4; break;
				}

				int topround = baseRounds*2 - 1;
				int bys = baseRounds*2 - toload.size();
				for (int ii = 0; ii < toload.size(); ii++)
				{
					int placement = pos[ii];
					int rndidx = topround - placement/2;
					Id.Entry.Level level = (placement%2==1) ? Id.Entry.Level.LOWER : Id.Entry.Level.UPPER;
					Id.Entry entry = new Id.Entry(challenge.getId(), rndidx, level);
					if (bys > 0)
					{
						entry = entry.advancesTo();
						bys--;
					}
					model.setEntrant(entry, toload.get(ii).entrant, toload.get(ii).dialin);
				}

				// call set challenge to update all of our labels
				setChallenge(challenge);
				break;

			case ENTRANT_CHANGED:
				Id.Entry eid = (Id.Entry)data;
				if (eid.challengeid == challenge.getId())
				{
					if (eid.round == 0)
					{
						winner1.updateEntrant();
						winner3.updateEntrant();
						break;
					}
					else if (eid.round == 99)
					{
						thirdPRound.updateEntrants();
					}
					else
					{
						rounds.get(eid.round).updateEntrants();
					}
				}
				break;
				
			case PRINT_BRACKET:
				try
				{
					PrinterJob job = PrinterJob.getPrinterJob();
					job.setPageable(this);					
					job.setJobName(challenge.getName() + " Bracket");
					job.setCopies(1);
					if (!job.printDialog()) return; // user cancelled
					job.print();
				}
				catch (PrinterException pe)
				{
					log.log(Level.SEVERE, "Failed to print: {0}", pe.getMessage());
				}
				break;
		}
	}
	
	/**
	 * Return the ChallengeModel being used.
	 * @return
	 */
	public ChallengeModel getModel()
	{
		return model;
	}
	
	/**
	 * Set a new data model for use.
	 * @param m
	 */
	public void setModel(ChallengeModel m)
	{
		model = m;
	}

	/**
	 * Sets the depth of rounds that take place, 1 is just 2 people in a finale,
	 * recreate the brackets and components.
	 * @param id
	 */
	public void setChallenge(Challenge c)
	{
		challenge = c;
		int newDepth = challenge.getDepth();

		/* base rounds is the number of entrants int the first full round */
		removeAll();		
		baseRounds = (int)Math.pow(2, newDepth-1);
		
		int width = roundWidth * (newDepth+1);
		int height = (baseRounds * initialSpacing * 2); // - initialSpacing;

		Insets insets = getInsets();
		Dimension newSize = new Dimension(width + insets.left + insets.right, 
										height + insets.top + insets.bottom);

		setMinimumSize(newSize);
		setPreferredSize(newSize);
		//setPreferredSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		
		/* +1 extra round for the 3rd place bracket 
		 * example:
		 *   7
		 *   6 3
		 *   5 2 1
		 *   4   [0 index is 1st and 3rd place winners]
		 */
		
		rounds.clear();  // just restart from scratch for ease
		
		int cnt = (int)Math.pow(2, newDepth);
		for (int ii = rounds.size(); ii < cnt; ii++)
			rounds.add(new RoundGroup(ii));			

		winner1 = new EntrantLabel(new Id.Entry(challenge.getId(), 0, Id.Entry.Level.UPPER));
		winner3 = new EntrantLabel(new Id.Entry(challenge.getId(), 0, Id.Entry.Level.LOWER));
		thirdPRound = new RoundGroup(99);
		rounds.add(thirdPRound);
		
		if (cnt > 1)
		{
			RoundGroup rg;
			
			for (int ii = cnt - 1; ii >= 0; ii--)
			{
				rg = rounds.get(ii);
				rg.upper.updateEntrant();
				rg.lower.updateEntrant();
				add(rg.upper, new Integer(5));
				add(rg.lower, new Integer(5));
				add(rg.open, new Integer(5));
			}
			
			thirdPRound.upper.updateEntrant();
			thirdPRound.lower.updateEntrant();
			add(thirdPRound.upper, new Integer(5));
			add(thirdPRound.lower, new Integer(5));
			add(thirdPRound.open, new Integer(5));
			
			add(winner1, new Integer(5));
			add(winner3, new Integer(5));
			winner1.updateEntrant();
			winner3.updateEntrant();
		}
		
		revalidate();
		repaint();
	}
	
	@Override
	public void doLayout()
	{
		Insets insets = getInsets();
		int startX = insets.left;
		int startY = insets.top;
		int spacing = initialSpacing;
		int ridx = baseRounds * 2 - 1;
		int x = 0, y = 0;
		
		for (int ii = baseRounds; ii > 0; ii/=2)
		{
			/* Line ourselves up */
			x = startX;
			y = startY;
			
			/* Draw one vertical line of brackets */
			for (int jj = 0; jj < ii; jj++)
			{
				/* Draw first horizontal, second horizontal and then right hand vertical */
				setLabelByBottomLeft(rounds.get(ridx).upper, x, y);
				setButtonByCenterLeft(rounds.get(ridx).open, x, y+(spacing/2));
				y += spacing;
				setLabelByBottomLeft(rounds.get(ridx).lower, x, y);
				y += spacing;
				ridx--;
			}

			/* Adjust our starting position and spacing for the next column */
			startX += roundWidth;
			startY += spacing/2;
			spacing *= 2;
		}

		if (baseRounds > 0)
		{
			/* Draw the third place bracket and 3rd place winner line */
			x += 30;
			y = y - (spacing/2) + initialSpacing;
			setLabelByBottomLeft(thirdPRound.upper, x, y);
			setButtonByCenterLeft(thirdPRound.open, x, y+(initialSpacing/2));
			y += initialSpacing;
			setLabelByBottomLeft(thirdPRound.lower, x, y);
			/* Draw the 3rd place winner line */
			x += roundWidth;
			y -= (initialSpacing/2);
			setLabelByBottomLeft(winner3, x, y);

			/* Draw the winner line (0 rounds) */
			setLabelByBottomLeft(winner1, startX, startY);
		}
	}
	
	/**
	 * Override paintComponent so we can paint the background ladder
	 * @param g1 Graphics component
	 */
	@Override
	protected void paintComponent(Graphics g1)
	{
		Graphics2D g = (Graphics2D)g1;

		/* If opaque (when woulnd't we be?) fill the background */
		if (isOpaque())
		{
			Dimension size = getSize();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, size.width, size.height);
		}
		
		/* This is where we actually draw the bracket */
		g.setColor(Color.BLACK);
		Insets insets = getInsets();
		int startX = insets.left;
		int startY = insets.top;
		int spacing = initialSpacing;
		int x = 0, y = 0;
		
		/* Draw each round of brackets */
		for (int ii = baseRounds; ii > 0; ii/=2)
		{
			/* Line ourselves up */
			x = startX;
			y = startY;
			
			/* Draw one vertical line of brackets */
			for (int jj = 0; jj < ii; jj++)
			{
				/* Draw first horizontal, second horizontal and then right hand vertical */
				g.drawLine(x,            y,         x+roundWidth, y);
				y += spacing;
				g.drawLine(x,            y,         x+roundWidth, y);
				g.drawLine(x+roundWidth, y-spacing, x+roundWidth, y);
				y += spacing;
			}
			
			/* Adjust our starting position and spacing for the next column */
			startX += roundWidth;
			startY += spacing/2;
			spacing *= 2;
		}


		if (baseRounds > 0)
		{
			/* Draw the third place bracket and 3rd place winner line */
			x += 30;
			y = y - (spacing/2) + initialSpacing;
			/* Draw first horizontal, second horizontal and then right hand vertical */
			g.drawLine(x,            y,         x+roundWidth, y);
			y += initialSpacing;
			g.drawLine(x,            y,         x+roundWidth, y);
			g.drawLine(x+roundWidth, y-initialSpacing, x+roundWidth, y);
			/* Draw the 3rd place winner line */
			x += roundWidth;
			y -= (initialSpacing/2);
			g.drawLine(x, y, x+roundWidth, y);


			/* Draw the 1st place winner line (0 rounds) */
			g.drawLine(startX, startY, startX+roundWidth, startY);
		}
	}
	

	@Override
	public int getNumberOfPages() 
	{
		return 1;
	}

	@Override
	public PageFormat getPageFormat(int pageIndex) throws IndexOutOfBoundsException 
	{
		PageFormat p = new PageFormat();
		Dimension current = getMinimumSize();
		if (current.width > current.height)
			p.setOrientation(PageFormat.LANDSCAPE);
		else
			p.setOrientation(PageFormat.PORTRAIT);
		return p;
	}

	@Override
	public Printable getPrintable(int pageIndex) throws IndexOutOfBoundsException 
	{
		if (pageIndex != 0) throw new IndexOutOfBoundsException("Only one page for bracket");
		return this;
	}

	@Override
	public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
	{
		if (pageIndex != 0) return NO_SUCH_PAGE;

		Graphics2D g2 = (Graphics2D)graphics;
		Dimension current = this.getMinimumSize();
		double scale = 1.0;
		double w  = pageFormat.getImageableWidth(), h = pageFormat.getImageableHeight();
		if ((w < current.width) || (h < current.height))
			scale = Math.min(w/current.width, h/current.height);
		 
		log.info("Starting print job");
		log.log(Level.INFO, "Bracket is {0} x {1}", new Object[] { current.width, current.height });
		log.log(Level.INFO, "Printing with scale {0}", scale);
		log.log(Level.INFO, "Imageable at {0}, {1}, ({2} x {3})", new Object[] { pageFormat.getImageableX(), pageFormat.getImageableY(),
			pageFormat.getImageableWidth(), pageFormat.getImageableHeight()});
		log.log(Level.INFO, "Paper is {0} x {1}", new Object[] { pageFormat.getWidth(), pageFormat.getHeight() });
		
		g2.translate(pageFormat.getImageableX(),  pageFormat.getImageableY());
		g2.scale(scale, scale);
		
		for (RoundGroup rg : rounds)
			rg.open.setVisible(false);
		setOpaque(false);
		paint(g2);
		setOpaque(true);
		for (RoundGroup rg : rounds)
			rg.open.setVisible(true);
		
		return PAGE_EXISTS;
	}
	
	private void setLabelByBottomLeft(EntrantLabel label, int x, int y)
	{
		Dimension lblsize = label.getPreferredSize();
		lblsize.width = roundWidth-10;
		label.setBounds(x+4, y-lblsize.height-2, lblsize.width, lblsize.height);
	}
	
	private void setButtonByCenterLeft(JComponent button, int x, int y)
	{
		Dimension butsize = button.getPreferredSize();
		butsize.height = 14;
		button.setBounds(x+10, y-butsize.height, butsize.width, butsize.height);
	}

	@Override
	public Dimension getPreferredScrollableViewportSize()
	{
		return getPreferredSize();
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		return 100;
	}

	@Override
	public boolean getScrollableTracksViewportHeight()
	{
		return false;
	}

	@Override
	public boolean getScrollableTracksViewportWidth()
	{
		return false;
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		return 50;
	}
	
	/**
	 * 
	 */
	class EntrantLabel extends JLabel
	{
		private Entrant entrant;
		private double dialin;
		private Id.Entry entryId;

		public EntrantLabel(Id.Entry eid)
		{
			super();
			setTransferHandler(handler);
			MouseActions m = new MouseActions();
			addMouseListener(m);
			addMouseMotionListener(m);
			entryId = eid;
		}

		class MouseActions extends MouseAdapter
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
			}

			@Override
			public void mouseDragged(MouseEvent e) 
			{
				JComponent c = (JComponent)e.getSource();
				TransferHandler th = c.getTransferHandler();
				th.exportAsDrag(c, e, TransferHandler.MOVE);
			}		
		}

		private void updateText()
		{
			setText((entrant == null) ? " " : String.format("%.3f %s", dialin, entrant.getName()));
		}
		
		private void setEntry(BracketEntry e)
		{
			entrant = e.entrant;
			dialin = e.dialin;
			updateText();
			model.setEntrant(entryId, entrant, e.dialin);
		}

		public void updateEntrant()
		{
			entrant = model.getEntrant(entryId);
			if (entrant == null)
				dialin = 0;
			else
				dialin = model.getDial(entryId);
			updateText();
		}
		
		public BracketEntry getEntry()
		{
			return new BracketEntry(entryId, entrant, model.getDial(entryId));
		}
		
		public boolean isEmpty()
		{
			return (entrant == null);
		}
	}


	/**
	 * 
	 */
	class DriverDrop extends TransferHandler
	{
		@Override
		public int getSourceActions(JComponent c)
		{
			return COPY|MOVE|LINK;
		}

		@Override
		protected Transferable createTransferable(JComponent c)
		{
			return new BracketEntry.Transfer(((EntrantLabel)c).getEntry());
		}

		@Override
		protected void exportDone(JComponent c, Transferable data, int action)
		{
			if (action == MOVE)
			{
				BracketEntry.Transfer e = (BracketEntry.Transfer)data;
				if (JOptionPane.showConfirmDialog(null,
						String.format("This will remove %s from this round. Is that what you would like to do?", e.entry.entrant.getName()),
						"Entrant Swap", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
					return;

				model.setEntrant(e.entry.source, null, 0);
				rounds.get(e.entry.source.round).updateEntrants();
				Messenger.sendEvent(MT.ENTRANTS_CHANGED, e.entry);
			}
		}

		/**
		 * Returns true if data flavor is good and entrant isn't the same as the one dropping.
		 * @param support DND support object
		 * @return true if okay to drop
		 */
		@Override
		public boolean canImport(TransferHandler.TransferSupport support)
		{
			try 
			{
				EntrantLabel l = (EntrantLabel)support.getComponent();
				BracketEntry e = (BracketEntry) support.getTransferable().getTransferData(BracketEntry.Transfer.myFlavor);
				if (e.source == null)
					return true;
				if (l.entryId.round == e.source.round) // can't drag to same round
					return false;
				if (e.source.getDepth() < l.entryId.getDepth())  // can't drag backwards
					return false;
				if ((e.source.getDepth() - l.entryId.getDepth()) > 1) // can't go more than one forward
					return false;
				if (e.source.getDepth() == l.entryId.getDepth())
					support.setDropAction(LINK);
				else
					support.setDropAction(COPY);
				return true;
			} 
			catch (Exception ex) 
			{ 
				return false; 
			}
		}

		
		/* Called for drop and paste operations */
		@Override
		public boolean importData(TransferHandler.TransferSupport support)
		{
			try
			{
				if (support.isDrop())
				{
					BracketEntry transfer = (BracketEntry)support.getTransferable().getTransferData(BracketEntry.Transfer.myFlavor);
					EntrantLabel target = (EntrantLabel)support.getComponent();
					BracketEntry old = target.getEntry();					
					boolean swap = ((transfer.source != null) && (transfer.source.getDepth() == target.entryId.getDepth()));

					// swap operation in the same level of the challenge
					if (swap)
					{
						if (!target.isEmpty() && (JOptionPane.showConfirmDialog(null,
												String.format("This will swap %s and %s.  Is that what you would like to do?", old.entrant.getName(), transfer.entrant.getName()),
													"Entrant Swap", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION))
							return false;
						model.setEntrant(transfer.source, old.entrant, old.dialin);
						rounds.get(transfer.source.round).updateEntrants();
					}

					// copy from tree or previous round
					if (!swap)
					{
						if (!target.isEmpty() && (JOptionPane.showConfirmDialog(null, 
												String.format("This will overwrite %s with %s.  Is that what you would like to do?", old.entrant.getName(), transfer.entrant.getName()), 
													"Overwrite Warning", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION))
							return false;
					}
						
					target.setEntry(transfer);
					Messenger.sendEvent(MT.ENTRANTS_CHANGED, old);
					return true;
				}
			}
			catch (Exception ioe) { log.warning("Error during drop:" + ioe); }
			return false;
		}
	}
}