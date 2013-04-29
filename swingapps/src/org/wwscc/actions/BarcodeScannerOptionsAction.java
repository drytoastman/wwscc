package org.wwscc.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import org.wwscc.util.MT;
import org.wwscc.util.Messenger;

public class BarcodeScannerOptionsAction extends AbstractAction
{
	public BarcodeScannerOptionsAction()
	{
		super("Barcode Scanner Options");
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		Messenger.sendEvent(MT.SCANNER_OPTIONS, null);	
	}
}
