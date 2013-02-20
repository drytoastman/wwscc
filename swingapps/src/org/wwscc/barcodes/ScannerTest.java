/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wwscc.barcodes;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;
import net.miginfocom.swing.MigLayout;
import org.wwscc.util.MT;
import org.wwscc.util.Messenger;

public class ScannerTest extends JFrame
{
	JTextField input;
	public ScannerTest()
	{
		super("Simulate Scanner");
		setLayout(new MigLayout("", "fill", "fill"));	

		input = new JTextField(10);
		add(input, "w 250, wrap");

		JButton send = new JButton("Send");
		send.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				Messenger.sendEvent(MT.BARCODE_SCANNED, input.getText());
			}
		});
		add(send, "wrap");

		pack();
		setVisible(true);
	}
}
