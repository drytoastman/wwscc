package org.wwscc.dataentry;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;

import org.wwscc.storage.Database;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;
import org.wwscc.util.SearchTrigger;

public abstract class MiniInput extends JPanel implements ActionListener
{
	JTextField entry;
	
	/**
	 * Base mini input that set the layout and basic actions (Esc means hide)
	 * @param label the string for the label portion
	 * @param openevent the event to listen for that will cause it to be visible and request focus
	 */
	public MiniInput(String label, MT openevent)
	{
		super(new MigLayout("fill, ins 1", "[80][grow]"));
		entry = new JTextField();

		JLabel lbl = new JLabel(label);
		lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 12));
		
		add(lbl, "al right");
		add(entry, "growx");
		setVisible(false);
		
		Messenger.register(openevent,  new MessageListener() {
			public void event(MT type, Object data) {
				setVisible(true);
				entry.requestFocus();
			}
		});

		entry.registerKeyboardAction(this, "esc", KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_FOCUSED);
		entry.registerKeyboardAction(this, "enter", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand() == "esc")
		{
			setVisible(false);
		}
	}
	

	/**
	 * Mini input for entering text to find in the current table
	 */
	public static class FindEntry extends MiniInput
	{		
		public FindEntry()
		{
			super("Find", MT.OPEN_FIND);
			entry.getDocument().addDocumentListener(new SearchTrigger() {
				@Override
				public void search(String txt) {
					Messenger.sendEvent(MT.FIND_ENTRANT, txt);
				}
			}); 
		}
	}
	
	/**
	 * Mini input for entering barcodes manually
	 */
	public static class ManualBarcodeInput extends MiniInput
	{		
		public ManualBarcodeInput()
		{
			super("Barcode", MT.OPEN_BARCODE_ENTRY);
		}
		
		public void actionPerformed(ActionEvent e)
		{
			if (e.getActionCommand().equals("enter"))
			{
				Messenger.sendEvent(MT.BARCODE_SCANNED, entry.getText().trim());
				entry.setText("");
			}
			else
				super.actionPerformed(e);
		}
	}
	
	/**
	 * Mini input for entering carids manually
	 */
	public static class ManualCarIdInput extends MiniInput
	{
		public ManualCarIdInput()
		{
			super("CarId", MT.OPEN_CARID_ENTRY);
		}
		
		public void actionPerformed(ActionEvent e)
		{
			if (e.getActionCommand().equals("enter"))
				processQuickTextField();
			else
				super.actionPerformed(e);
		}
		/**
		 * This takes care of the processing required to validate the quickTextField
		 * input and send out a CAR_ADD event.
		 */
		private void processQuickTextField()
		{
			String carText = entry.getText().trim();
			if(carText.length() > 0)
			{
				try
				{
					int carID = Integer.parseInt(carText);
					if(!Database.d.isRegistered(carID))
					{
						JOptionPane.showMessageDialog(
							getRootPane(),
							"The inputed registration card # is not registered for this event.",
							"User Input Error",
							JOptionPane.ERROR_MESSAGE
						);
					}
					else
					{
						Messenger.sendEvent(MT.CAR_ADD, carID);
					}
				}
				catch(NumberFormatException fe)
				{
					JOptionPane.showMessageDialog(
						getRootPane(),
						"The inputed registration card # was not valid ("+carText+").",
						"User Input Error",
						JOptionPane.ERROR_MESSAGE
					);
				}
				entry.setText("");
			}
		}
	}
}