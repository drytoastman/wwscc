/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.challenge;

import java.awt.datatransfer.Transferable;
import java.util.Collection;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import org.wwscc.components.CarTree;
import org.wwscc.components.CarTreeRenderer;
import org.wwscc.storage.Database;
import org.wwscc.storage.Dialins;
import org.wwscc.storage.Entrant;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;

/**
 * The tree of possible entrants for a challenge.  Provides ability to drag new entrants
 * onto the challenge tree as well as a place to drag them off for 'deleting'.
 */
@SuppressWarnings("serial")
public class EntrantTree extends CarTree implements MessageListener
{
	private static final Logger log = Logger.getLogger(EntrantTree.class.getCanonicalName());

	/** indicator that drag operations use bonus dialins, otherwise we use regular style */
	protected boolean useBonusDialins;
	
	/**
	 * Create the entrant tree.  By default we use bonus dialins
	 */
	public EntrantTree()
	{
		setCellRenderer(new CarTreeRenderer());
		Messenger.register(MT.CHALLENGE_CHANGED, this);
		Messenger.register(MT.ENTRANTS_CHANGED, this);
		setTransferHandler(new DriverDrag());
		useBonusDialins = true;
	}
	
	@Override
	public void event(MT type, Object o)
	{
		switch (type)
		{
			case CHALLENGE_CHANGED:
			case ENTRANTS_CHANGED:
				UUID challengeid = ChallengeGUI.state.getCurrentChallengeId();
				Collection<UUID> exclude;
				Collection<Entrant> reg;
				if (challengeid != new UUID(0,0))
				{
					reg = Database.d.getEntrantsByEvent(null);
					exclude = Database.d.getCarIdsByChallenge(challengeid);
				}
				else
				{
					reg = new Vector<Entrant>();
					exclude = new Vector<UUID>();
				}

				makeTree(reg, exclude);
				break;
		}
	}

	/**
	 * Sets the type of dialins that will be used when an entrant is dragged over.
	 * @param bonus true for bonus, false for regular dialins
	 */
	public void useBonusDialins(boolean bonus)
	{
		useBonusDialins = bonus;
	}

	
	/**
	* Takes care of the drag from the entrant tree
	* and drop back in for 'deleteing' from the challenge pane
	*/
	class DriverDrag extends TransferHandler
	{
		@Override
		public int getSourceActions(JComponent c)
		{
			return COPY;
		}

		@Override
		protected Transferable createTransferable(JComponent c)
		{ 
			if (c instanceof EntrantTree) 
			{
				Object o = ((EntrantTree)c).getLastSelectedPathComponent();
				if (o instanceof DefaultMutableTreeNode)
				{
					Entrant e = (Entrant)((DefaultMutableTreeNode)o).getUserObject();
					Dialins dial = Database.d.loadDialins(null);
					return new BracketEntry.Transfer(new BracketEntry(null, e, dial.getDial(e.getCarId(), useBonusDialins)));
				}
			}
			return null;
		}

		/*protected void exportDone(JComponent c, Transferable data, int action)*/
		
		@Override
		public boolean canImport(TransferHandler.TransferSupport support)
		{
			try 
			{
				BracketEntry e = (BracketEntry) support.getTransferable().getTransferData(BracketEntry.Transfer.myFlavor);
				if (e.source == null) return false;
				support.setDropAction(MOVE);
				return true;
			} 
			catch (Exception ex) { return false; }
		}
		
		
		/* Called for drop operations */
		@Override
		public boolean importData(TransferHandler.TransferSupport support)
		{
			try
			{
				if (support.isDrop())
				{
					BracketEntry transfer = (BracketEntry)support.getTransferable().getTransferData(BracketEntry.Transfer.myFlavor);
					return transfer.source != null;
				}
			}
			catch (Exception ioe) { log.log(Level.WARNING, "Error during drop:{0}", ioe); }
			return false;
		}
	}
}