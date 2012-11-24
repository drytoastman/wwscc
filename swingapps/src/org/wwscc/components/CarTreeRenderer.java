/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */
package org.wwscc.components;

import java.awt.Color;
import java.awt.Component;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import org.wwscc.storage.Entrant;

/**
 * Class to modify the default cell rendering for a JTree
 */
public class CarTreeRenderer extends DefaultTreeCellRenderer
{	
	ImageIcon blu, gre, yel, red;
	Color grey = new Color(230, 230, 230);
 
	/**
	 * Create a new CarTreeRenderer and load our icons.
	 */
	public CarTreeRenderer()
	{
		setLeafIcon(null);
	}  

	/**
	 * Overridden DefaultTreeCellRenderer method to change the type of drawing.
	 * @param tree		the main tree object
	 * @param value		the value we want to display
	 * @param isSel		if this tree node is selected
	 * @param isExp		if this tree node is expanded
	 * @param isLeaf	if this is a leaf node
	 * @param index		the index of this node in the tree node array
	 * @param cHasFocus	if the focus is on this node
	 */
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value,
						boolean isSel, boolean isExp, boolean isLeaf, int index, boolean cHasFocus)
	{
		super.getTreeCellRendererComponent(tree, value, isSel, isExp, isLeaf, index, cHasFocus);

		Object o = ((DefaultMutableTreeNode)value).getUserObject();
		if (o instanceof Entrant)
		{
			Entrant e = (Entrant)o;
			
			String display = e.getNumber() + " - " + e.getName() + " - " + e.getCarModel();
			if (!e.getIndexCode().equals(""))
				display += " ("+ e.getIndexCode() + ")";

			setText(display);

			if (e.isInRunOrder())
			{
				setForeground(grey);
				setBackground(Color.WHITE);
			}
		}

		return this;
	}
}