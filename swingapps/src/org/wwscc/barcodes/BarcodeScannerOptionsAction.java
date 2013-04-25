package org.wwscc.barcodes;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;

import org.wwscc.util.MT;
import org.wwscc.util.Messenger;

public class BarcodeScannerOptionsAction extends AbstractAction
{
	public BarcodeScannerOptionsAction()
	{
		super("Barcode Scanner Options");
		putValue(MNEMONIC_KEY, KeyEvent.VK_I);
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		Messenger.sendEvent(MT.SCANNER_OPTIONS, null);	
	}
}
