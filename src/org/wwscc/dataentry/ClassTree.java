/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dataentry;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import org.wwscc.components.CarTree;
import org.wwscc.storage.Database;
import org.wwscc.storage.Entrant;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;


public class ClassTree extends CarTree implements MessageListener
{
	private static Logger log = Logger.getLogger("org.wwscc.dataentry.ClassTree");

	public ClassTree()
	{
		super();
		addMouseListener(new DClickWatch());
		Messenger.register(MT.ENTRANTS_CHANGED, this);
		Messenger.register(MT.COURSE_CHANGED, this);
		Messenger.register(MT.RUNGROUP_CHANGED, this);
	}

	class DClickWatch extends MouseAdapter
	{
		@Override
		public void mouseClicked(MouseEvent e)
		{
			if (e.getClickCount() == 2)
			{
				TreePath tp = getSelectionPath();
				if (tp == null)
					return;

				DefaultMutableTreeNode lf = (DefaultMutableTreeNode)tp.getLastPathComponent();
				Object o = lf.getUserObject();
				if (o instanceof Entrant)
				{
					Entrant entrant = (Entrant)lf.getUserObject();
					if (entrant.isInRunOrder())
						return;

					Messenger.sendEvent(MT.CAR_ADD, entrant.getCarId());
				}
			}
		}
	}	

	@Override
	public void event(MT type, Object o)
	{
		switch (type)
		{
			case COURSE_CHANGED:
			case ENTRANTS_CHANGED:
				List<Entrant> reg = Database.d.getRegisteredEntrants();
				Set<Integer> runorder = Database.d.getCarIdsForCourse();
				makeTree(reg, runorder);
				break;

			case RUNGROUP_CHANGED:
				System.out.println(Database.d.getRunGroupMapping());
				break;
		}
	}
}
