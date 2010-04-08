/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.wwscc.admin;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JTable;
import javax.swing.table.TableModel;

/**
 *
 * @author bwilson
 */
public class BetterTable extends JTable
{
	public BetterTable(TableModel m)
	{
		super(m);

		addKeyListener( new KeyAdapter() { public void keyTyped(KeyEvent e) {
			if ((e.getKeyChar() == 24) && (e.getModifiersEx() == 128)) cutAction();
		}});

		addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) doubleClick();
		}});
	}

	public void doubleClickModelRow(int r)
	{
	}

	public void cutModelRows(int r[])
	{
	}

	public void doubleClick()
	{
		int idx = this.getSelectedRow();
		if (idx >= 0)
			doubleClickModelRow(convertRowIndexToModel(idx));
	}

	public void cutAction()
	{
		int[] selection = getSelectedRows();
		for (int i = 0; i < selection.length; i++) {
			selection[i] = convertRowIndexToModel(selection[i]);
		}
		cutModelRows(selection);
	}
}
