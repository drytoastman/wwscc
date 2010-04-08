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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.Scrollable;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;
import org.wwscc.storage.Challenge;
import org.wwscc.storage.Entrant;
import org.wwscc.util.MessageListener;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;

/**
 * The BracketPane displays a tournament bracket, the driver labels and some action buttons.
 * @author bwilson
 */
public class BracketPane extends JComponent implements MessageListener, Scrollable
{
	private static Logger log = Logger.getLogger(BracketPane.class.getCanonicalName());
	private static final int roundWidth = 100;
	private static final int initialSpacing = 36;

    public static int[] RANK4 =  new int[] { 1, 4, 2, 3 };
    public static int[] RANK8 =  new int[] { 1, 8, 4, 5, 2, 7, 3, 6 };
    public static int[] RANK16 = new int[] { 1, 16, 8, 9, 4, 13, 5, 12, 2, 15, 7, 10, 3, 14, 6, 11 };
    public static int[] RANK32 = new int[] { 1, 32, 16, 17, 8, 25, 9, 24, 4, 29, 13, 20, 5, 28, 12, 21, 2, 31, 15, 18, 7, 26, 10, 23, 3, 30, 14, 19, 6, 27, 11, 22 };

	public static int[] POS4 = new int[4];
	public static int[] POS8 = new int[8];
	public static int[] POS16 = new int[16];
	public static int[] POS32 = new int[32];
	
	static
	{
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
	//private TransferHandler roundMove = new RoundDrag();
	private int challengeid;
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
			upper = new EntrantLabel(round, Id.Entry.Level.UPPER);
			lower = new EntrantLabel(round, Id.Entry.Level.LOWER);
			open = new JButton("Open " + round);
			//open.setTransferHandler(roundMove);
			open.addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseDragged(MouseEvent e)
				{
					JComponent c = (JComponent)e.getSource();
					TransferHandler th = c.getTransferHandler();
					th.exportAsDrag(c, e, TransferHandler.MOVE);
				}
			});
			open.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e)
				{
					Messenger.sendEvent(MT.OPEN_ROUND, new Id.Round(challengeid, round));
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
			case MODEL_CHANGED:
				setModel((ChallengeModel)data);
				break;
				
			case CHALLENGE_CHANGED:
				Challenge c = (Challenge)data;
				if (c == null)
					break;
				setChallenge(c.getId());
				break;

			case PRELOAD_MENU:
				BracketingList b = new BracketingList(baseRounds*2);
				b.doDialog("Preload", null);
				List<Entrant> toload = b.getResult();
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
					Id.Entry entry = new Id.Entry(challengeid, rndidx, level);
					if (bys > 0)
					{
						entry = entry.advancesTo();
						bys--;
					}
					model.setEntrant(entry, toload.get(ii));
				}

				// call set challenge to update all of our labels
				setChallenge(challengeid);
				break;

			case ENTRANT_CHANGED:
				Id.Entry eid = (Id.Entry)data;
				if (eid.challengeid == challengeid)
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
						RoundGroup rg = rounds.get(eid.round);
						rg.updateEntrants();
					}
				}
				break;
				
			case PRINT_BRACKET:
				saveToImage((File)data);
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
	public void setChallenge(int id)
	{
		challengeid = id;
		int newDepth = model.getDepth(challengeid);

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
		setMaximumSize(newSize);
		
		/* +1 extra round for the 3rd place bracket 
		 * example:
		 *   7
		 *   6 3
		 *   5 2 1
		 *   4   [0 index is 1st and 3rd place winners]
		 */
		
		/* Create components on demand, incase we reuse this pane. */
		int cnt = (int)Math.pow(2, newDepth);
		if (rounds.size() < cnt)
		{
			for (int ii = rounds.size(); ii < cnt; ii++)
				rounds.add(new RoundGroup(ii));			
		}

		if (winner1 == null)
		{
			winner1 = new EntrantLabel(0, Id.Entry.Level.UPPER); 
			winner3 = new EntrantLabel(0, Id.Entry.Level.LOWER);
			thirdPRound = new RoundGroup(99);
		}
		
		if (cnt > 1)
		{
			RoundGroup rg;
			
			for (int ii = cnt - 1; ii >= 0; ii--)
			{
				rg = rounds.get(ii);
				rg.upper.updateEntrant();
				rg.lower.updateEntrant();
				add(rg.upper);
				add(rg.lower);
				add(rg.open);
			}
			
			thirdPRound.upper.updateEntrant();
			thirdPRound.lower.updateEntrant();
			add(thirdPRound.upper);
			add(thirdPRound.lower);
			add(thirdPRound.open);
			
			add(winner1);
			add(winner3);
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

	public void saveToImage(File f)
	{
		try 
		{
			// 8.5 x 11 inch  (72dpi)
			// 612 x 792
			double scale = 1.0;
			int curwidth = getWidth();
			int curheight = getHeight();
			int newwidth = Math.min(curwidth, 750);
			int newheight = Math.min(curheight, 950);
			if ((newwidth < curwidth) || (newheight < curheight))
				scale = Math.min((double)newwidth/(double)curwidth, (double)newheight/(double)curheight);

			for (RoundGroup rg : rounds)
			{
				rg.open.setVisible(false);
			}

			BufferedImage image = new BufferedImage(newwidth, newheight, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = image.createGraphics();
			g2.setColor(Color.WHITE);
			g2.fillRect(0, 0, newwidth, newheight);
			AffineTransform at = AffineTransform.getScaleInstance(scale,scale);
			g2.setTransform(at);
			paint(g2);
			g2.dispose();
			ImageIO.write(image, "png", f);

			for (RoundGroup rg : rounds)
			{
				rg.open.setVisible(true);
			}
		}
		catch(IOException ioe) 
		{
			System.out.println(ioe.getMessage());
		}
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
		private int round;
		private Id.Entry.Level level;

		public EntrantLabel(int inRound, Id.Entry.Level inLevel)
		{
			super();
			setTransferHandler(handler);
			MouseActions m = new MouseActions();
			addMouseListener(m);
			addMouseMotionListener(m);
			round = inRound;
			level = inLevel;
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

		private void setEntrant(Entrant e)
		{
			entrant = e;
			setText((e == null) ? " " : e.getName());
			model.setEntrant(new Id.Entry(challengeid, round, level), entrant);
		}

		public void updateEntrant()
		{
			entrant = model.getEntrant(new Id.Entry(challengeid, round, level));
			setText((entrant == null) ? " " : entrant.getName());			
		}
		
		public Entrant getEntrant()
		{
			return entrant;
		}
	}


	/** Let the user drag round to round for errors during initial setup */
	class RoundDrag extends TransferHandler
	{
		@Override
		public int getSourceActions(JComponent c)
		{
			return MOVE;
		}

		@Override
		protected Transferable createTransferable(JComponent c)
		{
			return new StringSelection(((JButton)c).getText());
		}

		@Override
		public boolean canImport(TransferHandler.TransferSupport support)
		{
			try
			{
				String s = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
				return (s != null);
			}
			catch (Exception ex) { return false; }
		}

		/* Called for drop and paste operations */
		@Override
		public boolean importData(TransferHandler.TransferSupport support)
		{
			try
			{
				if (support.isDrop())
				{
					String s = (String)support.getTransferable().getTransferData(DataFlavor.stringFlavor);
					JButton b = (JButton)support.getComponent();
					int src = Integer.parseInt(s.substring(5));
					int dst = Integer.parseInt(b.getText().substring(5));
					Messenger.sendEvent(MT.MOVE_ROUND, new Id.Round[] {
										new Id.Round(challengeid, src),
										new Id.Round(challengeid, dst) } );
					return true;
				}
			}
			catch (Exception ioe) { log.warning("Error during drop:" + ioe); }
			return false;
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
			return COPY_OR_MOVE;
		}

		@Override
		protected Transferable createTransferable(JComponent c)
		{
			return new EntrantTransfer(((EntrantLabel)c).getEntrant());
		}

		@Override
		protected void exportDone(JComponent c, Transferable data, int action)
		{
			if (action == MOVE)
			{
				((EntrantLabel)c).setEntrant(null);
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
				Entrant e = (Entrant) support.getTransferable().getTransferData(EntrantTransfer.myFlavor);
				if (e == l.getEntrant())
					return false;
			} 
			catch (Exception ex) { return false; }
			return true;
		}


		/* Called for drop and paste operations */
		@Override
		public boolean importData(TransferHandler.TransferSupport support)
		{
			try
			{
				if (support.isDrop())
				{
					Entrant e = (Entrant)support.getTransferable().getTransferData(EntrantTransfer.myFlavor);
					EntrantLabel l = (EntrantLabel)support.getComponent();
					l.setEntrant(e);
					return true;
				}
			}
			catch (Exception ioe) { log.warning("Error during drop:" + ioe); }
			return false;
		}
	}
}