/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */


package org.wwscc.components;

import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.wwscc.storage.*;


public class CarTree extends JTree
{
	private static Logger log = Logger.getLogger(CarTree.class.getCanonicalName());
	HashSet<ClassNode> saved;

	public CarTree()
	{
		super(new Object[] {});
		setCellRenderer(new CarTreeRenderer());
	}

	final static class ClassNode
	{
		public String name;
		public String label;
		public ClassNode(String n)
		{
			name = n;
			label = null;
		}
		@Override
		public String toString()
		{
			if (label == null)
				return name;
			return label;
		}
		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof ClassNode)) return false;
			return ((ClassNode)o).name.equals(name);
		}
		@Override
		public int hashCode()
		{
			int hash = 5;
			hash = 31 * hash + (this.name != null ? this.name.hashCode() : 0);
			return hash;
		}
	}
	
	public void saveExpandedState()
	{
		DefaultMutableTreeNode root = (DefaultMutableTreeNode)getModel().getRoot();
		if (root == null) return;
		
		Enumeration<TreePath> e = getExpandedDescendants(new TreePath(root));
		if (e == null) return;

		saved = new HashSet<ClassNode>();
		while (e.hasMoreElements())
		{
			Object o = ((DefaultMutableTreeNode)e.nextElement().getLastPathComponent()).getUserObject();
			if (o instanceof ClassNode)
				saved.add((ClassNode)o);
		} 
	}


	public void restoreExpandedState()
	{
		if (saved == null) return;

		DefaultMutableTreeNode root = (DefaultMutableTreeNode)getModel().getRoot();
		if (root == null) return;

		TreePath start = new TreePath(root);

		for (Enumeration<?> e = root.children(); e.hasMoreElements(); )
		{
			TreeNode tn = (TreeNode)e.nextElement();
			Object o = ((DefaultMutableTreeNode)tn).getUserObject();
			if ((o instanceof ClassNode) && (saved.contains((ClassNode)o)))
			{
				log.fine("Expand " + tn);
				setExpandedState(start.pathByAddingChild(tn), true);
			}
		}
	}

	
	protected void makeTree(Collection<Entrant> reg, Collection<Integer> exclude)
	{
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
		Hashtable <String,Vector<Entrant>> classes = new Hashtable<String,Vector<Entrant>>();

		/* Create the class list */
		for (Entrant e : reg)
		{
			if (e == null)
			{
				log.warning("Null entrant in reg list?");
				continue;
			}
	
			Vector<Entrant> v = classes.get(e.getClassCode());
			if (v == null)
			{
				v = new Vector<Entrant>();
				classes.put(e.getClassCode(), v);
			}
	
			v.add(e);
		}


		Entrant.NumOrder carsorter = new Entrant.NumOrder();

		/* Create the tree, 'disable' anyone already in the runorder */
		String[] classlist = classes.keySet().toArray(new String[0]);	
		Arrays.sort(classlist);
		for (String name : classlist)
		{
			DefaultMutableTreeNode code = new DefaultMutableTreeNode(new ClassNode(name));
			root.add(code);

			Entrant[] elist = classes.get(name).toArray(new Entrant[0]);	
			Arrays.sort(elist, carsorter);
			for (Entrant e : elist)
			{
				if (!exclude.contains(e.getCarId()))
					code.add(new DefaultMutableTreeNode(e));
			}

			((ClassNode)code.getUserObject()).label = name + " (" + code.getChildCount() + " of " + elist.length + ")";
		}

		saveExpandedState();
		setModel(new DefaultTreeModel(root));
		restoreExpandedState();
    }
}
