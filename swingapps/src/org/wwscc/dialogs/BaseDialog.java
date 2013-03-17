/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dialogs;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import org.wwscc.util.AutoTextField;
import org.wwscc.util.NumberField;

/**
 * Core functions for all dialogs.
 * @param <E> the type of value returned as the dialog result
 */
public class BaseDialog<E> extends JPanel implements ActionListener
{
	protected HashMap<String,JLabel> labels;
	protected HashMap<String,JTextField> fields;
	protected HashMap<String,JComboBox<Object>> selects;
	protected HashMap<String,JCheckBox> checks;
	protected HashMap<String,JRadioButton> radios;
	protected ButtonGroup buttonGroup;

	protected JPanel mainPanel;
	protected JPanel buttonPanel;
	protected JButton defaultButton;
	protected boolean floating;

	JDialog currentDialog;
	DialogFinisher<E> finisher;
	
	protected JButton ok;
	protected JButton cancel;
	
	protected boolean valid;
	protected String errorMessage;
	protected E result;

	public interface DialogFinisher<T>
	{
		public void dialogFinished(T object);
	}

	/**
	 * Create the dialog.
	 *
	 * @param lm
	 * @param floatme 
	 */
    public BaseDialog(LayoutManager lm, boolean floatme)
	{
        super(new BorderLayout());
		valid = false;
		currentDialog = null;
		floating = floatme;
		
		labels = new HashMap<String,JLabel>();
		fields = new HashMap<String,JTextField>();
		selects = new HashMap<String,JComboBox<Object>>();
		checks = new HashMap<String,JCheckBox>();
		radios = new HashMap<String,JRadioButton>();
		buttonGroup = new ButtonGroup();

		ok = new JButton("OK");
		cancel = new JButton("Cancel");
		ok.addActionListener(this);
		cancel.addActionListener(this);
		defaultButton = ok;
		
		buttonPanel = new JPanel();
		buttonPanel.add(ok);
		buttonPanel.add(cancel);

		mainPanel = new JPanel(lm);
		mainPanel.setBorder(new EmptyBorder(6,6,6,6));

		add(mainPanel, BorderLayout.WEST);
		add(buttonPanel, BorderLayout.SOUTH);
    }
  
	/**
	 * Create a new label with some default look
	 * @param name		the string data for the label
	 * @param bold		whether or not to make the Font bold
	 * @return			a pointer to the new JLabel
	 */
	protected JLabel label(String name, boolean bold)
	{
        JLabel lbl = new JLabel(name);

		Font base = lbl.getFont();
		if (bold) lbl.setFont(base.deriveFont(Font.BOLD));
		else lbl.setFont(base.deriveFont(Font.PLAIN));

		labels.put(name, lbl);
		return lbl;
	}

	/**
	 * Create a new textfield with some default look
	 * @param name		the name to store the entry under
	 * @param text		the default text for the field
	 * @return			a pointer to the new JLabel
	 */
	protected JTextField entry(String name, String text)
	{
		JTextField tf = new JTextField();
		tf.setText(text);
		fields.put(name, tf);
		return tf;
	}

	protected JTextField autoentry(String name, String text, List<String> list)
	{
		AutoTextField tf = new AutoTextField(list);
		tf.setText(text);
		fields.put(name, tf);
		return tf;
	}

	protected JTextField ientry(String name, Integer val)
	{
		NumberField tf = new NumberField(5, false);
		if (val != null)
			tf.setText(""+val);
		fields.put(name, tf);
		return tf;
	}

	protected String getEntryText(String name)
	{
		JTextField tf = fields.get(name);
		if (tf == null) return null;
		return tf.getText();
	}

	protected void setEntryText(String name, String val)
	{
		JTextField tf = fields.get(name);
		if (tf == null) return;
		tf.setText(val);
	}

	protected int getEntryInt(String name)
	{
		NumberField tf = (NumberField)fields.get(name);
		if (tf == null) return 0;
		return Integer.parseInt(tf.getText());
	}

	protected JComboBox<Object> select(String name, Object initial, List<?> possible, ActionListener al)
	{
		return select(name, initial, possible.toArray(), al);
	}
	
	protected JComboBox<Object> select(String name, Object initial, Object[] possible, ActionListener al)
	{
		JComboBox<Object> cb = new JComboBox<Object>(possible);
		selects.put(name, cb);
		cb.setSelectedItem(initial);
		if (al != null)
			cb.addActionListener(al);
		return cb;
	}

	protected Object getSelect(String name)
	{
		JComboBox<Object> cb = selects.get(name);
		if (cb == null) return null;
		return cb.getSelectedItem();
	}

	protected JCheckBox checkbox(String name, boolean selected)
	{
		JCheckBox rb = new JCheckBox(name);
		rb.setActionCommand(name);
		rb.setText("");
		rb.setSelected(selected);
		checks.put(name, rb);
		return rb;
	}
	
	protected boolean isChecked(String name)
	{
		JCheckBox cb = checks.get(name);
		if (cb == null) return false;
		return cb.isSelected();
	}
		
	protected JRadioButton radio(String name)
	{
		JRadioButton rb = new JRadioButton(name);
		rb.setActionCommand(name);
		radios.put(name, rb);
		buttonGroup.add(rb);
		return rb;
	}

	protected void radioEnable(String name, boolean enable)
	{
		JRadioButton rb = radios.get(name);
		if (rb == null) return;
		rb.setEnabled(enable);
	}

	protected void setSelectedRadio(String name)
	{
		JRadioButton rb = radios.get(name);
		if (rb == null) return;
		rb.doClick();
	}

	protected String getSelectedRadio()
	{
		ButtonModel m = buttonGroup.getSelection();
		if (m != null) return m.getActionCommand();
		return "";
	}


	@Override
	public void actionPerformed(ActionEvent ae)
	{	
		if (ae.getSource() == cancel)
		{
			close();
		}
		else if (ae.getSource() == ok)
		{
			errorMessage = null;
			if (verifyData())
			{
				valid = true;
				close();
			}
			else if (errorMessage != null)
			{
				JOptionPane.showMessageDialog(null, errorMessage, "Dialog Error", JOptionPane.WARNING_MESSAGE);
			}
			else
			{
				Toolkit.getDefaultToolkit().beep();
			}
		}
	}

	public void close()
	{
		if (currentDialog != null)
		{
			currentDialog.setVisible(false);
			currentDialog.dispose();
			currentDialog = null;
			if (finisher != null)
				finisher.dialogFinished(getResult()); // TODO: should this be in a non event thread?
		}
	}

	public boolean verifyData()
	{
		return false;
	}
	
	public boolean isValid()
	{
		return valid;
	}

	public E getResult()
	{
		return result;
	}

	public void doDialog(String title, DialogFinisher<E> finish)
	{
		if (currentDialog != null)
			return;

		finisher = finish;
		
		currentDialog = new JDialog();
		if (finisher == null)
		{
			currentDialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		}
		else if (floating)
		{
			currentDialog.setModalityType(Dialog.ModalityType.MODELESS);
			currentDialog.setAlwaysOnTop(true);
		}
		currentDialog.setContentPane(this);
		if (defaultButton != null)
			currentDialog.getRootPane().setDefaultButton(defaultButton);
		currentDialog.pack();
		currentDialog.setTitle(title);
		currentDialog.setLocationRelativeTo(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow());
		currentDialog.setVisible(true);
	}
}

